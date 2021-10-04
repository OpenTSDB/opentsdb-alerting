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
import java.util.SortedMap;

public interface WebhookEvent {
    /**
     * @return alert id.
     */
    @JsonProperty("alert_id")
    default long getAlertId() { return 0; };

    /**
     * @return alert body.
     */
    @JsonProperty("body")
    String getBody();

    /**
     * @return short alert description, not subject or body.
     */
    @JsonProperty("description")
    String getDescription();

    /**
     * @return true if nag alert.
     */
    @JsonProperty("is_nag")
    boolean isNag();

    /**
     * @return namespace.
     */
    @JsonProperty("namespace")
    String getNamespace();

    /**
     * @return true if snoozed.
     */
    @JsonProperty("is_snoozed")
    boolean isSnoozed();

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
     * @return alert subject.
     */
    @JsonProperty("subject")
    String getSubject();

    /**
     * @return alert tags.
     */
    @JsonProperty("tags")
    SortedMap<String, String> getTags();

    /**
     * @return alert time in secs.
     */
    @JsonProperty("alert_time_sec")
    long getAlertTimeSec();

    /**
     * @return dashboard url.
     */
    @JsonProperty("url")
    String getUrl();

    /**
     * @return alert type.
     */
    @JsonProperty("type")
    String getType();

    /**
     * @return signature.
     */
    @JsonProperty("signature")
    String getSignature();

    @JsonProperty("is_recovery")
    boolean isRecovery();
}
