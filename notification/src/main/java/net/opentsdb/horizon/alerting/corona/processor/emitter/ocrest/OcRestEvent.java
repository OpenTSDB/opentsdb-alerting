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
import net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.impl.AlertDetailsDeserializer;

import java.util.SortedMap;

public interface OcRestEvent {

    String DEFAULT_CLASS = "HORIZON";

    String DEFAULT_AGENT = "OTSDB";

    String DEFAULT_TYPE = "";

    // ------ Minimum Moog Required Fields ------ //

    /**
     * @return alert hash.
     */
    @JsonProperty("signature")
    String getAlertHash();

    /**
     * @return OpsDB property.
     */
    @JsonProperty("source_id")
    String getOpsDbProperty();

    /**
     * @return Namespace.
     */
    @JsonProperty("external_id")
    String getNamespace();

    /**
     * @return Box sending the alerts.
     */
    @JsonProperty("manager")
    String getHostname();

    /**
     * @return customer hostname from which alert came.
     */
    @JsonProperty("source")
    String getSource();

    /**
     * Default: HORIZON.
     *
     * @return alert class.
     */
    @JsonProperty("class")
    default String getClazz()
    {
        return DEFAULT_CLASS;
    }

    /**
     * Default: OTSDB.
     *
     * @return agent.
     */
    @JsonProperty("agent")
    default String getAgent()
    {
        return DEFAULT_AGENT;
    }

    /**
     * @return namespace.
     */
    @JsonProperty("agent_location")
    default String getAgentLocation()
    {
        return getNamespace();
    }

    /**
     * Default: empty string. Not used on Moog side.
     *
     * @return OpsDB type.
     */
    @JsonProperty("type")
    default String getType()
    {
        return DEFAULT_TYPE;
    }

    /**
     * @return 0 if recovery, 1-5 otherwise.
     */
    @JsonProperty("severity")
    int getSeverity();

    /**
     * @return short alert description, not subject or body.
     */
    @JsonProperty("description")
    String getDescription();

    /**
     * @return alert time in seconds
     */
    @JsonProperty("agent_time")
    long getAlertTimeSec();

    // ------ Shared Required Metadata ------ //

    /**
     * @return runbook id.
     */
    @JsonProperty("runbook_id")
    String getRunbookId();

    /**
     * @return true if recovery.
     */
    @JsonProperty("recovery")
    boolean isRecovery();

    /**
     * @return dashboard url.
     */
    @JsonProperty("dashboard_url")
    String getDashboardUrl();

    /**
     * @return true if nag alert.
     */
    @JsonProperty("is_nag")
    boolean isNag();

    /**
     * @return alert subject.
     */
    @JsonProperty("subject")
    String getSubject();

    /**
     * @return alert body.
     */
    @JsonProperty("body")
    String getBody();

    /**
     * @return alert id.
     */
    @JsonProperty("alert_id")
    long getAlertId();

    /**
     * @return alert tags.
     */
    @JsonProperty("tags")
    SortedMap<String, String> getTags();

    /**
     * @return state from which the alert transitioned.
     */
    @JsonProperty("state_from")
    String getStateFrom();

    /**
     * @return state to which the alert transitioned.
     */
    @JsonProperty("state_to")
    String getStateTo();

    /**
     * @return alert details specific to the alert type.
     */
    @JsonProperty("alert_details")
    AlertDetails getAlertDetails();

    @JsonDeserialize(using = AlertDetailsDeserializer.class)
    interface AlertDetails {

        @JsonProperty("type")
        String getType();

        @JsonProperty("subtype")
        String getSubtype();

        interface Builder
                extends net.opentsdb.horizon.alerting.Builder<Builder, AlertDetails>
        {

        }
    }
}
