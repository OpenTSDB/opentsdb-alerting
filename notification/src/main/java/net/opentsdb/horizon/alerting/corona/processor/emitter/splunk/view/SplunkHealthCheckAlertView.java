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

package net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.view;

import java.util.HashMap;
import java.util.Map;

import net.opentsdb.horizon.alerting.corona.processor.emitter.Interpolator;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthCheckAlertView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = SplunkHealthCheckAlertView.class)
@JsonPropertyOrder({
        "_logged_at",
        "ts", "namespace", "alert_id", "state_from", "state_to", "description",
        "url", "snoozed", "nag", "tags", "contacts", "subject", "body", "alert_type",
        "check_namespace", "check_application", "check_message"
})
public final class SplunkHealthCheckAlertView extends SplunkAbstractView {

    private final HealthCheckAlertView innerView;

    public SplunkHealthCheckAlertView(SplunkViewSharedData sharedData,
                                      HealthCheckAlertView innerView) {
        super(sharedData, innerView);
        this.innerView = innerView;
    }

    @Override
    public String getSubject() {
        Map<String, String> tags = new HashMap<>(innerView.getSortedTags());
        tags.put("status_message", innerView.getStatusMessage());
        return Interpolator.interpolate(sharedData.subject, tags);
    }

    @Override
    public String getBody() {
        Map<String, String> tags = new HashMap<>(innerView.getSortedTags());
        tags.put("status_message", innerView.getStatusMessage());
        return Interpolator.interpolate(sharedData.body, tags);
    }

    @JsonProperty("check_namespace")
    public String getCheckNamespace() {
        return innerView.getDataNamespace();
    }

    @JsonProperty("check_application")
    public String getCheckApplication() {
        return innerView.getApplication();
    }

    @JsonProperty("check_message")
    public String getCheckMessage() {
        return innerView.getStatusMessage();
    }
}
