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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

@Getter
@Setter
public class Event implements KryoSerializable {

    private String namespace;

    private String source;

    private String title;

    private String message;

    private String priority;

    private long timestamp;

    private Map<String, String> tags;


    @Override
    public void write(Kryo kryo, Output output) {
        output.writeString(namespace);
        output.writeString(source);
        output.writeString(title);
        output.writeString(message);
        output.writeString(priority);
        output.writeLong(timestamp);

        if (tags == null || tags.size() == 0) {
            output.writeInt(0);
        } else {
            output.writeInt(tags.size());
            tags.forEach((k, v) -> {
                output.writeString(k);
                output.writeString(v);
            });
        }
    }

    @Override
    public void read(Kryo kryo, Input input) {
        namespace = input.readString();
        source = input.readString();
        title = input.readString();
        message = input.readString();
        priority = input.readString();
        timestamp = input.readLong();


        final int tagsSize = input.readInt();
        if (tagsSize == 0) {
            tags = new HashMap<>();
        } else {
            tags = new HashMap<>(tagsSize);
            for (int i = 0; i < tagsSize; i++) {
                tags.put(input.readString(), input.readString());
            }
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
        Event other = (Event) o;
        return timestamp == other.timestamp &&
                Objects.equals(namespace, other.namespace) &&
                Objects.equals(source, other.source) &&
                Objects.equals(title, other.title) &&
                Objects.equals(message, other.message) &&
                Objects.equals(priority, other.priority) &&
                Objects.equals(tags, other.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                namespace,
                source,
                title,
                message,
                priority,
                timestamp,
                tags
        );
    }

    @Override
    public String toString() {
        return "Event{" +
                "namespace='" + namespace + '\'' +
                ", source='" + source + '\'' +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", priority='" + priority + '\'' +
                ", timestamp=" + timestamp +
                ", tags=" + tags +
                '}';
    }
}
