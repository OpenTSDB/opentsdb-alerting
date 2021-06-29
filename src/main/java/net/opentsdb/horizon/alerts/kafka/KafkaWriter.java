/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.kafka;

import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.Monitoring;
import net.opentsdb.horizon.alerts.OutputWriter;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.model.tsdb.Datum;
import net.opentsdb.horizon.alerts.model.tsdb.YmsStatusEvent;
import net.opentsdb.horizon.alerts.serde.Serde;
import kafka.producer.KeyedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Uses a pool to write to kafka
 */
public class KafkaWriter implements OutputWriter {

    private final EnvironmentConfig environmentConfig = new EnvironmentConfig();
    private final Serde serde = new Serde();

    private final String status_topic;

    private final String alerts_topic;

    private static final Logger LOG = LoggerFactory.getLogger(KafkaWriter.class);

    public KafkaWriter() {
        KafkaProducerPool.initPool(environmentConfig.getNumberOfProducers(),
                environmentConfig.getKafkaBrokersFormatted(),
                environmentConfig.getMessageSendMaxRetries(),
                environmentConfig.getRetryBackOff());

        status_topic = environmentConfig.getStatusTopic();
        alerts_topic = environmentConfig.getAlertsTopic();
    }

    public void sendAlertEvent(AlertEventBag alertEventBag) {

        if(environmentConfig.fireEmails()) {
            LOG.info("Fire emails is true so, not writing to kafka");
            return;
        }

        LOG.info("Writing {} to alert {} topic",
                alertEventBag.toString(),alerts_topic);

        alertEventBag.getAlertEvents()
            .stream()
            .forEach(alertEvent -> {
                final byte[] bytes = serde.kryoSerializeEvent(alertEvent);
                KeyedMessage dataToAlert = new KeyedMessage<>(alerts_topic,
                                               String.valueOf(alertEvent.getAlertHash()),
                                               bytes);
                KafkaProducerPool.getProducer().send(dataToAlert);
                Monitoring.get().incAlertsWrittenToKafka(alertEvent.getAlertId(),
                                                    alertEvent.getNamespace());
            });

    }

    public void sendStatusEvent(YmsStatusEvent ymsStatusEvent) {
        if(environmentConfig.fireEmails()) {
            LOG.info("Fire emails is true so, not writing to kafka");
            return;
        }

        LOG.info("Writing {} to status {} topic",
                ymsStatusEvent,status_topic);
        final Datum datum = ymsStatusEvent.getDatum();

        final TimeUnit timeUnit = datum.getTimeUnit();

        final long timestampInMicros = timeUnit.toMicros(datum.getTimestamp());

        datum.setTimestamp(timestampInMicros);
        datum.setTimeUnit(TimeUnit.MICROSECONDS);

        final byte[] bytes = serde.kryoSerializeYmsEvent(ymsStatusEvent);
                    KeyedMessage dataToStatus = new KeyedMessage<>(status_topic,
                            String.valueOf(ymsStatusEvent.hashCode()),
                            bytes);
                    KafkaProducerPool.getProducer().send(dataToStatus);
                    Monitoring.get().incStatusesWrittenToKafka(
                            Long.parseLong(String.valueOf(ymsStatusEvent.getTags()
                                            .get("horizon_alert_id"))),
                            datum.getCluster());
    }

}
