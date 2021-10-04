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

package net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@JsonDeserialize(builder = MockOcRestEvent.Builder.class)
public class MockOcRestEvent {

    // ------ Minimum Moog Required Fields ------ //

    @JsonProperty("signature")
    private final String signature;

    @JsonProperty("source_id")
    private final String sourceId;

    @JsonProperty("external_id")
    private final String externalId;

    @JsonProperty("manager")
    private final String manager;

    @JsonProperty("source")
    private final String source;

    @JsonProperty("class")
    private final String clazz;

    @JsonProperty("agent")
    private final String agent;

    @JsonProperty("agent_location")
    private final String agentLocation;

    @JsonProperty("type")
    private final String type;

    @JsonProperty("severity")
    private final int severity;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("agent_time")
    private final long agentTimeSec;

    // ------ Shared Required Metadata ------ //

    @JsonProperty("runbook_id")
    private final String runbookId;

    @JsonProperty("recovery")
    private final boolean recovery;

    @JsonProperty("dashboard_url")
    private final String dashboardUrl;

    @JsonProperty("is_nag")
    private final boolean isNag;

    @JsonProperty("subject")
    private final String subject;

    @JsonProperty("body")
    private final String body;

    @JsonProperty("alert_id")
    private final long alertId;

    @JsonProperty("tags")
    private final Map<String, String> tags;

    @JsonProperty("state_from")
    private final String stateFrom;

    @JsonProperty("state_to")
    private final String stateTo;

    @JsonProperty("alert_details")
    private final Map<String, String> alertDetails;

    private MockOcRestEvent(Builder builder)
    {
        this.signature = builder.signature;
        this.sourceId = builder.sourceId;
        this.externalId = builder.externalId;
        this.manager = builder.manager;
        this.source = builder.source;
        this.clazz = builder.clazz;
        this.agent = builder.agent;
        this.agentLocation = builder.agentLocation;
        this.type = builder.type;
        this.severity = builder.severity;
        this.description = builder.description;
        this.agentTimeSec = builder.agentTimeSec;
        this.runbookId = builder.runbookId;
        this.recovery = builder.recovery;
        this.dashboardUrl = builder.dashboardUrl;
        this.isNag = builder.isNag;
        this.subject = builder.subject;
        this.body = builder.body;
        this.alertId = builder.alertId;
        this.tags = builder.tags;
        this.stateFrom = builder.stateFrom;
        this.stateTo = builder.stateTo;
        this.alertDetails = builder.alertDetails;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MockOcRestEvent that = (MockOcRestEvent) o;
        return severity == that.severity &&
                agentTimeSec == that.agentTimeSec &&
                recovery == that.recovery &&
                alertId == that.alertId &&
                isNag == that.isNag &&
                Objects.equals(signature, that.signature) &&
                Objects.equals(sourceId, that.sourceId) &&
                Objects.equals(externalId, that.externalId) &&
                Objects.equals(manager, that.manager) &&
                Objects.equals(source, that.source) &&
                Objects.equals(clazz, that.clazz) &&
                Objects.equals(agent, that.agent) &&
                Objects.equals(agentLocation, that.agentLocation) &&
                Objects.equals(type, that.type) &&
                Objects.equals(description, that.description) &&
                Objects.equals(runbookId, that.runbookId) &&
                Objects.equals(dashboardUrl, that.dashboardUrl) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(body, that.body) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(alertDetails, that.alertDetails);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(
                signature,
                sourceId,
                externalId,
                manager,
                source,
                clazz,
                agent,
                agentLocation,
                type,
                severity,
                description,
                agentTimeSec,
                runbookId,
                recovery,
                dashboardUrl,
                subject,
                body,
                alertId,
                tags,
                isNag,
                alertDetails
        );
    }

    @Override
    public String toString()
    {
        return "MockOcRestEvent{" +
                "signature='" + signature + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", externalId='" + externalId + '\'' +
                ", manager='" + manager + '\'' +
                ", source='" + source + '\'' +
                ", clazz='" + clazz + '\'' +
                ", agent='" + agent + '\'' +
                ", agentLocation='" + agentLocation + '\'' +
                ", type='" + type + '\'' +
                ", severity=" + severity +
                ", description='" + description + '\'' +
                ", agentTimeSec=" + agentTimeSec +
                ", runbookId='" + runbookId + '\'' +
                ", recovery=" + recovery +
                ", dashboardUrl='" + dashboardUrl + '\'' +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", alertId=" + alertId +
                ", tags=" + tags +
                ", isNag=" + isNag +
                ", alertDetails=" + alertDetails +
                '}';
    }

