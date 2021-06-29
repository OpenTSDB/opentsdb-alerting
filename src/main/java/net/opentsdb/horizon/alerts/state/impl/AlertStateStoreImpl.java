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

package net.opentsdb.horizon.alerts.state.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.config.TransitionConfig;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.state.AlertStateChange;
import net.opentsdb.horizon.alerts.state.AlertStateEntry;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import net.opentsdb.horizon.alerts.state.ModifiableAlertStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Alert state logic depends on the
 * type of alert but all share a few common things.
 * This is run controlled.
 * Not Thread Safe
 *
 * Can work with any combinations of bad/warn/recovery thresholds
 *
 * Any state implementation is expected to extend this and persist the entities.
 *
 * Typical cycle:
 *
 * for non-missing
 *
 * just call raiseAlert()
 *
 *
 * for missing:
 * call update data point
 * and get iterator for stores data
 *
 * TODO: This class and the interface should be split in two:
 * just a state store and state evaluator.
 */
public class AlertStateStoreImpl implements ModifiableAlertStateStore {

    private static final Logger LOG = LoggerFactory.getLogger(AlertStateStoreImpl.class);

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Entities to state management - needs to be persisted
     */
    private final Long2ByteMap currentStates;

    /**
     * Entities to previous state map - to support nag across transitions
     */
    private final Long2ByteMap previousStates;

    /**
     * To be persisted - but I dunno... dmn.
     */
    private final Long2LongMap nagIntervalMap;

    private final Long2LongMap lastSeenDataPoint;

    /**
     * For the duration of a run - not to be persisted
     */
    private final Long2LongMap alertRaisedInCurrent;

    private final Long2ObjectMap<String[]> identities;

    private volatile boolean storeAlertIdentity;

    private final String storeKey;

    private TransitionConfig alertTransitionConfig;

    private volatile int nagIntervalInSecs = AlertUtils.DO_NOT_NAG;

    /**
     * Example of a store key is an alert id.
     *
     * @param storeKey
     */
    public AlertStateStoreImpl(final String storeKey,
                               final int nagIntervalInSecs,
                               final TransitionConfig alertTransitionConfig,
                               final boolean storeAlertIdentity) {
        this.storeKey = storeKey;
        this.storeAlertIdentity = storeAlertIdentity;
        setNagIntervalInSecs(nagIntervalInSecs);
        setTransitionConfig(alertTransitionConfig);
        this.currentStates = new Long2ByteOpenHashMap();
        this.previousStates = new Long2ByteOpenHashMap();
        this.nagIntervalMap = new Long2LongOpenHashMap();
        this.lastSeenDataPoint = new Long2LongOpenHashMap();
        this.alertRaisedInCurrent = new Long2LongOpenHashMap();
        this.identities = new Long2ObjectArrayMap<>();
    }

    // To be used in {@link #copy()}.
    private AlertStateStoreImpl(final Long2ByteMap currentStates,
                                final Long2ByteMap previousStates,
                                final Long2LongMap nagIntervalMap,
                                final Long2LongMap lastSeenDataPoint,
                                final Long2LongMap alertRaisedInCurrent,
                                final Long2ObjectMap<String[]> identities,
                                final boolean storeAlertIdentity,
                                final String storeKey,
                                final TransitionConfig alertTransitionConfig,
                                final int nagIntervalInSecs) {
        this.currentStates = currentStates;
        this.previousStates = previousStates;
        this.nagIntervalMap = nagIntervalMap;
        this.lastSeenDataPoint = lastSeenDataPoint;
        this.alertRaisedInCurrent = alertRaisedInCurrent;
        this.identities = identities;
        this.storeAlertIdentity = storeAlertIdentity;
        this.storeKey = storeKey;
        this.alertTransitionConfig = alertTransitionConfig;
        this.nagIntervalInSecs = nagIntervalInSecs;
    }

    @Override
    public void newRun() {
        alertRaisedInCurrent.clear();
    }

    @Override
    public boolean getStoreAlertIdentity() {
        return storeAlertIdentity;
    }

