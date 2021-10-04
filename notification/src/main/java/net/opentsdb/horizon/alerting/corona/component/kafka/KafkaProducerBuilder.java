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

package net.opentsdb.horizon.alerting.corona.component.kafka;

import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;

public class KafkaProducerBuilder {

    public static KafkaProducerBuilder create()
    {
        return new KafkaProducerBuilder();
    }

    private final Properties properties;

    private KafkaProducerBuilder()
    {
        this.properties = new Properties();
    }

    public KafkaProducerBuilder setMetadataBrokerList(String brokers)
    {
        properties.setProperty("metadata.broker.list", brokers);
        return this;
    }

    public KafkaProducerBuilder setRequestRequiredAcks(String requiredAcks)
    {
        properties.setProperty("request.required.acks", requiredAcks);
        return this;
    }

    public KafkaProducerBuilder setRequestTimeoutMs(String timeoutMs)
    {
        properties.setProperty("request.timeout.ms", timeoutMs);
        return this;
    }

    public KafkaProducerBuilder setMessageSendMaxRetries(String maxRetries)
    {
        properties.setProperty("messagekit.send.max.retries", maxRetries);
        return this;
    }

    public KafkaProducerBuilder setRetryBackoffMs(String backoffMs)
    {
        properties.setProperty("retry.backoff.ms", backoffMs);
        return this;
    }

    public KafkaProducerBuilder setProducerType(String producerType)
    {
        properties.setProperty("producer.type", producerType);
        return this;
    }

    public KafkaProducerBuilder setKeySerializerClass(String className)
    {
        properties.setProperty("key.serializer.class", className);
        return this;
    }

    public KafkaProducerBuilder setCompressionCodec(String codec)
    {
        properties.setProperty("compression.codec", codec);
        return this;
    }

    public KafkaProducerBuilder setBatchNumMessages(String numMessages)
    {
        properties.setProperty("batch.num.messages", numMessages);
        return this;
    }

    public <K, V> Producer<K, V> build()
    {
        final ProducerConfig config = new ProducerConfig(properties);
        return new Producer<>(config);
    }
}
