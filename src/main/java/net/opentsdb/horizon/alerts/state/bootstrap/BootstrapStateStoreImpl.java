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

package net.opentsdb.horizon.alerts.state.bootstrap;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.state.persistence.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerts.state.AlertStateStore;
import net.opentsdb.horizon.alerts.state.ModifiableAlertStateStore;
import net.opentsdb.horizon.alerts.state.impl.AlertStateStoreImpl;

import com.esotericsoftware.kryo.io.Input;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class BootstrapStateStoreImpl implements BootstrapStateStore {

    private static final Logger LOG =
            LoggerFactory.getLogger(BootstrapStateStoreImpl.class);

    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();

    // Technically, previous
    private final Long2LongMap completeRunStamps = new Long2LongOpenHashMap();

    // Technically, current
    private final Long2LongMap incompleteRunStamps = new Long2LongOpenHashMap();

    private final Long2ObjectMap<ModifiableAlertStateStore> completes =
            new Long2ObjectOpenHashMap<>();

    private final Long2ObjectMap<ModifiableAlertStateStore> incompletes =
            new Long2ObjectOpenHashMap<>();

    private final Deserializer deserializer;

    // TODO: After the fact, we should not have these references here.
    private final Long2ObjectMap<AlertConfig> configs;

    public BootstrapStateStoreImpl(final Deserializer deserializer,
                                   final List<AlertConfig> configs) {
        Objects.requireNonNull(deserializer, "deserializer cannot be null");
        Objects.requireNonNull(configs, "configs cannot be null");
        this.deserializer = deserializer;
        this.configs =
                new Long2ObjectOpenHashMap<>(
                        configs.stream().collect(
                                Collectors.toMap(
                                        AlertConfig::getAlertId,
                                        Function.identity()
                                )
                        )
                );
    }

    @Override
    public void update(final Input input,
                       final long alertId,
                       final String namespace,
                       final long runStampSec) {
        rwlock.writeLock().lock();
        try {
            doUpdate(input, alertId, namespace, runStampSec);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    private void doUpdate(final Input input,
                          final long alertId,
                          final String namespace,
                          final long runStampSec) {
        if (!configs.containsKey(alertId)) {
            LOG.debug("State update. No configuration found: alert_id={}", alertId);
            return;
        }

        final byte partId = input.readByte();

        LOG.debug("State update: alert_id={}, namespace={}, run_stamp_sec={}, part_id={}",
                alertId, namespace, runStampSec,
                partId == 1 ? "header" : partId == 2 ? "state" : "footer"
        );

        if (!configs.containsKey(alertId)) {
            LOG.debug("Dropping state update, config not found: " +
                            "alert_id={}, namespace={}, run_stamp_sec={}",
                    alertId, namespace, runStampSec);
        }

        switch (partId) {
            case (byte) 1:
                headerMessage(alertId, namespace, runStampSec, input);
                break;
            case (byte) 2:
                stateMessage(alertId, namespace, runStampSec, input);
                break;
            case (byte) 3:
                footerMessage(alertId, namespace, runStampSec, input);
                break;
        }
    }

    /**
     * Header message indicates a new state dump.
     *
     * @param alertId
     * @param namespace
     * @param runStampSec
     * @param input
     */
    private void headerMessage(final long alertId,
                               final String namespace,
                               final long runStampSec,
                               final Input input) {
        if (hasIncompleteState(alertId)) {
            // This should not happen, and generally means that the footer
            // for the previous state was not received, hence the state was
            // not moved to the complete state.

            // If out of order message.
            if (incompleteRunStamps.get(alertId) >= runStampSec) {
                // TODO: Maybe implement OutOfOrderException.
                throw new RuntimeException("Out of order message: "
                        + "alert_id=" + alertId + " "
                        + "namespace=" + namespace + " "
                        + "run_stamp_sec(received)=" + runStampSec + " "
                        + "run_stamp_sec(stored)=" + incompleteRunStamps.get(alertId)
                );
            }

            // In order message.
            // TODO: Salvaging whatever information we can here. Strategy
            //       can change.
            LOG.debug("Move incomplete state to complete on header message: " +
                            "alert_id={}, namespace={}, run_stamp_sec={}",
                    alertId, namespace, runStampSec);
            moveToComplete(alertId);
            // Continue creating a new state.
        }

        LOG.debug("Creating a new state: alert_id={}, namespace={}, run_stamp_sec={}",
                alertId, namespace, runStampSec);

        final AlertConfig config = configs.get(alertId);
        final ModifiableAlertStateStore stateStore =
                deserializer.initialize(input, config);

        incompletes.put(alertId, stateStore);
        incompleteRunStamps.put(alertId, runStampSec);
    }

    private void stateMessage(final long alertId,
                              final String namespace,
                              final long runStampSec,
                              final Input input) {
        if (runStampSec <= completeRunStamps.get(alertId)
                || runStampSec < incompleteRunStamps.get(alertId)) {
            LOG.debug("Out of order message: alert_id={} namespace={} " +
                            "run_stamp_sec(received)={} run_stamp_sec(stored)={}",
                    alertId, namespace, runStampSec, completeRunStamps.get(alertId));
            return;
        }

        if (incompleteRunStamps.get(alertId) < runStampSec) {
            if (hasIncompleteState(alertId)) {
                // Salvage remains.
                moveToComplete(alertId);
            }

            // Initialize new state.
            final AlertConfig config = configs.get(alertId);
            // TODO: This should be abstracted.
            final ModifiableAlertStateStore stateStore =
                    new AlertStateStoreImpl(
                            String.valueOf(config.getAlertId()),
                            config.getNagIntervalInSecs(),
                            config.getTransitionConfig(),
                            config.storeIdentity()
                    );
            incompletes.put(alertId, stateStore);
            incompleteRunStamps.put(alertId, runStampSec);
        }

        deserializer.update(input, incompletes.get(alertId));
    }

    private void footerMessage(final long alertId,
                               final String namespace,
                               final long runStampSec,
                               final Input input) {
        final long incompleteRunStampSec = incompleteRunStamps.get(alertId);
        if (incompleteRunStampSec != runStampSec) {
            LOG.warn("Ignoring footer message, mismatched run stamps:" +
                            "alert_id={}, namespace={}, " +
                            "run_stamp_sec(received)={}, run_stamp_sec(stored)={}",
                    alertId, namespace, runStampSec, incompleteRunStampSec);
            return;
        }

        deserializer.finalize(input, incompletes.get(alertId));
        moveToComplete(alertId);
    }

    private boolean hasIncompleteState(final long alertId) {
        return incompleteRunStamps.containsKey(alertId);
    }

    private void moveToComplete(final long alertId) {
        if (!incompleteRunStamps.containsKey(alertId)) {
            // Nothing to move to complete.
            return;
        }

        completes.put(alertId, incompletes.get(alertId));
        completeRunStamps.put(alertId, incompleteRunStamps.get(alertId));

        incompletes.remove(alertId);
        incompleteRunStamps.remove(alertId);


        LOG.debug("Moved to complete: alert_id={}, complete_run_stamp={}",
                alertId, completeRunStamps.get(alertId));
    }

    @Override
    public Optional<BootstrapStateEntry> get(long alertId) {
        rwlock.writeLock().lock();
        try {
            return doGet(alertId);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    /**
     * To be protected by a lock. See {@link #get(long)}.
     *
     * @param alertId alert id
     * @return {@link BootstrapStateEntry} instance
     */
    private Optional<BootstrapStateEntry> doGet(long alertId) {
        if (!completes.containsKey(alertId)
                && !incompletes.containsKey(alertId)) {
            return Optional.empty();
        }

        final long completeStamp = completeRunStamps.containsKey(alertId)
                ? completeRunStamps.get(alertId)
                : BootstrapStateEntry.NO_VALUE;

        final AlertStateStore complete = completes.containsKey(alertId)
                ? completes.get(alertId).copy()
                : BootstrapStateEntry.NO_STATE;

        final long incompleteStamp = incompleteRunStamps.containsKey(alertId)
                ? incompleteRunStamps.get(alertId)
                : BootstrapStateEntry.NO_VALUE;

        final AlertStateStore incomplete = incompletes.containsKey(alertId)
                ? incompletes.get(alertId).copy()
                : BootstrapStateEntry.NO_STATE;

        return Optional.of(
                BootstrapStateEntry.create(
                        alertId,
                        completeStamp,
                        complete,
                        incompleteStamp,
                        incomplete
                )
        );
    }

    /**
     * To be protected by a lock.
     *
     * See {@link #get(long)}, {@link #update(Input, long, String, long)}.
     *
     * @param alertId alert id.
     */
    @Override
    public void remove(long alertId) {
        rwlock.writeLock().lock();
        try {
            doRemove(alertId);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    private void doRemove(long alertId) {
        completes.remove(alertId);
        completeRunStamps.remove(alertId);
        incompletes.remove(alertId);
        incompleteRunStamps.remove(alertId);
    }
}
