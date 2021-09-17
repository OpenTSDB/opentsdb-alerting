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

package net.opentsdb.horizon.alerting.corona.processor.emitter.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Getter;

import java.util.Map;
import java.util.Objects;

@Getter
@JsonDeserialize(builder = MockWebhookEvent.Builder.class)
public class MockWebhookEvent {
    @JsonProperty("alert_id")
    private final long alertId;

    @JsonProperty("body")
    private final String body;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("is_nag")
    private final boolean isNag;

    @JsonProperty("namespace")
    private final String namespace;

    @JsonProperty("is_snoozed")
    private final boolean isSnoozed;

    @JsonProperty("state_from")
    private final String stateFrom;

    @JsonProperty("state_to")
    private final String stateTo;

    @JsonProperty("subject")
    private final String subject;

    @JsonProperty("tags")
    private final Map<String, String> tags;

    @JsonProperty("alert_time_sec")
    private final long alertTime;

    @JsonProperty("url")
    private final String url;

    @JsonProperty("metric")
    private final String metric;

    @JsonProperty("value")
    private final double value;

    @JsonProperty("type")
    private final String type;

    @JsonProperty("signature")
    private final String signature;

    @JsonProperty("is_recovery")
    private final boolean isRecovery;

    private MockWebhookEvent(Builder builder) {
        this.alertId = builder.alertId;
        this.body = builder.body;
        this.description = builder.description;
        this.isNag = builder.isNag;
        this.namespace = builder.namespace;
        this.isSnoozed = builder.isSnoozed;
        this.stateFrom = builder.stateFrom;
        this.stateTo = builder.stateTo;
        this.subject = builder.subject;
        this.tags = builder.tags;
        this.alertTime = builder.alertTime;
        this.url = builder.url;
        this.metric = builder.metric;
        this.value = builder.value;
        this.type = builder.type;
        this.signature = builder.signature;
        this.isRecovery = builder.isRecovery;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockWebhookEvent that = (MockWebhookEvent) o;
        return alertId == that.alertId &&
            Objects.equals(body, that.body) &&
            Objects.equals(description, that.description) &&
            Objects.equals(isNag, that.isNag) &&
            Objects.equals(namespace, that.namespace) &&
            Objects.equals(isSnoozed, that.isSnoozed) &&
            Objects.equals(stateFrom, that.stateFrom) &&
            Objects.equals(stateTo, that.stateTo) &&
            Objects.equals(subject, that.subject) &&
            Objects.equals(tags, that.tags) &&
            Objects.equals(alertTime, that.alertTime) &&
            Objects.equals(url, that.url) &&
            Objects.equals(metric, that.metric) &&
            Objects.equals(value, that.value) &&
            Objects.equals(type, that.type) &&
            Objects.equals(signature, that.signature) &&
            Objects.equals(isRecovery, that.isRecovery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                alertId, body, description, isNag, namespace, isSnoozed,
                stateFrom, stateTo, subject, tags, alertTime, url, metric, value,
                type, signature, isRecovery
        );
    }

    @Override
    public String toString() {
        return "MockWebhookEvent{" +
                "alertId='" + alertId + '\'' +
                ", body='" + body + '\'' +
                ", description='" + description + '\'' +
                ", isNag='" + isNag + '\'' +
                ", namespace=" + namespace +
                ", isSnoozed='" + isSnoozed + '\'' +
                ", stateFrom='" + stateFrom + '\'' +
                ", stateTo='" + stateTo + '\'' +
                ", subject='" + subject + '\'' +
                ", tags=" + tags +
                ", alertTime=" + alertTime +
                ", url=" + url +
                ", metric=" + metric +
                ", value=" + value +
                ", type=" + type +
                ", signature=" + signature +
                ", isRecovery=" + isRecovery +
                '}';
    }

    @JsonPOJOBuilder(withPrefix = "set")
    public static final class Builder {
        private long alertId;
        private String body;
        private String description;
        private boolean isNag;
        private String namespace;
        private boolean isSnoozed;
        private String stateFrom;
        private String stateTo;
        private String subject;
        private Map<String, String> tags;
        private long alertTime;
        private String url;
        private String metric;
        private double value;
        private String type;
        private String signature;
        private Boolean isRecovery;

        private Builder() {
        }

        @JsonProperty("alert_id")
        public Builder setAlertId(long alertId) {
            this.alertId = alertId;
            return this;
        }

        @JsonProperty("body")
        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        @JsonProperty("description")
        public Builder setDescription(String description)
        {
            this.description = description;
            return this;
        }

        @JsonProperty("is_nag")
        public Builder setIsNag(boolean isNag)
        {
            this.isNag = isNag;
            return this;
        }

        @JsonProperty("namespace")
        public Builder setNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        @JsonProperty("is_snoozed")
        public Builder setIsSnoozed(boolean isSnoozed)
        {
            this.isSnoozed = isSnoozed;
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

        @JsonProperty("subject")
        public Builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        @JsonProperty("tags")
        public Builder setTags(Map<String, String> tags)
        {
            this.tags = tags;
            return this;
        }

        @JsonProperty("alert_time_sec")
        public Builder setAlertTime(long alertTime) {
            this.alertTime = alertTime;
            return this;
        }

        @JsonProperty("url")
        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        @JsonProperty("metric")
        public Builder setMetric(String metric) {
            this.metric = metric;
            return this;
        }

        @JsonProperty("value")
        public Builder setValue(double value) {
            this.value = value;
            return this;
        }

        @JsonProperty("type")
        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        @JsonProperty("signature")
        public Builder setSignature(String signature) {
            this.signature = signature;
            return this;
        }

        @JsonProperty("is_recovery")
        public Builder setIsRecovery(boolean isRecovery) {
            this.isRecovery = isRecovery;
            return this;
        }

        public MockWebhookEvent build() {
            return new MockWebhookEvent(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
