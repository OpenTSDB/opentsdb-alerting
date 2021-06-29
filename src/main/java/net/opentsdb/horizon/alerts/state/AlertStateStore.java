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

package net.opentsdb.horizon.alerts.state;

import java.util.SortedMap;

import net.opentsdb.horizon.alerts.config.TransitionConfig;
import net.opentsdb.horizon.alerts.enums.AlertState;

import it.unimi.dsi.fastutil.longs.LongIterator;

public interface AlertStateStore extends Iterable<AlertStateEntry> {

    /**
     * Set alert transition config.
     *
     * @param config alert transition config.
     */
    void setTransitionConfig(TransitionConfig config);

    /**
     * Set nag interval in seconds.
     *
     * @param nagIntervalInSecs nag interval in seconds.
     */
    void setNagIntervalInSecs(int nagIntervalInSecs);

    /**
     * This method is called before every evaluation round.
     */
    void newRun();

    AlertStateChange raiseAlert(String namespace,
                                long alertId,
                                SortedMap<String, String> tags,
                                AlertState newState);

    void updateDataPoint(String namespace,
                         long alertId,
                         SortedMap<String, String> tags,
                         long seenTime);

    /**
     * Get current alert state.
     *
     * @param stateId state id (alert hash)
     * @return current alert state.
     */
    AlertState getCurrentState(long stateId);

    /**
     * Get previous alert state.
     *
     * @param stateId state id (alert hash)
     * @return previous alert state.
     */
    AlertState getPreviousState(long stateId);

    /**
     * Get timestamp of when the data was last seen.
     *
     * @param stateId state id (alert hash)
     * @return timestamp in seconds.
     */
    long getLastSeenTime(long stateId);

    /**
     * Get identity tags associated with the given state id.
     *
     * @param stateId state id (alert hash)
     * @return identity tags, null if {@link #getStoreAlertIdentity()} is false.
     */
    SortedMap<String, String> getTags(long stateId);

    /**
     * Get a flag indicating if alert identity information is stored when
     * {@link #updateDataPoint(String, long, SortedMap, long)} is called.
     *
     * @return true if identity information is stored, false otherwise.
     */
    boolean getStoreAlertIdentity();

    /**
     * Set the flag if alert identity information is stored when
     * {@link #updateDataPoint(String, long, SortedMap, long)} is called.
     * @param storeAlertIdentity
     */
    void setStoreAlertIdentity(boolean storeAlertIdentity);

    /**
     * Get iterator for state ids for which identity information was stored.
     *
     * The iterator iterates only over ids for which state was added
     * by the {@link #updateDataPoint(String, long, SortedMap, long)}
     * call.
     *
     * Note: A call to {@link #getTags(long)} may not return tags if
     * {@link #getStoreAlertIdentity()} is false.
     *
     * @return state id iterator.
     */
    LongIterator getIteratorForStoredData();

    /**
     * Purge all state for the given state id.
     *
     * @param stateId state id (alert hash).
     */
    void purgeState(long stateId);

    /**
     * Purge all state with last update time < given timestamp.
     *
     * @param timestampSec timestamp in seconds.
     */
    void purgeStateByTime(long timestampSec);

    /**
     * Number of stored states.
     *
     * @return number of stored states.
     */
    int size();
}
