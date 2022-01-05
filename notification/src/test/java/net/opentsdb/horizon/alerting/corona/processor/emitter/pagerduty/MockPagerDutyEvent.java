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

package net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@JsonDeserialize(builder = MockPagerDutyEvent.Builder.class)
public class MockPagerDutyEvent {
    @JsonProperty("routing_key")
    private final String routingKey;

    @JsonProperty("event_action")
    private final EventAction eventAction;

    @JsonProperty("dedup_key")
    private final String dedupKey;

    @JsonProperty("links")
    private final List<Map<String, String>> links;

    @JsonProperty("payload")
    private final MockPayload payload;

    public enum EventAction {
        TRIGGER("trigger"),
        ACKNOWLEDGE("acknowledge"),
        RESOLVE("resolve");

        private final String value;

        EventAction(String value) {
            this.value = value;
        }

        @JsonCreator
        public static EventAction fromValue(String text) {
            for (EventAction e : EventAction.values()) {
                if (String.valueOf(e.value).equals(text)) {
                    return e;
                }
            }
            return null;
        }

        @JsonValue
        public String getValue() {
            return this.value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }


    private MockPagerDutyEvent(Builder builder) {
        this.routingKey = builder.routingKey;
        this.eventAction = builder.eventAction;
        this.dedupKey = builder.dedupKey;
        this.links = builder.links;
        this.payload = builder.payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockPagerDutyEvent that = (MockPagerDutyEvent) o;
        return Objects.equals(routingKey, that.routingKey) &&
                Objects.equals(eventAction, that.eventAction) &&
                Objects.equals(dedupKey, that.dedupKey) &&
                Objects.equals(links, that.links) &&
                Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                routingKey, eventAction, dedupKey, links, payload
        );
    }

    @Override
    public String toString() {
        return "MockPagerDutyEvent{" +
                "routingKey='" + routingKey + '\'' +
                ", eventAction='" + eventAction + '\'' +
                ", dedupKey='" + dedupKey + '\'' +
                ", links=" + links +
                ", payload=" + payload +
                "}";
    }

    @JsonPOJOBuilder(withPrefix = "set")
    public static final class Builder {
        private String routingKey;
        private EventAction eventAction;
        private String dedupKey;
        private List<Map<String, String>> links;
        private MockPayload payload;

        private Builder() {
        }

        @JsonProperty("routing_key")
        public Builder setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
            return this;
        }

        @JsonProperty("event_action")
        public Builder setEventAction(EventAction eventAction) {
            this.eventAction = eventAction;
            return this;
        }

        @JsonProperty("dedup_key")
        public Builder setDedupKey(String dedupKey) {
            this.dedupKey = dedupKey;
            return this;
        }

        @JsonProperty("links")
        public Builder setLinks(List<Map<String, String>> links) {
            this.links = links;
            return this;
        }

        @JsonProperty("payload")
        public Builder setPayload(MockPayload payload) {
            this.payload = payload;
            return this;
        }

        public MockPagerDutyEvent build() {
            return new MockPagerDutyEvent(this);
        }
    }

    public static Builder builder() { return new Builder(); }
}
