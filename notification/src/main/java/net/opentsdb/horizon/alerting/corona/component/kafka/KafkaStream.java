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

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Properties;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import kafka.consumer.ConsumerConfig;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.Decoder;
import kafka.serializer.DefaultDecoder;

public class KafkaStream<K, V>
        implements Closeable, Iterable<MessageAndMetadata<K, V>>
{

    public static Builder<byte[], byte[]> builder()
    {
        return new Builder<>();
    }

    private final ConsumerConnector consumerConnector;

    private final kafka.consumer.KafkaStream<K, V> kafkaStream;

    /**
     * Stream's internal queue. Use only to get size, as in {@link #size()}.
     * <p>
     * Release the reference in {@link #close()}.
     */
    private BlockingQueue streamQueue;

    private KafkaStream(final ConsumerConnector consumerConnector,
                        final kafka.consumer.KafkaStream<K, V> kafkaStream)
    {
        Objects.requireNonNull(consumerConnector,
                "consumerConnector cannot be null");
        Objects.requireNonNull(kafkaStream, "kafkaStream cannot be null");
        final String streamClassName = kafkaStream.getClass().getName();
        if (!streamClassName.equals("kafka.consumer.KafkaStream")) {
            throw new IllegalArgumentException(
                    "expected kafka.consumer.KafkaStream. Given: "
                            + streamClassName);
        }

        this.consumerConnector = consumerConnector;
        this.kafkaStream = kafkaStream;

        try {
            final Field queueField =
                    kafkaStream.getClass().getDeclaredField("queue");
            queueField.setAccessible(true);
            this.streamQueue = (BlockingQueue) queueField.get(kafkaStream);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalArgumentException(
                    "expected `queue` field on the kafkaStream", e);
        }
    }

    public int size()
    {
        // Feels like kafkaStream.size() does something very funky,
        // e.g consuming alert messages. Anyway the implementation
        // is not efficient.
        return streamQueue.size();
    }

    @Override
    @Nonnull
    public Iterator<MessageAndMetadata<K, V>> iterator()
    {
        return kafkaStream.iterator();
    }

    @Override
    public void forEach(Consumer<? super MessageAndMetadata<K, V>> action)
    {
        kafkaStream.forEach(action);
    }

    @Override
    public Spliterator<MessageAndMetadata<K, V>> spliterator()
    {
        return kafkaStream.spliterator();
    }

    @Override
    public void close()
    {
        streamQueue = null;
        consumerConnector.shutdown();
    }

    public static class Builder<K, V> {

        private static final Decoder DEFAULT_DECODER = new DefaultDecoder(null);

        private final Properties properties;

        private Decoder<K> keyDecoder;

        private Decoder<V> valueDecoder;

        private String topic;

        /**
         * Unchecked assignments are fine, since {@link KafkaStream#builder()}
         * creates a {@code Builder<byte[], byte[]>}.
         */
        @SuppressWarnings("unchecked")
        private Builder()
        {
            this.properties = new Properties();
            // Unchecked assignments:
            this.keyDecoder = DEFAULT_DECODER;
            this.valueDecoder = DEFAULT_DECODER;
        }

        public Builder<K, V> setTopic(String topic)
        {
            this.topic = topic;
            return this;
        }

        public Builder<K, V> setZookeeperConnect(String zookeeperConnect)
        {
            properties.setProperty("zookeeper.connect", zookeeperConnect);
            return this;
        }

        public Builder<K, V> setGroupId(String groupId)
        {
            properties.setProperty("group.id", groupId);
            return this;
        }

        public Builder<K, V> setZookeeperSessionTimeoutMs(String timeoutMs)
        {
            properties.setProperty("zookeeper.session.timeout.ms", timeoutMs);
            return this;
        }

        public Builder<K, V> setZookeeperConnectionTimeoutMs(String timeoutMs)
        {
            properties.setProperty("zookeeper.connection.timeout.ms",
                    timeoutMs);
            return this;
        }

        public Builder<K, V> setRebalanceBackoffMs(String backoffMs)
        {
            properties.setProperty("rebalance.backoff.ms", backoffMs);
            return this;
        }

        public Builder<K, V> setRebalanceRetriesMax(String retriesMax)
        {
            properties.setProperty("rebalance.retries.max", retriesMax);
            return this;
        }

        public Builder<K, V> setAutoCommitEnable(String autoCommitEnable)
        {
            properties.setProperty("auto.commit.enable", autoCommitEnable);
            return this;
        }

        public Builder<K, V> setAutoOffsetReset(String autoOffsetReset)
        {
            properties.setProperty("auto.offset.reset", autoOffsetReset);
            return this;
        }

        public Builder<K, V> setConsumerId(String consumerId)
        {
            properties.setProperty("consumer.id", consumerId);
            return this;
        }

        public <O> Builder<O, V> setKeyDecoder(Decoder<O> keyDecoder)
        {
            @SuppressWarnings("unchecked")
            final Builder<O, V> other = (Builder<O, V>) this;
            other.keyDecoder = keyDecoder;
            return other;
        }

        public <O> Builder<K, O> setValueDecoder(Decoder<O> valueDecoder)
        {
            @SuppressWarnings("unchecked")
            final Builder<K, O> other = (Builder<K, O>) this;
            other.valueDecoder = valueDecoder;
            return other;
        }

        public KafkaStream<K, V> build()
        {
            final ConsumerConfig config = new ConsumerConfig(properties);
            final ConsumerConnector connector =
                    kafka.consumer.Consumer.createJavaConsumerConnector(config);

            final kafka.consumer.KafkaStream<K, V> stream =
                    connector
                            .createMessageStreams(
                                    new HashMap<String, Integer>() {{
                                        put(topic, 1);
                                    }},
                                    keyDecoder,
                                    valueDecoder
                            )
                            .get(topic)
                            .get(0);

            return new KafkaStream<>(connector, stream);
        }
    }
}
