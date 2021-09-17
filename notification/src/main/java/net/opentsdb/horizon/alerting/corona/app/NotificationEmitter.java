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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import net.opentsdb.horizon.alerting.corona.Utils;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKitSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.core.OpenSSLSaltedAESCBSRSA256Encryptor;
import net.opentsdb.horizon.alerting.corona.component.http.CloseableHttpClientBuilder;
import net.opentsdb.horizon.alerting.corona.component.kafka.KafkaStream;
import net.opentsdb.horizon.alerting.corona.component.secrets.SecretProvider;
import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;
import net.opentsdb.horizon.alerting.corona.processor.debug.ContactOverrider;
import net.opentsdb.horizon.alerting.corona.processor.debug.SyntheticMessageKitCounter;
import net.opentsdb.horizon.alerting.corona.processor.denoiser.Denoiser;
import net.opentsdb.horizon.alerting.corona.processor.emitter.email.EmailClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.email.EmailEmitter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.email.EmailFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.oc.OcClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.oc.OcEmitter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.oc.OcFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.OcRestEmitter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.impl.DefaultOcRestClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.impl.DefaultOcRestFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie.OpsGenieClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie.OpsGenieEmitter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie.OpsGenieFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.prism.PrismEmitter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.prism.impl.DefaultPrismClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.prism.impl.DefaultPrismFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.SlackEmitter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.SlackFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.impl.SlackClientImpl;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.WebhookEmitter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.impl.DefaultWebhookClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.impl.DefaultWebhookFormatter;
import net.opentsdb.horizon.alerting.corona.processor.kafka.impl.KafkaMessageKitReader;

public class NotificationEmitter {

    enum Type {
        EMAIL,
        OPSGENIE,
        SLACK,
        OC,
        OCREST,
        PRISM,
        WEBHOOK
    }

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(NotificationEmitter.class);

    /**
     * This id is used to distinguish between different components for
     * monitoring purposes.
     * <p>
     * E.g. {@link KafkaMessageKitReader} will use it to add a tag
     * `component-id` to the gauge of the Kafka's queue size.
     */
    private static final String MESSAGE_KIT_READER_COMPONENT_ID =
            "messagekit-reader";

    /* ------------ Fields ------------ */

    private final NotificationEmitterConfig config;

    private final ExecutorService executor;

    private final Runnable pipeline;

    /* ------------ Constructor ------------ */

    public NotificationEmitter(final NotificationEmitterConfig config)
    {
        Objects.requireNonNull(config, "config cannot be null");
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor();
        this.pipeline = createPipeline();
    }

    /* ------------ Methods ------------ */

    private Processor<MessageKit> createEmailEmitter()
    {
        return EmailEmitter.builder()
                .setEmailClient(new EmailClient(
                        config.getEmailClientSmtpHost(),
                        config.getEmailClientConnectionTimeoutMs()
                ))
                .setFormatter(new EmailFormatter(
                        config.getDebugEmailPrefix()
                ))
                .setMaxSendAttempts(3)
                .build();
    }

    private Processor<MessageKit> createOpsGenieEmitter()
    {
        Function<String, String> apikeyEncryptor;
        try {
            // TODO - Need to handle secrets here.
            final SecretProvider secretProvider = null;

            final String encryptorSecret = secretProvider.getString(
                    config.getOpsgenieApikeyEncryptorSecretKeyName()
            );
            secretProvider.shutdown();

            final OpenSSLSaltedAESCBSRSA256Encryptor encryptor = new OpenSSLSaltedAESCBSRSA256Encryptor(encryptorSecret);
            apikeyEncryptor = encryptor::encryptBase64;
        } catch (Exception e) {
            LOG.error("Failed to initialize OpsGenie ApiKey encryptor", e);
            apikeyEncryptor = null;
        }

        final OpsGenieEmitter opsGenieEmitter = OpsGenieEmitter.builder()
                .setClient(new OpsGenieClient())
                .setFormatter(OpsGenieFormatter.builder()
                        .setUser(config.getOpsgenieUser())
                        .setSource(config.getOpsgenieSource())
                        .build()
                )
                .setMaxSendAttempts(config.getOpsgenieMaxSendAttempts())
                .setApiKeyEncryptor(apikeyEncryptor)
                .build();
        return Denoiser.builder()
                .setNext(opsGenieEmitter)
                .setEmitterType("opsgenie")
                .build();
    }

