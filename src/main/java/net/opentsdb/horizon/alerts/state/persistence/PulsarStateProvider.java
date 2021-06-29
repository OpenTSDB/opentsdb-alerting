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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.state.bootstrap.BootstrapStateEntry;
import net.opentsdb.horizon.alerts.state.bootstrap.BootstrapStateStore;
import net.opentsdb.horizon.alerts.state.bootstrap.BootstrapStateStoreImpl;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerts.Monitoring;
import net.opentsdb.horizon.alerts.state.AlertStateStore;

import com.esotericsoftware.kryo.io.Input;

/**
 * Be advised, that {@link #get(AlertConfig)} will return non-empty optional
 * only on the first call, if an element exists.
 *
 * TODO: The only-once get semantics is a hack to inject a state into
 *       an executor only once. This should be removed.
 */
public class PulsarStateProvider implements StateProvider, Closeable {

    private static final Logger LOG =
            LoggerFactory.getLogger(PulsarStateProvider.class);

    private static final int READ_TIMEOUT_SEC = 15;

    private final Consumer consumer;

    private final long cutoffTimeSec;

    private volatile BootstrapStateStore bootstrapStateStore;

    protected PulsarStateProvider(final Consumer consumer,
                                  final long cutoffTimeSec) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        this.consumer = consumer;
        this.cutoffTimeSec = cutoffTimeSec;
    }

    public void bootstrap(final List<AlertConfig> configs) {
        // TODO: Remove, this is a plug to make things work.
        bootstrapStateStore = createBootstrapStateStore(configs);

        final Input input = new Input();

        final long startMs = System.currentTimeMillis();
        while (true) {
            final Message message;
            try {
                // TODO: Add retries.
                message = consumer.receive(READ_TIMEOUT_SEC, TimeUnit.SECONDS);
                Monitoring.get().countPulsarReadOk();
            } catch (PulsarClientException e) {
                Monitoring.get().countPulsarReadErr();
                throw new RuntimeException(
                        "Bootstrap failed, unexpected read exception", e
                );
            }
            if (message == null) {
                final long stopMs = System.currentTimeMillis();
                Monitoring.get().timeBootstrapTimeMs(stopMs - startMs);
                LOG.debug("Initialization complete, read timed out: timout_sec={}",
                        READ_TIMEOUT_SEC);
                return;
            }
            input.setBuffer(message.getData());
            final boolean shouldContinue = updateState(input);
            if (!shouldContinue) {
                return;
            }
        }
    }

    private BootstrapStateStore createBootstrapStateStore(
            final List<AlertConfig> configs) {
        return new BootstrapStateStoreImpl(
                new BatchDeserializer(),
                Collections.unmodifiableList(
                        new ArrayList<>(configs)
                )
        );
    }

    /**
     * Updates state.
     *
     * @param input input buffer
     * @return true if state updates should continue, false to stop.
     */
    private boolean updateState(final Input input) {
        final byte version = input.readByte();
        if (version != AbstractStatePersistor.VERSION) {
            LOG.error("Unsupported package version: expected={}, received={}",
                    AbstractStatePersistor.VERSION, version);
            // Continue updates.
            return true;
        }

        final long alertId = input.readLong();
        final long runStampSec = input.readLong();
        final String namespace = input.readString();

        LOG.trace("Update state: alert_id={}, run_stamp_sec={}, buffer_size_bytes={}",
                alertId, runStampSec, input.getBuffer().length);

        if (runStampSec >= cutoffTimeSec) {
            LOG.debug("Cutoff time reached: cutoff_time_sec={}, run_stamp_sec={}",
                    cutoffTimeSec, runStampSec);
            // Abort updates.
            //
            // TODO: We make a strong assumption that all run stamps are
            //       monotonically increasing, which might not be true.
            return false;
        }

        try {
            bootstrapStateStore.update(input, alertId, namespace, runStampSec);
        } catch (Exception e) {
            LOG.error("Single state update failed: alert_id={}, run_stamp_sec={}.",
                    alertId, runStampSec, e);
        }

        // Continue updates.
        return true;
    }

    @Override
    public void close() throws IOException {
        consumer.close();
    }

    /**
     * Get the alert state store for the given config.
     *
     * The enforces ONLY-ONCE semantics: that is, a store, if exists, is
     * returned only on the first request.
     *
     * @param config alert configuration
     * @return optional alert state store.
     */
    @Override
    public Optional<AlertStateStore> get(final AlertConfig config) {
        if (bootstrapStateStore == null) {
            // TODO: Remove, this is a plug to make things work.
            throw new RuntimeException("bootstrap(.) has to be called first");
        }

        // TODO: This class doesn't have to implement StateProvider.
        //       Ideally, it should be another class, which handles
        //       bootstrap and catch up logic.
        final Optional<BootstrapStateEntry> maybeEntry =
                bootstrapStateStore.get(config.getAlertId());

        if (!maybeEntry.isPresent()) {
            return Optional.empty();
        }

        try {
            // TODO: This is where catch up should happen.
            final BootstrapStateEntry entry = maybeEntry.get();

            // TODO: Complete might be at least one run behind, definitely
            //       so if the entry.hasIncomplete() is true.
            if (entry.hasComplete()) {
                return Optional.of(entry.getComplete());
            }
            // Incomplete means that the state transfer did not receive
            // the last state closing message.
            return Optional.ofNullable(entry.getIncomplete());
        } finally {
            // Remove the entry for effectively read once semantics.
            bootstrapStateStore.remove(config.getAlertId());
        }
    }

    public static PulsarStateProvider create(final PulsarClient pulsarClient,
                                             final String topicName,
                                             final String consumerName,
                                             final long cutoffTimeSec) {
        final Consumer consumer;
        try {

            LOG.info("Creating Pulsar consumer: {} for topic: {} ",
                    consumerName, topicName);
            consumer = pulsarClient.newConsumer()
                    .topic(topicName)
                    .consumerName(consumerName)
                    .subscribe();
        } catch (PulsarClientException e) {
            throw new RuntimeException(e);
        }

        return new PulsarStateProvider(consumer, cutoffTimeSec);
    }
}