    @Override
    public void setStoreAlertIdentity(boolean storeAlertIdentity) {
        this.storeAlertIdentity = storeAlertIdentity;
    }

    @Override
    public void purgeState(long stateId) {
        this.currentStates.remove(stateId);
        this.previousStates.remove(stateId);
        this.nagIntervalMap.remove(stateId);
        this.alertRaisedInCurrent.remove(stateId);
    }

    /**
     * Method assumes that its called in the order of severity.
     * BAD first, then WARN and then RECOVERY.
     * tags for logging
     * Big if I guess...
     *
     * TODO: write a better state machine without a significant object overhead.
     *
     * @param namespace
     * @param alertId
     * @param tags
     * @param newState
     * @return
     */
    @Override
    public AlertStateChange raiseAlert(final String namespace,
                                       final long alertId,
                                       final SortedMap<String, String> tags,
                                       final AlertState newState) {
        final long hashForNAMT = AlertUtils.getHashForNAMT(namespace, alertId, tags);
        LOG.debug(storeKey + " check before contains key: " + hashForNAMT +
                " " + alertRaisedInCurrent.containsKey(hashForNAMT) + " " + newState.name());
        if (!alertRaisedInCurrent.containsKey(hashForNAMT)) {
            LOG.debug(storeKey + " contains key: " + hashForNAMT +
                    " " + alertRaisedInCurrent.containsKey(hashForNAMT) + " " + newState.name());

            alertRaisedInCurrent.put(hashForNAMT, 1l);
            return checkInRun(hashForNAMT, newState, tags);
        }

        return new AlertStateChangeImpl(
                AlertState.fromId(previousStates.get(alertId)),
                newState,
                false,
                false
        );
    }

    private AlertStateChange checkInRun(long stateId, AlertState newState, SortedMap<String, String> tags) {

        AlertState oldState = AlertState.fromId(currentStates.
                computeIfAbsent(stateId, k -> AlertState.GOOD.getId()));

        if (newState != oldState) {
            /*
             * State transition has happened.
             * 1. Reset nag to zero.
             * 2. Update origin and current states.
             * 3. Check if alert needs to be raised.
             * 4. Update nag if alert is to be raised.
             */
            resetNagToZero(stateId);
            currentStates.put(stateId, newState.getId());
            previousStates.put(stateId, oldState.getId());
            LOG.debug("id: {} transition of state for: {} {} {}",
                    storeKey, stateId, oldState, newState);
            if (checkWhetherToRaiseAlert(newState, oldState)) {
                startNag(stateId);
                LOG.info("id: {} transition of state warranting firing of alert: {} {} old: {} new: {}",
                        storeKey, tags.toString(), stateId, oldState, newState);
                return new AlertStateChangeImpl(oldState, newState, false, true);
            }
        } else {

            AlertState originState = AlertState.fromId(previousStates.get(stateId));

            LOG.debug("id: {} no transition of state warranting firing of alert: {} old: {} new: {} orig: {}",
                    storeKey, stateId, oldState, newState, originState);
            /*
             * No state transition has happened.
             * 1. Check if things are good. If yes, do nothing and return false.
             * 2. Check if origin state alert is warranted
             * 2. Check if nag has expired. If yes, raise an alert.
             */
            if (newState == AlertState.GOOD) {
                //Just for cleanliness
                resetNagToZero(stateId);
                return new AlertStateChangeImpl(originState,
                        newState,
                        true, false);
            } else {
                if (checkWhetherToRaiseAlert(newState, originState)) {
                    if (nag(stateId)) {
                        LOG.info("id: {} Time to nag the alert for {} {} state: {} nag_interval: {}",
                                storeKey, tags.toString(), stateId, newState.name(), nagIntervalInSecs);
                        startNag(stateId);
                        return new AlertStateChangeImpl(originState, newState, true, true);
                    }
                }
            }
        }
        return new AlertStateChangeImpl(oldState, newState, false, false);
    }

