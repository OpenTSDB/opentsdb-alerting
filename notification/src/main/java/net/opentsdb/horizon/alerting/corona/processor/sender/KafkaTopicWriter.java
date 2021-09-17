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

package net.opentsdb.horizon.alerting.corona.processor.sender;

import java.util.Objects;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;

import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;

public class KafkaTopicWriter implements Processor<byte[]> {

    /* ------------ Fields ------------ */

    private final String topic;

    private final Producer<String, byte[]> kafkaProducer;

    /* ------------ Constructor ------------ */

    private KafkaTopicWriter(final Builder builder)
    {
        Objects.requireNonNull(builder.topic, "topic cannot be null");
        Objects.requireNonNull(builder.kafkaProducer,
                "kafkaProducer cannot be null");
        this.topic = builder.topic;
        this.kafkaProducer = builder.kafkaProducer;
    }

    /* ------------ Methods ------------ */

    @Override
    public void process(byte[] item)
    {
        kafkaProducer.send(new KeyedMessage<>(topic, item));
        AppMonitor.get().countKafkaMessageWrite(topic);
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private String topic;

        private Producer<String, byte[]> kafkaProducer;

        private Builder() {}

        public Builder setTopic(final String topic)
        {
            this.topic = topic;
            return this;
        }

        public Builder setKafkaProducer(
                final Producer<String, byte[]> kafkaProducer)
        {
            this.kafkaProducer = kafkaProducer;
            return this;
        }

        public KafkaTopicWriter build()
        {
            return new KafkaTopicWriter(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
