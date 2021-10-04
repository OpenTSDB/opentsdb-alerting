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

import java.util.Objects;

@Getter
@JsonDeserialize(builder = MockPrismEvent.Builder.class)
public class MockPrismEvent {

    @JsonProperty("signature")
    private final String signature;

    @JsonProperty("source")
    private final String source;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("severity")
    private final int severity;

    @JsonProperty("opsdb_property")
    private final String opsdbProperty;

    @JsonProperty("event_time_sec")
    private final long eventTimeSec;

    @JsonProperty("external_id")
    private final String externalId;

    @JsonProperty("agent")
    private final String agent;

    @JsonProperty("agent_location")
    private final String agentLocation;

    @JsonProperty("runbook")
    private final String runbookId;

    @JsonProperty("is_production")
    private final boolean isProduction;

    @JsonProperty("escalation_tier")
    private final int escalationTier;

    @JsonProperty("graph_url")
    private final String graphUrl;

    @JsonProperty("alert")
    private final MockAlertDetails alertDetails;

    private MockPrismEvent(Builder builder) {
        this.signature = builder.signature;
        this.opsdbProperty = builder.opsdbProperty;
        this.externalId = builder.externalId;
        this.source = builder.source;
        this.agent = builder.agent;
        this.agentLocation = builder.agentLocation;
        this.severity = builder.severity;
        this.description = builder.description;
        this.eventTimeSec = builder.eventTimeSec;
        this.runbookId = builder.runbookId;
        this.isProduction = builder.isProduction;
        this.escalationTier = builder.escalationTier;
        this.graphUrl = builder.graphUrl;
        this.alertDetails = builder.alertDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockPrismEvent that = (MockPrismEvent) o;
        return severity == that.severity &&
                eventTimeSec == that.eventTimeSec &&
                isProduction == that.isProduction &&
                escalationTier == that.escalationTier &&
                Objects.equals(signature, that.signature) &&
                Objects.equals(source, that.source) &&
                Objects.equals(description, that.description) &&
                Objects.equals(opsdbProperty, that.opsdbProperty) &&
                Objects.equals(externalId, that.externalId) &&
                Objects.equals(agent, that.agent) &&
                Objects.equals(agentLocation, that.agentLocation) &&
                Objects.equals(runbookId, that.runbookId) &&
                Objects.equals(graphUrl, that.graphUrl) &&
                Objects.equals(alertDetails, that.alertDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                signature, source, description, severity, opsdbProperty,
                eventTimeSec, externalId, agent, agentLocation, runbookId,
                isProduction, escalationTier, graphUrl, alertDetails
        );
    }

    @Override
    public String toString() {
        return "MockPrismEvent{" +
                "signature='" + signature + '\'' +
                ", source='" + source + '\'' +
                ", description='" + description + '\'' +
                ", severity=" + severity +
                ", opsdbProperty='" + opsdbProperty + '\'' +
                ", eventTimeSec=" + eventTimeSec +
                ", externalId='" + externalId + '\'' +
                ", agent='" + agent + '\'' +
                ", agentLocation='" + agentLocation + '\'' +
                ", runbookId='" + runbookId + '\'' +
                ", isProduction=" + isProduction +
                ", escalationTier=" + escalationTier +
                ", graphUrl='" + graphUrl + '\'' +
                ", alertDetails=" + alertDetails +
                '}';
    }

    @JsonPOJOBuilder(withPrefix = "set")
    public static final class Builder {
        private String signature;
        private String opsdbProperty;
        private String externalId;
        private String source;
        private String agent;
        private String agentLocation;
        private int severity;
        private String description;
        private long eventTimeSec;
        private String runbookId;
        private String graphUrl;
        private boolean isProduction;
        private int escalationTier;
        private MockAlertDetails alertDetails;

        private Builder() {
        }

        @JsonProperty("signature")
        public Builder setSignature(String signature) {
            this.signature = signature;
            return this;
        }

        @JsonProperty("opsdb_property")
        public Builder setOpsdbProperty(String opsdbProperty) {
            this.opsdbProperty = opsdbProperty;
            return this;
        }

        @JsonProperty("external_id")
        public Builder setExternalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        @JsonProperty("source")
        public Builder setSource(String source) {
            this.source = source;
            return this;
        }

        @JsonProperty("agent")
        public Builder setAgent(String agent) {
            this.agent = agent;
            return this;
        }

        @JsonProperty("agent_location")
        public Builder setAgentLocation(String agentLocation) {
            this.agentLocation = agentLocation;
            return this;
        }

        @JsonProperty("severity")
        public Builder setSeverity(int severity) {
            this.severity = severity;
            return this;
        }

        @JsonProperty("description")
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        @JsonProperty("event_time_sec")
        public Builder setEventTimeSec(long eventTimeSec) {
            this.eventTimeSec = eventTimeSec;
            return this;
        }

        @JsonProperty("runbook")
        public Builder setRunbookId(String runbookId) {
            this.runbookId = runbookId;
            return this;
        }

        @JsonProperty("graph_url")
        public Builder setGraphUrl(String graphUrl) {
            this.graphUrl = graphUrl;
            return this;
        }

        @JsonProperty("is_production")
        public Builder setIsProduction(boolean isProduction) {
            this.isProduction = isProduction;
            return this;
        }

        @JsonProperty("escalation_tier")
        public Builder setIsProduction(int escalationTier) {
            this.escalationTier = escalationTier;
            return this;
        }

        @JsonProperty("alert")
        public Builder setAlert(MockAlertDetails alertDetails) {
            this.alertDetails = alertDetails;
            return this;
        }

        public MockPrismEvent build() {
            return new MockPrismEvent(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
