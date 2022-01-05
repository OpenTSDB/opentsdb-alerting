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

import java.util.Objects;
import java.util.SortedMap;

@Getter
@JsonDeserialize(builder = MockPayload.Builder.class)
public class MockPayload {

    @JsonProperty("summary")
    private final String summary;

    @JsonProperty("source")
    private final String source;

    @JsonProperty("severity")
    private final Severity severity;

    public enum Severity {
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

    @JsonProperty("timestamp")
    private final String timestamp;

    @JsonProperty("custom_details")
    private SortedMap<String, Object> customDetails;

    public MockPayload(Builder builder) {
        this.summary = builder.summary;
        this.source = builder.source;
        this.severity = builder.severity;
        this.timestamp = builder.timestamp;
        this.customDetails = builder.customDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockPayload that = (MockPayload) o;
        return Objects.equals(summary, that.summary) &&
                Objects.equals(source, that.source) &&
                Objects.equals(severity, that.severity) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(customDetails, that.customDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                summary, source, severity, timestamp, customDetails
        );
    }

    @Override
    public String toString() {
        return "MockPayload{" +
                "summary='" + summary + '\'' +
                ", source='" + source + '\'' +
                ", severity='" + severity + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", customDetails=" + customDetails +
                '}';
    }

    @JsonPOJOBuilder(withPrefix = "set")
    public static class Builder {
        private String summary;
        private String source;
        private Severity severity;
        private String timestamp;
        private SortedMap<String, Object> customDetails;

        private Builder() {
        }

        @JsonProperty("summary")
        public Builder setSummary(String summary) {
            this.summary = summary;
            return this;
        }

        @JsonProperty("source")
        public Builder setSource(String source) {
            this.source = source;
            return this;
        }

        @JsonProperty("severity")
        public Builder setSeverity(Severity severity) {
            this.severity = severity;
            return this;
        }

        @JsonProperty("timestamp")
        public Builder setTimestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @JsonProperty("custom_details")
        public Builder setCustomDetails(SortedMap<String, Object> customDetails) {
            this.customDetails = customDetails;
            return this;
        }

        public MockPayload build() {
            return new MockPayload(this);
        }
    }
}
