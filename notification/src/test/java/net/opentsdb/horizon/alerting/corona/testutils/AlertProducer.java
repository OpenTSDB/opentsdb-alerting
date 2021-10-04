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

package net.opentsdb.horizon.alerting.corona.testutils;

import com.esotericsoftware.kryo.io.Output;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import net.opentsdb.horizon.alerting.corona.component.kafka.KafkaProducerBuilder;
import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.GenericAlertSerializer;

public class AlertProducer {

    private final AbstractSerializer<Alert> alertSerializer;

    private final Producer<String, byte[]> producer;

    public AlertProducer(String brokerList)
    {
        this.alertSerializer = new GenericAlertSerializer();
        this.producer = KafkaProducerBuilder.create()
                .setProducerType("async")
                .setBatchNumMessages("2000")
                .setCompressionCodec("gzip")
                .setKeySerializerClass("kafka.serializer.StringEncoder")
                .setMessageSendMaxRetries("200")
                .setRequestTimeoutMs("500")
                .setRequestRequiredAcks("0")
                .setRetryBackoffMs("500")
                .setMetadataBrokerList(brokerList)
                .build();
    }

    public void send(String topic, String key, Alert alert)
    {
        final byte[] body;
        try (final Output output = new Output(1024, -1)) {
            alertSerializer.write(null, output, alert);
            body = output.toBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        producer.send(new KeyedMessage<>(topic, key, body));
    }

    public void send(String topic, Alert alert)
    {
        send(topic, null, alert);
    }

    public void stop()
    {
        producer.close();
    }
}

