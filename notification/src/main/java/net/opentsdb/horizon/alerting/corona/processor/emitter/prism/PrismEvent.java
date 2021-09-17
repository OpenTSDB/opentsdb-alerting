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

import java.util.Map;
import java.util.SortedMap;

import net.opentsdb.horizon.alerting.corona.processor.emitter.prism.impl.AlertDetailsDeserializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public interface PrismEvent {

    /**
     * @return alert hash.
     */
    @JsonProperty("signature")
    String getSignature();

    /**
     * @return customer hostname from which alert came.
     */
    @JsonProperty("source")
    String getSource();

    /**
     * @return we use subject to fill in this description.
     */
    @JsonProperty("description")
    String getDescription();

    /**
     * @return 0 if recovery, 1-5 otherwise.
     */
    @JsonProperty("severity")
    int getSeverity();

    /**
     * @return OpsDB property.
     */
    @JsonProperty("opsdb_property")
    String getOpsDbProperty();

    /**
     * @return alert time in seconds
     */
    @JsonProperty("event_time_sec")
    long getAlertTimeSec();


    /**
     * @return Namespace.
     */
    @JsonProperty("external_id")
    String getNamespace();

    /**
     * Default: OTSDB.
     *
     * @return agent.
     */
    @JsonProperty("agent")
    String getAgent();

    /**
     * @return Namespace.
     */
    @JsonProperty("agent_location")
    String getAgentLocation();

    /**
     * @return runbook id.
     */
    @JsonProperty("runbook")
    String getRunbookId();

    /**
     * @return Is this a production alert?
     */
    @JsonProperty("is_production")
    boolean isProduction();

    /**
     * @return Is this a production alert?
     */
    @JsonProperty("escalation_tier")
    int getEscalationTier();

    /**
     * @return dashboard url.
     */
    @JsonProperty("graph_url")
    String getDashboardUrl();

    /**
     * @return alert details specific to the alert type.
     */
    @JsonProperty("alert")
    AlertDetails getAlertDetails();

    @JsonDeserialize(using = AlertDetailsDeserializer.class)
    interface AlertDetails {

        /**
         * @return alert id.
         */
        @JsonProperty("id")
        long getAlertId();

        /**
         * @return alert type, single metric alert, event alert, period over
         * period alert, etc.
         */
        @JsonProperty("type")
        String getType();

        /**
         * @return additional metadata specific to the type.
         */
        @JsonProperty("type_meta")
        Map<String, String> getTypeMeta();


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
         * @return Generated alert description.
         */
        @JsonProperty("description")
        String getDescription();

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
         * @return true if nag alert.
         */
        @JsonProperty("is_nag")
        boolean isNag();

        /**
         * @return SingleMetricAlert: `full_metric_name`
         * EventAlert: `data_namespace`
         * HealthCheckAlert: `data_namespace.application`
         * PeriodOverPeriodAlert: `full_metric_name`.
         */
        @JsonProperty("_key")
        String getKey();

        interface Builder extends net.opentsdb.horizon.alerting.Builder<Builder, AlertDetails> {

        }
    }
}
