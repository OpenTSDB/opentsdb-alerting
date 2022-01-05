/*
 *  This file is part of OpenTSDB.
 *  Copyright (C) 2021 Yahoo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.opentsdb.horizon.alerting.corona.app;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.opentsdb.horizon.alerting.corona.Utils;
import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.GenericAlertSerializer;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroupSerializer;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.ContactsParser;
import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;
import net.opentsdb.horizon.alerting.corona.model.namespace.NamespaceListParser;
import org.apache.http.impl.client.CloseableHttpClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.component.Triple;
import net.opentsdb.horizon.alerting.corona.component.http.CloseableHttpClientBuilder;
import net.opentsdb.horizon.alerting.corona.component.kafka.KafkaProducerBuilder;
import net.opentsdb.horizon.alerting.corona.component.kafka.KafkaStream;
import net.opentsdb.horizon.alerting.corona.config.MetadataProvider;
import net.opentsdb.horizon.alerting.corona.config.impl.DbConfigFetcher;
import net.opentsdb.horizon.alerting.corona.config.impl.DbMetadataProvider;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.impl.NAlertConfig;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.impl.NAlertConfigListParser;
import net.opentsdb.horizon.alerting.corona.processor.Processor;
import net.opentsdb.horizon.alerting.corona.processor.debug.Fork;
import net.opentsdb.horizon.alerting.corona.processor.debug.MetadataOverrider;
import net.opentsdb.horizon.alerting.corona.processor.debug.Printer;
import net.opentsdb.horizon.alerting.corona.processor.debug.SyntheticAlertCounter;
import net.opentsdb.horizon.alerting.corona.processor.dispatcher.Dispatcher;
import net.opentsdb.horizon.alerting.corona.processor.dispatcher.MetadataAppender;
import net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.SplunkJson;
import net.opentsdb.horizon.alerting.corona.processor.filter.PeriodOverPeriodAlertFilter;
import net.opentsdb.horizon.alerting.corona.processor.filter.SnoozedFilter;
import net.opentsdb.horizon.alerting.corona.processor.groupby.GroupByProcessor;
import net.opentsdb.horizon.alerting.corona.processor.groupby.GroupKeyGenerator;
import net.opentsdb.horizon.alerting.corona.processor.kafka.impl.KafkaAlertReader;
import net.opentsdb.horizon.alerting.corona.processor.sender.KafkaTopicWriter;
import net.opentsdb.horizon.alerting.corona.processor.serializer.PreDispatchSerializer;
import net.opentsdb.horizon.alerting.corona.processor.serializer.PreSendSerializer;

public class AlertProcessor {

    private static final Logger LOG =
            LoggerFactory.getLogger(AlertProcessor.class);

    private final AlertProcessorConfig config;

    private final ExecutorService executor;

    private final Runnable pipeline;

    public AlertProcessor(AlertProcessorConfig config) {
        Objects.requireNonNull(config, "config cannot be null");

        this.config = config;
        this.executor = Executors.newSingleThreadExecutor();
        this.pipeline = createPipeline();
    }

    private DbMetadataProvider metadataProvider() {
        final CloseableHttpClient client =
                CloseableHttpClientBuilder.create()
                        .setMaxConnPerRoute(
                                config.getConfigApiClientMaxConnPerRoute())
                        .setMaxConnTotal(
                                config.getConfigApiClientMaxConnTotal())
                        .setRetryMax(
                                config.getConfigApiClientRetryMax())
                        .setConnectionRequestTimeoutMs(
                                config.getConfigApiClientConnectionRequestTimeoutMs())
                        .setConnectTimeoutMs(
                                config.getConfigApiClientConnectTimeoutMs())
                        .setSocketTimeoutMs(
                                config.getConfigApiClientSocketTimeoutMs())
                        .setCertificatePath(
                                config.getConfigApiClientHttpCertificatePath())
                        .setPrivateKeyPath(
                                config.getConfigApiClientHttpPrivateKeyPath())
                        .setTrustStorePassword(
                                config.getConfigApiClientHttpTrustStorePassword())
                        .setTrustStorePath(
                                config.getConfigApiClientHttpTrustStorePath())
                        .build();

        final DbConfigFetcher<NAlertConfig> configFetcher =
                DbConfigFetcher.<NAlertConfig>builder()
                        .setConfigListParser(new NAlertConfigListParser())
                        .setNamespaceListParser(new NamespaceListParser())
                        .setContactsParser(new ContactsParser())
                        .setClient(client)
                        .setBaseUrl(config.getConfigApiUrl())
                        .build();

        return DbMetadataProvider.create(configFetcher);
    }

    private KafkaTopicWriter kafkaTopicWriter(final String topic) {
        if (topic == null || topic.isEmpty()) {
            throw new RuntimeException("topic cannot be null or empty");
        }
        return KafkaTopicWriter.builder()
                .setTopic(topic)
                .setKafkaProducer(KafkaProducerBuilder.create()
                        .setProducerType("async")
                        .setBatchNumMessages("2000")
                        .setCompressionCodec("gzip")
                        .setKeySerializerClass("kafka.serializer.StringEncoder")
                        .setMessageSendMaxRetries("200")
                        .setRequestTimeoutMs("500")
                        .setRequestRequiredAcks("0")
                        .setRetryBackoffMs("500")
                        .setMetadataBrokerList(config.getKafkaBrokerListAsString())
                        .build()
                )
                .build();
    }

    private Runnable createPipeline() {
        final DbMetadataProvider dbMetadataProvider = metadataProvider();
        LOG.debug("Created DbMetadataProvider");

        final MetadataProvider metadataProvider;
        if (config.isDebug()) {
            metadataProvider = new MetadataOverrider(dbMetadataProvider);
            LOG.info("Debug enabled. Using MetadataOverrider.");
        } else {
            metadataProvider = dbMetadataProvider;
        }

        final KafkaTopicWriter emailTopicWriter =
                kafkaTopicWriter(config.getEmailKafkaTopic());
        final KafkaTopicWriter opsgenieTopicWriter =
                kafkaTopicWriter(config.getOpsgenieKafkaTopic());
        final KafkaTopicWriter slackTopicWriter =
                kafkaTopicWriter(config.getSlackKafkaTopic());
        final KafkaTopicWriter ocTopicWriter =
                kafkaTopicWriter(config.getOcKafkaTopic());
        final KafkaTopicWriter webhookTopicWriter =
                kafkaTopicWriter(config.getWebhookKafkaTopic());
        final KafkaTopicWriter pagerDutyTopicWriter =
                kafkaTopicWriter(config.getPagerDutyKafkaTopic());
        LOG.debug("Created KafkaTopicWriters");

        final Dispatcher dispatcher = Dispatcher.builder()
                .setEmailHandler(
                        PreSendSerializer.create(emailTopicWriter)
                )
                .setOpsGenieHandler(
                        PreSendSerializer.create(opsgenieTopicWriter)
                )
                .setSlackHandler(
                        PreSendSerializer.create(slackTopicWriter)
                )
                .setOcHandler(
                        PreSendSerializer.create(ocTopicWriter)
                )
                .setWebhookHandler(
                        PreSendSerializer.create(webhookTopicWriter)
                )
                .setPagerDutyHandler(
                        PreSendSerializer.create(pagerDutyTopicWriter)
                )
                .build();
        LOG.debug("Created Dispatcher");

        final PreDispatchSerializer preDispatchSerializer =
                PreDispatchSerializer.builder()
                        .setNext(dispatcher)
                        .setAlertGroupSerializer(new AlertGroupSerializer())
                        .build();
        LOG.debug("Created PreDispatchSerializer");

        final Processor<Triple<AlertGroup, Metadata, Contacts>> filters =
                createFilters(preDispatchSerializer);
        LOG.debug("Created alert filter(s).");

        // TODO: Not a real fork. One thread is doing all job.
        final Fork<Triple<AlertGroup, Metadata, Contacts>> preDispatchFork =
                Fork.<Triple<AlertGroup, Metadata, Contacts>>builder()
                        .addProcessor(filters)
                        .addProcessor(new SplunkJson())
                        .build();
        LOG.debug("Created preDispatchFork");

        final MetadataAppender metadataAppender =
                MetadataAppender.builder()
                        .setNext(preDispatchFork)
                        .setMetadataProvider(metadataProvider)
                        .build();
        LOG.debug("Created MetadataAppender");

        final GroupByProcessor groupByProcessor =
                GroupByProcessor.builder()
                        .setNext(metadataAppender)
                        .setNumWorkers(4)
                        .setFlushFrequencyMs(60_000L)
                        .build();
        LOG.debug("Created GroupByProcessor");

        final GroupKeyGenerator groupKeyGenerator =
                GroupKeyGenerator.builder()
                        .setNext(groupByProcessor)
                        .setMetadataProvider(metadataProvider)
                        .build();
        LOG.debug("Created GroupKeyGenerator");

        final Processor<Alert> alertFork = Fork.<Alert>builder()
                .addProcessor(SyntheticAlertCounter.create(config.getSyntheticAlertIds()))
                .addProcessor(groupKeyGenerator)
                .addProcessor(new Printer<>())
                .build();

        final KafkaAlertReader kafkaAlertReader = KafkaAlertReader.builder()
                .setNext(alertFork)
                .setComponentId("alert-reader")
                .setSerializer(new GenericAlertSerializer())
                .setKafkaStream(KafkaStream.builder()
                        .setAutoCommitEnable(
                                config.getAlertProcessorKafkaAutoCommitEnable())
                        .setAutoOffsetReset(
                                config.getAlertProcessorKafkaAutoOffsetReset())
                        .setRebalanceBackoffMs(
                                config.getAlertProcessorKafkaRebalanceBackoffMs())
                        .setRebalanceRetriesMax(
                                config.getAlertProcessorKafkaRebalanceRetriesMax())
                        .setZookeeperConnectionTimeoutMs(
                                config.getAlertProcessorKafkaZookeeperConnectionTimeoutMs())
                        .setZookeeperSessionTimeoutMs(
                                config.getAlertProcessorKafkaZookeeperSessionTimeoutMs())
                        .setZookeeperConnect(
                                config.getKafkaZookeeperConnectAsString())
                        .setGroupId(config.getAlertProcessorKafkaGroupId())
                        .setTopic(config.getAlertProcessorKafkaTopic())
                        .setConsumerId(Utils.getConsumerIdWithRandomPrefix())
                        .build())
                .build();
        LOG.debug("Created KafkaAlertReader");

        LOG.info("Created the AlertProcessor pipeline");
        return () -> {
            LOG.debug("Starting DbMetadataProvider");
            dbMetadataProvider.start();

            LOG.debug("Starting GroupByProcessor");
            groupByProcessor.start();

            try {
                LOG.debug("Starting KafkaAlertReader");
                kafkaAlertReader.run();
            } finally {
                kafkaAlertReader.close();
                LOG.debug("Closed KafkaAlertReader");

                groupByProcessor.stop();
                LOG.debug("Stopped GroupByProcessor");

                dbMetadataProvider.stop();
                LOG.debug("Stopped DbMetadataProvider");
            }
        };
    }

    private Processor<Triple<AlertGroup, Metadata, Contacts>> createFilters(
            Processor<Triple<AlertGroup, Metadata, Contacts>> next) {

        final SnoozedFilter snoozedFilter = new SnoozedFilter(next);
        LOG.debug("Created SnoozedFilter");

        if (config.isEnablePeriodOverPeriodAlertFilter()) {
            final PeriodOverPeriodAlertFilter popaFilter =
                    new PeriodOverPeriodAlertFilter(snoozedFilter);

            LOG.debug("Created PeriodOverPeriodAlertFilter");
            return popaFilter;
        }

        return snoozedFilter;
    }

    public void start() {
        LOG.debug("Submitting pipeline to executor");
        final Future future = executor.submit(pipeline);

        try {
            LOG.debug("Waiting for execution result");
            future.get();
        } catch (Exception e) {
            LOG.error("Exception running AlertProcessor", e);
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        executor.shutdownNow();
    }
}
