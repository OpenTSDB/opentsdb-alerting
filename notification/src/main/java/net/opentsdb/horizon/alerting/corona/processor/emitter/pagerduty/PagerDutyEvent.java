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

import java.util.List;
import java.util.Map;

public interface PagerDutyEvent {
    @JsonProperty("routing_key")
    String getRoutingKey();

    @JsonProperty("event_action")
    EventAction getEventAction();

    @JsonProperty("dedup_key")
    String getDedupKey();

    @JsonProperty("links")
    List<Map<String, String>> getLinks();

    @JsonProperty("payload")
    Payload getPayload();

    enum EventAction {
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

    interface Payload {
        @JsonProperty("summary")
        String getSummary();

        @JsonProperty("source")
        String getSource();

        @JsonProperty("severity")
        Severity getSeverity();

        @JsonProperty("timestamp")
        String getTimestamp();

        @JsonProperty("custom_details")
        Map<String, Object> getCustomDetails();

        enum Severity {
            CRITICAL("critical"),
            ERROR("error"),
            WARNING("warning"),
            INFO("info");

            private String value;

            Severity(final String value) {
                this.value = value;
            }

            @JsonCreator
            public static Severity fromValue(String text) {
                for (Severity e : Severity.values()) {
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
    }
}