    private Processor<MessageKit> createSlackEmitter()
    {
        return SlackEmitter.builder()
                .setClient(new SlackClientImpl(
                        CloseableHttpClientBuilder.create()
                                .setTLSEnabled(false)
                                .build()
                ))
                .setFormatter(new SlackFormatter())
                .build();
    }

    private Processor<MessageKit> createOcEmitter()
    {
        return OcEmitter.builder()
                .setClient(new OcClient())
                .setFormatter(new OcFormatter(config.getOcColo(), config.getOcHost()))
                .setDeniedNamespaces(config.getOcDeniedNamespaces())
                .build();
    }

    private Processor<MessageKit> createOcRestEmitter()
    {
        // TODO - secret provider here.
        final SecretProvider secretProvider = null;

        final OcRestEmitter emitter = OcRestEmitter.builder()
                .setClientBuilder(DefaultOcRestClient.builder()
                        .setHttpClient(CloseableHttpClientBuilder.create()
                                .setRetryMax(5)
                                .setTLSEnabled(false)
                                .build()
                        )
                        .setMoogEndpoint(config.getOcMoogEndpoint())
                        .setAuthTokenProvider(() ->
                                secretProvider.getString(config.getOcMoogAuthTokenKeyName())
                        )
                )
                .setFormatterBuilder(DefaultOcRestFormatter.builder()
                        .setHostname(config.getOcHost()))
                .build();


        final Map<String, Boolean> namespaceWhitelist = new HashMap<>();
        for (String namespace : config.getOcWhitelistNamespaces()) {
            if (namespace == null || namespace.isEmpty()) {
                continue;
            }
            namespaceWhitelist.put(namespace.toLowerCase(), true);
        }
        final Map<Long, Boolean> idsWhitelist = new HashMap<>();
        for (String id : config.getOcWhitelistIds()) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            try {
                idsWhitelist.put(Long.valueOf(id), true);
            } catch (NumberFormatException e) {
                LOG.warn("Failed to parse whitelisted alert id '{}'", id);
            }
        }

