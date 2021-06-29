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

import java.util.Objects;
import java.util.SortedMap;

import lombok.Getter;

import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.state.AlertStateEntry;

@Getter
public class AlertStateEntryImpl implements AlertStateEntry {

    private final long stateId;

    private final SortedMap<String, String> tags;

    private final AlertState currentState;

    private final AlertState previousState;

    private final long lastSeenTimestamp;

    private final long nagInterval;

    public AlertStateEntryImpl(final long stateId,
                               final SortedMap<String, String> tags,
                               final AlertState currentState,
                               final AlertState previousState,
                               final long lastSeenTimestamp,
                               final long nagInterval) {
        this.stateId = stateId;
        this.tags = tags;
        this.currentState = currentState;
        this.previousState = previousState;
        this.lastSeenTimestamp = lastSeenTimestamp;
        this.nagInterval = nagInterval;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AlertStateEntryImpl that = (AlertStateEntryImpl) o;
        return stateId == that.stateId &&
                lastSeenTimestamp == that.lastSeenTimestamp &&
                nagInterval == that.nagInterval &&
                Objects.equals(tags, that.tags) &&
                currentState == that.currentState &&
                previousState == that.previousState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                stateId,
                tags,
                currentState,
                previousState,
                lastSeenTimestamp,
                nagInterval
        );
    }

    @Override
    public String toString() {
        return "AlertStateEntryImpl{" +
                "stateId=" + stateId +
                ", tags=" + tags +
                ", currentState=" + currentState +
                ", previousState=" + previousState +
                ", lastSeenTimestamp=" + lastSeenTimestamp +
                ", nagInterval=" + nagInterval +
                '}';
    }
}
