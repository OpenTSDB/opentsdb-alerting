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

package net.opentsdb.horizon.alerts.state.persistence;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerts.Monitoring;

@Slf4j
public class PulsarStatePersistor
        extends AbstractStatePersistor
        implements Closeable {

    private static final Logger LOG =
            LoggerFactory.getLogger(PulsarStatePersistor.class);

    private final Producer producer;

    private final String topicName;

    PulsarStatePersistor(final Producer producer) {
        Objects.requireNonNull(producer, "producer cannot be null");
        this.producer = producer;
        this.topicName = producer.getTopic();
    }

    @Override
    protected void persist(byte[] payload) {
        Monitoring.get().gaugePulsarSendPayloadSize(payload.length);
        try {
            if(log.isTraceEnabled()) {
                log.trace("Sending message to Pulsar");
            }
            final long start = System.currentTimeMillis();
            producer.newMessage()
                    .value(payload)
                    .send();
            final long end = System.currentTimeMillis();
            Monitoring.get().timePulsarSend((end - start));
            Monitoring.get().countPulsarSendOk();
        } catch (PulsarClientException e) {
            Monitoring.get().countPulsarSendErr();
            LOG.error("Failed to persist: topic={}", topicName, e);
        }
    }

    @Override
    public void close() throws IOException {
        producer.close();
    }

    // ------------ Statics ------------ //

    public static PulsarStatePersistor create(final Producer producer) {
        return new PulsarStatePersistor(producer);
    }

    // TODO unused. Delete this.
    public static PulsarStatePersistor create(final PulsarClient pulsarClient,
                                              final String topicName) {
        try {
            Producer producer = pulsarClient.newProducer()
                    .topic(topicName)
                    .create();
            return create(producer);
        } catch (PulsarClientException e) {
            throw new RuntimeException("Unable to create producer for: " +
                    "topicName=" + topicName, e);
        }
    }

    public static PulsarStatePersistor create(final PulsarClient pulsarClient,
                                              final String topicName,
                                              final Producer producer) {
        return create(producer);
    }
}
