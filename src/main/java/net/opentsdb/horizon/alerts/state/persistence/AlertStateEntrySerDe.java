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

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.state.impl.AlertStateEntryImpl;
import net.opentsdb.horizon.alerts.state.AlertStateEntry;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

class AlertStateEntrySerDe {

    private static final byte STATE_ABSENT = (byte) -1;

    private static final short TAGS_ABSENT = (short) -1;

    static void write(final Output output,
                      final AlertStateEntry entry) {
        output.writeLong(entry.getStateId());

        final SortedMap<String, String> tags = entry.getTags();
        if (tags == null) {
            output.writeShort(TAGS_ABSENT);
        } else {
            output.writeShort((short) tags.size());
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                output.writeString(tag.getKey());
                output.writeString(tag.getValue());
            }
        }

        final AlertState current = entry.getCurrentState();
        output.writeByte(current == AlertStateEntry.NO_STATE
                ? STATE_ABSENT
                : current.getId());

        final AlertState previous = entry.getPreviousState();
        output.writeByte(previous == AlertStateEntry.NO_STATE
                ? STATE_ABSENT
                : previous.getId());

        output.writeLong(entry.getLastSeenTimestamp());
        output.writeLong(entry.getNagInterval());
    }

    static AlertStateEntry read(final Input input) {
        final long stateId = input.readLong();

        final short tagsSize = input.readShort();
        final SortedMap<String, String> tags;
        if (tagsSize == TAGS_ABSENT) {
            tags = null;
        } else if (tagsSize == (short) 0) {
            tags = Collections.emptySortedMap();
        } else {
            tags = new TreeMap<>();
            for (short i = 0; i < tagsSize; i++) {
                tags.put(input.readString(), input.readString());
            }
        }

        final byte currentStateId = input.readByte();
        final AlertState currentState = currentStateId == STATE_ABSENT
                ? AlertStateEntry.NO_STATE
                : AlertState.fromId(currentStateId);

        final byte previousStateId = input.readByte();
        final AlertState previousState = previousStateId == STATE_ABSENT
                ? AlertStateEntry.NO_STATE
                : AlertState.fromId(previousStateId);

        final long lastSeen = input.readLong();
        final long nagInterval = input.readLong();

        return new AlertStateEntryImpl(
                stateId,
                tags,
                currentState,
                previousState,
                lastSeen,
                nagInterval
        );
    }
}
