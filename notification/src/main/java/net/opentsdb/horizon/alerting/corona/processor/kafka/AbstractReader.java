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

package net.opentsdb.horizon.alerting.corona.processor.kafka;

import java.util.Objects;

import kafka.message.MessageAndMetadata;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;

/**
 * Reads {@code MessageAndMetadata<byte[], byte[]>} from KafkaStream,
 * deserializes messages to the output type objects, and submits them
 * to the next {@link Processor}.
 *
 * @param <T> type of objects passed to the next processor
 */
public abstract class AbstractReader<T>
        extends KafkaStreamReader<byte[], byte[], T>
{

    /* ------------ Fields ------------ */

    private final AbstractSerializer<T> serializer;

    /* ------------ Constructor ------------ */

    protected AbstractReader(final Builder<T, ?> builder)
    {
        super(builder);
        Objects.requireNonNull(builder.serializer, "serializer cannot be null");
        this.serializer = builder.serializer;
    }

    /* ------------ Methods ------------ */

    /**
     * Deserializes given MessageAndMetadata object and passes the result to
     * the next processor.
     *
     * @param mm MessageAndMetadata object with byte[] key and value
     */
    @Override
    public void process(final MessageAndMetadata<byte[], byte[]> mm)
    {
        final byte[] bytes = mm.message();
        if (Objects.isNull(bytes)) {
            logger.error("Received null as Kafka message: topic={}, key={}, message={}",
                    mm.topic(), mm.key(), mm);
            return;
        }
        AppMonitor.get().gaugeKafkaReadMesssageSize(bytes.length, mm.topic());

        final T message;
        try {
            message = serializer.fromBytes(bytes);
            AppMonitor.get().countKafkaDeserializationSuccess(
                    componentId,
                    message.getClass().getSimpleName()
            );
        } catch (Exception e) {
            AppMonitor.get().countKafkaDeserializationFailed(componentId);
            logger.error(
                    "Failed to deserialize: topic = {}, key = {}, bytes = {}",
                    mm.topic(), mm.key(), bytes, e);
            return;
        }
        submit(message);
    }

    /* ------------ Builder ------------ */

    protected abstract static class Builder<T, B extends Builder<T, B>>
            extends KafkaStreamReader.Builder<byte[], byte[], T, B>
    {

        private AbstractSerializer<T> serializer;

        /**
         * Set type serializer.
         *
         * @param serializer type serializer
         * @return builder
         */
        public B setSerializer(final AbstractSerializer<T> serializer)
        {
            this.serializer = serializer;
            return self();
        }
    }
}
