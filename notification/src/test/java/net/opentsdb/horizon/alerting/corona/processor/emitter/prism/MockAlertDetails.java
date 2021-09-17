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

package net.opentsdb.horizon.alerting.corona.processor.emitter.prism;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Getter;

import java.util.Map;
import java.util.Objects;

@Getter
@JsonDeserialize(builder = MockAlertDetails.Builder.class)
public class MockAlertDetails {
    @JsonProperty("id")
    private final long alertId;

    @JsonProperty("type")
    private final String type;

    @JsonProperty("type_meta")
    private final Map<String, String> typeMeta;

    @JsonProperty("subject")
    private final String subject;

    @JsonProperty("body")
    private final String body;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("tags")
    private final Map<String, String> tags;

    @JsonProperty("state_from")
    private final String stateFrom;

    @JsonProperty("state_to")
    private final String stateTo;

    @JsonProperty("is_nag")
    private final boolean isNag;

    @JsonProperty("_key")
    private final String key;

    public MockAlertDetails(Builder builder) {
        this.alertId = builder.alertId;
        this.type = builder.type;
        this.typeMeta = builder.typeMeta;
        this.subject = builder.subject;
        this.body = builder.body;
        this.description = builder.description;
        this.tags = builder.tags;
        this.stateFrom = builder.stateFrom;
        this.stateTo = builder.stateTo;
        this.isNag = builder.isNag;
        this.key = builder.key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockAlertDetails that = (MockAlertDetails) o;
        return alertId == that.alertId &&
                isNag == that.isNag &&
                Objects.equals(type, that.type) &&
                Objects.equals(typeMeta, that.typeMeta) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(body, that.body) &&
                Objects.equals(description, that.description) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(stateFrom, that.stateFrom) &&
                Objects.equals(stateTo, that.stateTo) &&
                Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                alertId, type, typeMeta, subject, body, description,
                tags, stateFrom, stateTo, isNag, key
        );
    }

    @Override
    public String toString() {
        return "MockAlertDetails{" +
                "alertId=" + alertId +
                ", type='" + type + '\'' +
                ", typeMeta=" + typeMeta +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                ", stateFrom='" + stateFrom + '\'' +
                ", stateTo='" + stateTo + '\'' +
                ", isNag=" + isNag +
                ", key='" + key + '\'' +
                '}';
    }

    @JsonPOJOBuilder(withPrefix = "set")
    public static class Builder {
        private long alertId;
        private String type;
        private Map<String, String> typeMeta;
        private String subject;
        private String body;
        private String description;
        private Map<String, String> tags;
        private String stateFrom;
        private String stateTo;
        private boolean isNag;
        private String key;

        private Builder() {
        }

        @JsonProperty("id")
        public Builder setId(long alertId) {
            this.alertId = alertId;
            return this;
        }

        @JsonProperty("type")
        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        @JsonProperty("type_meta")
        public Builder setTypeMeta(Map<String, String> typeMeta) {
            this.typeMeta = typeMeta;
            return this;
        }

        @JsonProperty("subject")
        public Builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        @JsonProperty("body")
        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        @JsonProperty("description")
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        @JsonProperty("tags")
        public Builder setTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        @JsonProperty("state_from")
        public Builder setStateFrom(String stateFrom) {
            this.stateFrom = stateFrom;
            return this;
        }

        @JsonProperty("state_to")
        public Builder setStateTo(String stateTo) {
            this.stateTo = stateTo;
            return this;
        }

        @JsonProperty("is_nag")
        public Builder setIsNag(boolean isNag) {
            this.isNag = isNag;
            return this;
        }

        @JsonProperty("_key")
        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public MockAlertDetails build() {
            return new MockAlertDetails(this);
        }
    }
}
