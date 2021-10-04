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

import net.opentsdb.horizon.alerting.corona.processor.emitter.Interpolator;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricAlertView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = SplunkSingleMetricAlertView.class)
@JsonPropertyOrder({
        "_logged_at",
        "ts", "namespace", "alert_id", "state_from", "state_to", "description",
        "url", "snoozed", "nag", "tags", "contacts", "subject", "body", "alert_type",
        "metric", "value"
})
public final class SplunkSingleMetricAlertView extends SplunkAbstractView {

    private final SingleMetricAlertView innerView;

    public SplunkSingleMetricAlertView(SplunkViewSharedData sharedData,
                                       SingleMetricAlertView innerView) {
        super(sharedData, innerView);
        this.innerView = innerView;
    }

    @Override
    String getSubject() {
        return Interpolator.interpolate(sharedData.subject, innerView.getSortedTags());
    }

    @Override
    String getBody() {
        return Interpolator.interpolate(sharedData.body, innerView.getSortedTags());
    }

    @JsonProperty("metric")
    public String getMetric() {
        return innerView.getMetric();
    }

    @JsonProperty("value")
    public double getValue() {
        return innerView.getMetricValue();
    }
}
