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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;
import org.slf4j.Logger;

import kafka.message.MessageAndMetadata;

import net.opentsdb.horizon.alerting.corona.component.DaemonThreadFactory;
import net.opentsdb.horizon.alerting.corona.component.kafka.KafkaStream;
import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;

/**
 * Reads messages from a KafkaStream and submits them to the next
 * {@link Processor}.
 *
 * @param <K> MessageAndMetadata key type
 * @param <V> MessageAndMetadata value type
 * @param <O> type of objects passed to the next processor
 */
public abstract class KafkaStreamReader<K, V, O>
        extends ChainableProcessor<MessageAndMetadata<K, V>, O>
        implements AutoCloseable, Runnable
{

    /* ------------ Constants ------------ */

    private static final int NO_DELAY = 0;

    private static final int EVERY_ONE = 1;

    /* ------------ Fields ------------ */

    protected final Logger logger;

    private final KafkaStream<K, V> kafkaStream;

    protected final String componentId;

    private final ScheduledExecutorService executor;

    /* ------------ Constructor ------------ */

    protected KafkaStreamReader(final Builder<K, V, O, ?> builder)
    {
        super(builder);
        Objects.requireNonNull(builder.logger,
                "logger from getLogger cannot be null");
        Objects.requireNonNull(builder.kafkaStream,
                "kafkaStream cannot be null");
        Objects.requireNonNull(builder.componentId,
                "componentId cannot be null");
        this.logger = builder.logger;
        this.kafkaStream = builder.kafkaStream;
        this.componentId = builder.componentId;
        this.executor =
                Executors.newSingleThreadScheduledExecutor(
                        DaemonThreadFactory.INSTANCE
                );
    }

    /* ------------ Methods ------------ */

    private void reportKafkaQueueSize()
    {
        AppMonitor.get().gaugeKafkaQueueSize(kafkaStream.size(), componentId);
    }

    /**
     * Starts reading from the KafkaStream.
     *
     * <p> If exception from the downstream is caught, the stream is closed,
     * exception logged, and the method returns.
     */
    @Override
    public void run()
    {
        logger.debug("Schedule queue size checks: delay={}, periodMin={}.",
                NO_DELAY, EVERY_ONE);

        executor.scheduleAtFixedRate(
                this::reportKafkaQueueSize,
                NO_DELAY,
                EVERY_ONE,
                TimeUnit.MINUTES
        );

        logger.debug("Running KafkaStreamReader.");
        try {
            for (final MessageAndMetadata<K, V> mm : kafkaStream) {
                process(mm);
                AppMonitor.get().countKafkaMessageRead(componentId);
            }
        } catch (Throwable t) {
            close();
            if (t.getClass() == InterruptedException.class) {
                Thread.currentThread().interrupt();
            }
            logger.error("Stream processing exited on exception", t);
        }
    }

    /**
     * Closes underlying KafkaStream.
     */
    @Override
    public void close()
    {
        kafkaStream.close();
        executor.shutdownNow();
    }

    /* ------------ Builder ------------ */

    protected abstract static class Builder<K, V, O, B extends Builder<K, V, O, B>>
            extends ChainableProcessor.Builder<O, B>
    {

        private Logger logger;

        private KafkaStream<K, V> kafkaStream;

        private String componentId;

        /**
         * @param logger logger
         * @return builder
         */
        public B setLogger(final Logger logger)
        {
            this.logger = logger;
            return self();
        }

        /**
         * @return true if logger is set, otherwise false
         */
        protected boolean hasLogger()
        {
            return logger != null;
        }

        /**
         * @param kafkaStream KafkaStream to readAddedFields from
         * @return builder
         */
        public B setKafkaStream(final KafkaStream<K, V> kafkaStream)
        {
            this.kafkaStream = kafkaStream;
            return self();
        }

        /**
         * @param componentId component id for instrumentation purposes
         * @return builder
         */
        public B setComponentId(final String componentId)
        {
            this.componentId = componentId;
            return self();
        }
    }
}