    @JsonPOJOBuilder(withPrefix = "set")
    public static final class Builder {

        private String signature;
        private String sourceId;
        private String externalId;
        private String manager;
        private String source;
        private String clazz;
        private String agent;
        private String agentLocation;
        private String type;
        private int severity;
        private String description;
        private long agentTimeSec;
        private String runbookId;
        private boolean recovery;
        private String dashboardUrl;
        private boolean isNag;
        private String subject;
        private String body;
        private long alertId;
        private String stateFrom;
        private String stateTo;
        private Map<String, String> tags;
        private Map<String, String> alertDetails = new HashMap<>();

        private Builder()
        {
        }

        @JsonProperty("signature")
        public Builder setSignature(String signature)
        {
            this.signature = signature;
            return this;
        }

        @JsonProperty("source_id")
        public Builder setSourceId(String sourceId)
        {
            this.sourceId = sourceId;
            return this;
        }

        @JsonProperty("external_id")
        public Builder setExternalId(String externalId)
        {
            this.externalId = externalId;
            return this;
        }

        @JsonProperty("manager")
        public Builder setManager(String manager)
        {
            this.manager = manager;
            return this;
        }

        @JsonProperty("source")
        public Builder setSource(String source)
        {
            this.source = source;
            return this;
        }

        @JsonProperty("class")
        public Builder setClazz(String clazz)
        {
            this.clazz = clazz;
            return this;
        }

        @JsonProperty("agent")
        public Builder setAgent(String agent)
        {
            this.agent = agent;
            return this;
        }

        @JsonProperty("agent_location")
        public Builder setAgentLocation(String agentLocation)
        {
            this.agentLocation = agentLocation;
            return this;
        }

        @JsonProperty("type")
        public Builder setType(String type)
        {
            this.type = type;
            return this;
        }

        @JsonProperty("severity")
        public Builder setSeverity(int severity)
        {
            this.severity = severity;
            return this;
        }

        @JsonProperty("description")
        public Builder setDescription(String description)
        {
            this.description = description;
            return this;
        }

        @JsonProperty("agent_time")
        public Builder setAgentTimeSec(long agentTimeSec)
        {
            this.agentTimeSec = agentTimeSec;
            return this;
        }

        @JsonProperty("runbook_id")
        public Builder setRunbookId(String runbookId)
        {
            this.runbookId = runbookId;
            return this;
        }

        @JsonProperty("recovery")
        public Builder setRecovery(boolean recovery)
        {
            this.recovery = recovery;
            return this;
        }

        @JsonProperty("dashboard_url")
        public Builder setDashboardUrl(String dashboardUrl)
        {
            this.dashboardUrl = dashboardUrl;
            return this;
        }

        @JsonProperty("is_nag")
        public Builder setIsNag(boolean isNag)
        {
            this.isNag = isNag;
            return this;
        }

        @JsonProperty("subject")
        public Builder setSubject(String subject)
        {
            this.subject = subject;
            return this;
        }

        @JsonProperty("body")
        public Builder setBody(String body)
        {
            this.body = body;
            return this;
        }

        @JsonProperty("alert_id")
        public Builder setAlertId(long alertId)
        {
            this.alertId = alertId;
            return this;
        }

        @JsonProperty("tags")
        public Builder setTags(Map<String, String> tags)
        {
            this.tags = tags;
            return this;
        }

        @JsonProperty("state_from")
        public Builder setStateFrom(String stateFrom)
        {
            this.stateFrom = stateFrom;
            return this;
        }

        @JsonProperty("state_to")
        public Builder setStateTo(String stateTo)
        {
            this.stateTo = stateTo;
            return this;
        }

        @JsonProperty("alert_details")
        public Builder setAlertDetails(Map<String, String> alertDetails)
        {
            this.alertDetails.putAll(alertDetails);
            return this;
        }

        public Builder addAlertDetail(String key, String value)
        {
            this.alertDetails.put(key, value);
            return this;
        }

        public MockOcRestEvent build()
        {
            return new MockOcRestEvent(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
