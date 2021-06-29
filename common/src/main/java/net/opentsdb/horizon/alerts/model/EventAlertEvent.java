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

package net.opentsdb.horizon.alerts.model;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

@Getter
@Setter
public class EventAlertEvent extends AlertEvent {

    private String dataNamespace;

    private String filterQuery;

    private int threshold;

    private int windowSizeSec;

    private int count;

    private Event event;

    @Override
    public void write(Kryo kryo, Output output) {
        super.write(kryo, output);
        output.writeString(dataNamespace);
        output.writeString(filterQuery);
        output.writeInt(threshold);
        output.writeInt(windowSizeSec);
        output.writeInt(count);

        output.writeBoolean(event != null);
        if (event != null) {
            event.write(kryo, output);
        }
    }

    @Override
    public void read(Kryo kryo, Input input) {
        super.read(kryo, input);
        dataNamespace = input.readString();
        filterQuery = input.readString();
        threshold = input.readInt();
        windowSizeSec = input.readInt();
        count = input.readInt();

        if (input.readBoolean()) {
            event = new Event();
            event.read(kryo, input);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventAlertEvent that = (EventAlertEvent) o;
        return threshold == that.threshold &&
                windowSizeSec == that.windowSizeSec &&
                count == that.count &&
                Objects.equals(dataNamespace, that.dataNamespace) &&
                Objects.equals(filterQuery, that.filterQuery) &&
                Objects.equals(event, that.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                dataNamespace,
                filterQuery,
                threshold,
                windowSizeSec,
                count,
                event
        );
    }

    @Override
    public String toString() {
        return "EventAlertEvent{" +
                "super=" + super.toString() +
                ", dataNamespace='" + dataNamespace + '\'' +
                ", filterQuery='" + filterQuery + '\'' +
                ", threshold=" + threshold +
                ", windowSizeSec=" + windowSizeSec +
                ", count=" + count +
                ", event=" + event +
                '}';
    }
}