        return new ChainableProcessor<MessageKit, MessageKit>(emitter) {
            @Override
            public void process(MessageKit mk)
            {
                if (namespaceWhitelist.containsKey(mk.getNamespace().toLowerCase())
                        || idsWhitelist.containsKey(mk.getAlertId())
                ) {
                    LOG.debug("Allow OC REST alert: namespace={}, alert_id={}",
                            mk.getNamespace(), mk.getAlertId());
                    submit(mk);
                }
            }
        };
    }

    private Processor<MessageKit> createPrismEmitter()
    {
        return PrismEmitter.builder()
                .setClientBuilder(DefaultPrismClient.builder()
                        .setHttpClient(CloseableHttpClientBuilder.create()
                                .setRetryMax(3)
                                .setTLSEnabled(true)
                                .setInsecureSkipVerify(config.isTlsInsecureSkipVerify())
                                .setCertificatePath(config.getTlsCertificatePath())
                                .setPrivateKeyPath(config.getTlsPrivateKeyPath())
                                .setTrustStorePath(config.getTlsTrustStorePath())
                                .setTrustStorePassword(config.getTlsTrustStorePassword())
                                .build()
                        )
                        .setEndpoint(config.getPrismEndpoint())
                )
                .setFormatterBuilder(DefaultPrismFormatter.builder()
                        .setHostname(config.getPrismHost()))
                .build();
    }

    private Processor<MessageKit> createWebhookEmitter()
    {
        return WebhookEmitter.builder()
                .setClient(DefaultWebhookClient.builder()
                        .setHttpClient(CloseableHttpClientBuilder.create()
                                .setRetryMax(3)
                                .setTLSEnabled(true)
                                .setTrustStorePath(config.getTlsTrustStorePath())
                                .setTrustStorePassword(config.getTlsTrustStorePassword())
                                .setPrivateKeyPath(config.getTlsPrivateKeyPath())
                                .setCertificatePath(config.getTlsCertificatePath())
                                .build()
                        )
                        .build()
                )
                .setFormatter(DefaultWebhookFormatter.builder().build())
                .build();
    }

    private Processor<MessageKit> proxy(final Processor<MessageKit> next)
    {
        final Processor<MessageKit> logWrappedNext = messageKit -> {
            AlertGroup alertGroup = messageKit.getAlertGroup();
            LOG.debug("Received new message kit: alert_id={}, group_key={}, alerts={}",
                    messageKit.getAlertId(), alertGroup.getGroupKey(), alertGroup.getAlerts());

            next.process(messageKit);
        };

        if (config.isDebug()) {
            LOG.info("Debug mode is enabled. Overriding contacts.");
            return new ContactOverrider(logWrappedNext, config);
        } else {
            return logWrappedNext;
        }
    }

    private Type getEmitterType()
    {
        final String type = config.getEmitterType().toUpperCase();
        return Type.valueOf(type);
    }

    /**
     * @return emitter of configured type.
     */
    private Processor<MessageKit> getEmitter()
    {
        final Type type = getEmitterType();
        switch (type) {
            case EMAIL:
                return createEmailEmitter();
            case OPSGENIE:
                return createOpsGenieEmitter();
            case SLACK:
                return createSlackEmitter();
            case OC:
                return createOcEmitter();
            case OCREST:
                return createOcRestEmitter();
            case PRISM:
                return createPrismEmitter();
            case WEBHOOK:
                return createWebhookEmitter();
            default:
                throw new IllegalArgumentException(
                        "Unsupported emitter type" + type.name());
        }
    }

    private Runnable createPipeline()
    {
        final Processor<MessageKit> emitter = getEmitter();
        final Processor<MessageKit> emitterProxy = proxy(emitter);
        final Processor<MessageKit> syntheticAlertCounter =
                SyntheticMessageKitCounter.create(
                        emitterProxy,
                        config.getSyntheticAlertIds()
                );
        final KafkaMessageKitReader kafkaReader = KafkaMessageKitReader
                .builder()
                .setNext(syntheticAlertCounter)
                .setComponentId(MESSAGE_KIT_READER_COMPONENT_ID)
                .setSerializer(MessageKitSerializer.instance())
                .setKafkaStream(KafkaStream.builder()
                        .setAutoCommitEnable(
                                config.getKafkaAutoCommitEnable())
                        .setAutoOffsetReset(
                                config.getKafkaAutoOffsetReset())
                        .setRebalanceBackoffMs(
                                config.getKafkaRebalanceBackoffMs())
                        .setRebalanceRetriesMax(
                                config.getKafkaRebalanceRetriesMax())
                        .setZookeeperConnectionTimeoutMs(
                                config.getKafkaZookeeperConnectionTimeoutMs())
                        .setZookeeperSessionTimeoutMs(
                                config.getKafkaZookeeperSessionTimeoutMs())
                        .setZookeeperConnect(
                                config.getKafkaZookeeperConnectAsString())
                        .setGroupId(config.getKafkaGroupId())
                        .setTopic(config.getKafkaTopic())
                        .setConsumerId(Utils.getConsumerIdWithRandomPrefix())
                        .build())
                .build();

        return () -> {
            try {
                LOG.debug("Starting KafkaMessageKitReader.");
                kafkaReader.run();
            } finally {
                kafkaReader.close();
                LOG.debug("Closed KafkaMessageKitReader.");
            }
        };
    }

    public void start()
    {
        LOG.debug("Submitting NotificationEmitter pipeline to executor.");
        final Future future = executor.submit(pipeline);

        try {
            LOG.debug("Waiting for execution result.");
            future.get();
        } catch (Exception e) {
            LOG.error("Exception running NotificationProcessor.", e);
            throw new RuntimeException(e);
        }
    }
}