    private boolean nag(long stateId) {
        if (nagIntervalInSecs == AlertUtils.DO_NOT_NAG) {
            return false;
        }

        long cursec = System.currentTimeMillis() / 1000;
        return (cursec - nagIntervalMap.get(stateId)) > nagIntervalInSecs;
    }

    private void startNag(long stateId) {
        nagIntervalMap.put(stateId, System.currentTimeMillis() / 1000);
    }

    private boolean checkWhetherToRaiseAlert(AlertState newState, AlertState oldState) {

        return alertTransitionConfig.raiseAlert(oldState, newState);
    }

    @Override
    public void setTransitionConfig(TransitionConfig alertTransitionConfig) {
        this.alertTransitionConfig = alertTransitionConfig;
    }

    @Override
    public void setNagIntervalInSecs(int nagIntervalInSecs) {
        this.nagIntervalInSecs = nagIntervalInSecs;
    }

    private void resetNagToZero(long stateId) {
        nagIntervalMap.put(stateId, 0);
    }

    @Override
    public AlertState getCurrentState(long stateId) {
        if (currentStates.containsKey(stateId)) {
            return AlertState.fromId(currentStates.get(stateId));
        } else {
            return null;
        }
    }

    @Override
    public AlertState getPreviousState(long stateId) {
        if (previousStates.containsKey(stateId)) {
            return AlertState.fromId(previousStates.get(stateId));
        } else {
            return null;
        }
    }

    @Override
    public void updateDataPoint(String namespace, long alertId, SortedMap<String, String> tags, long seenTime) {
        final long hashForNAMT = AlertUtils.getHashForNAMT(namespace, alertId, tags);
        
        if (storeAlertIdentity) {
            storeAlertIdentity(hashForNAMT, tags);
        }

        if (lastSeenDataPoint.containsKey(hashForNAMT)) {
            if (lastSeenDataPoint.get(hashForNAMT) >= seenTime) {
                return;
            }
        }

        lastSeenDataPoint.put(hashForNAMT, seenTime);
    }

