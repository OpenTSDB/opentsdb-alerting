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

import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class KafkaProducerPool {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducerPool.class);

    public static boolean IS_LOCAL = false;

    private static int numProducers = 0;

    private kafka.javaapi.producer.Producer<String, byte[]>[] keyedProducers;

    private static AtomicInteger roundRobinInc = new AtomicInteger(0);

    private volatile static boolean inited;

    private static class POOL_WRAPPER {

        private static KafkaProducerPool kafkaProducerPool = new KafkaProducerPool();

        private static KafkaProducerPool getKafkaProducerPool() {
            return kafkaProducerPool;
        }

    }

    private KafkaProducerPool() {

    }

    public static void initPool(int numOfProducers, String brokerList,
                                             String messageSendMaxRetries,
                                             String retryBackOff) {
        initPoolLocal(numOfProducers,getKafkaProps(brokerList,
                messageSendMaxRetries, retryBackOff));
    }

    public static void initPool(int numOfProducers,Properties kafkaProps) {

            initPoolLocal(numOfProducers, new ProducerConfig(kafkaProps));

    }

    private synchronized static void initPoolLocal(int numOfProducers, ProducerConfig kafkaConfig) {
        if(!inited) {
            final KafkaProducerPool kafkaProducerPool = POOL_WRAPPER.getKafkaProducerPool();
            numProducers = numOfProducers;
            kafkaProducerPool.keyedProducers = new Producer[numOfProducers];
            LOG.info("Initializing " + numOfProducers + " keyed kafka producers");

            for (int i = 0; i < numOfProducers; i++) {
                kafkaProducerPool.keyedProducers[i] = new Producer<String, byte[]>(kafkaConfig);
            }
            inited = true;
        }
    }

    public static Producer<String, byte[]> getProducer() {

        if(!inited) {
            throw new AssertionError("The Kafka producer pool needs to be inited first");
        }

        return POOL_WRAPPER.getKafkaProducerPool().getKeyedProducer();
    }

    private Producer<String, byte[]> getKeyedProducer() {
        if (numProducers == 1) {
            return keyedProducers[0];
        }
        return keyedProducers[getProducerIndex()];
    }

    private static int getProducerIndex() {
        return Math.abs(roundRobinInc.getAndIncrement() % numProducers);
    }

    private static ProducerConfig getKafkaProps(String brokerList, String messageSendMaxRetires, String retryBackOff) {
        Properties props = new Properties();
        props.put("metadata.broker.list", brokerList);
        props.put("request.required.acks","0");
        props.put("request.timeout.ms","10000");

        if (IS_LOCAL) {
            props.put("message.send.max.retries", "100");
            props.put("retry.backoff.ms", "100");
        } else {
            props.put("message.send.max.retries", messageSendMaxRetires);
            props.put("retry.backoff.ms", retryBackOff);
        }
        // props.put("producer.type", "async");
        props.put("key.serializer.class", "kafka.serializer.StringEncoder");
        props.put("compression.codec", "gzip");
        props.put("batch.num.messages", "2000");

        if (!IS_LOCAL) {
            props.put("producer.type", "async");
        }
        props.put("partitioner.class", KafkaHashBasedPartitioner.class.getCanonicalName());
        return new ProducerConfig(props);
    }


}