    private void storeAlertIdentity(final long stateId,
                                    final SortedMap<String, String> tags) {
        if (identities.containsKey(stateId)) {
            return;
        }

        final String[] tagsArr;

        if (tags.isEmpty()) {
            tagsArr = EMPTY_STRING_ARRAY;
        } else {
            tagsArr = new String[tags.size() << 1];
            int i = 0;
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                tagsArr[i << 1] = entry.getKey();
                tagsArr[(i << 1) + 1] = entry.getValue();
                i++;
            }
        }
        identities.put(stateId, tagsArr);
    }

    @Override
    public LongIterator getIteratorForStoredData() {
        return lastSeenDataPoint.keySet().iterator();
    }

    @Override
    public long getLastSeenTime(long stateId) {
        if (lastSeenDataPoint.containsKey(stateId)) {
            return lastSeenDataPoint.get(stateId);
        } else {
            return -1;
        }
    }

    @Override
    public void purgeStateByTime(long timestampSec) {
        final Iterator<Long> iterator = identities.keySet().iterator();

        while (iterator.hasNext()) {
            final long id = iterator.next();
            if (lastSeenDataPoint.get(id) < timestampSec) {
                // TODO: Should be in #purgeState(long)
                iterator.remove();
                lastSeenDataPoint.remove(id);
                purgeState(id);
            }
        }
    }

    @Override
    public SortedMap<String, String> getTags(long stateId) {
        if (!identities.containsKey(stateId)) {
            return null;
        }

        final String[] strings = identities.get(stateId);

        final SortedMap<String, String> tags = new TreeMap<>();
        final int mapSize = strings.length / 2;
        for (int i = 0; i < mapSize; i++) {
            tags.put(strings[i << 1], strings[(i << 1) + 1]);
        }
        return tags;
    }

    private LongSet getAlertHashes() {
        final LongSet alertHashes = new LongOpenHashSet();
        alertHashes.addAll(currentStates.keySet());
        alertHashes.addAll(previousStates.keySet());
        alertHashes.addAll(lastSeenDataPoint.keySet());
        alertHashes.addAll(nagIntervalMap.keySet());
        alertHashes.addAll(identities.keySet());
        return alertHashes;
    }

    private AlertStateEntry getStateEntry(final long stateId) {
        final SortedMap<String, String> maybeTags = getTags(stateId);
        final SortedMap<String, String> tags = maybeTags != null
                ? maybeTags
                : AlertStateEntry.NO_TAGS;

        final AlertState currentState = currentStates.containsKey(stateId)
                ? AlertState.fromId(currentStates.get(stateId))
                : AlertStateEntry.NO_STATE;

        final AlertState previousState = previousStates.containsKey(stateId)
                ? AlertState.fromId(previousStates.get(stateId))
                : AlertStateEntry.NO_STATE;

        final long lastSeenTimestamp = lastSeenDataPoint.containsKey(stateId)
                ? lastSeenDataPoint.get(stateId)
                : AlertStateEntry.NO_VALUE;

        final long nagInterval = nagIntervalMap.containsKey(stateId)
                ? nagIntervalMap.get(stateId)
                : AlertStateEntry.NO_VALUE;

        return new AlertStateEntryImpl(
                stateId,
                tags,
                currentState,
                previousState,
                lastSeenTimestamp,
                nagInterval
        );
    }

    public AlertStateStore copy() {
        return new AlertStateStoreImpl(
                new Long2ByteOpenHashMap(currentStates),
                new Long2ByteOpenHashMap(previousStates),
                new Long2LongOpenHashMap(nagIntervalMap),
                new Long2LongOpenHashMap(lastSeenDataPoint),
                new Long2LongOpenHashMap(alertRaisedInCurrent),
                new Long2ObjectOpenHashMap<String[]>(identities),
                storeAlertIdentity,
                storeKey,
                alertTransitionConfig,
                nagIntervalInSecs
        );
    }

    @Override
    public int size() {
        return getAlertHashes().size();
    }


    @Override
    public Iterator<AlertStateEntry> iterator() {
        return new StateIterator();
    }

    @Override
    public void forEach(Consumer<? super AlertStateEntry> action) {
        final LongIterator it = getAlertHashes().iterator();
        while (it.hasNext()) {
            action.accept(getStateEntry(it.nextLong()));
        }
    }

    /**
     * Put state entry.
     *
     * Take into account: there is no synchronization.
     *
     * @param entry state entry.
     */
    @Override
    public void put(AlertStateEntry entry) {
        final long stateId = entry.getStateId();

        final AlertState currentState = entry.getCurrentState();
        if (currentState != AlertStateEntry.NO_STATE) {
            currentStates.put(stateId, currentState.getId());
        }

        final AlertState previousState = entry.getPreviousState();
        if (previousState != AlertStateEntry.NO_STATE) {
            previousStates.put(stateId, previousState.getId());
        }

        final long lastSeen = entry.getLastSeenTimestamp();
        if (lastSeen != AlertStateEntry.NO_VALUE) {
            lastSeenDataPoint.put(stateId, lastSeen);
        }

        final long nagInterval = entry.getNagInterval();
        if (nagInterval != AlertStateEntry.NO_VALUE) {
            nagIntervalMap.put(stateId, nagInterval);
        }

        final SortedMap<String, String> tags = entry.getTags();
        if (tags != AlertStateEntry.NO_TAGS) {
            storeAlertIdentity(stateId, tags);
        }
    }

    @NotThreadSafe
    public final class StateIterator implements Iterator<AlertStateEntry> {

        private final LongIterator it;
        private long currentLong;

        private StateIterator() {
            this.it = getAlertHashes().iterator();
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public AlertStateEntry next() {
            currentLong = it.nextLong();
            return getStateEntry(currentLong);
        }

        @Override
        public void remove() {
                final long lVal = currentLong;
                it.remove();
                currentStates.remove(lVal);
                previousStates.remove(lVal);
                nagIntervalMap.remove(lVal);
                alertRaisedInCurrent.remove(lVal);
                lastSeenDataPoint.remove(lVal);
                identities.remove(lVal);
        }
    }
}
