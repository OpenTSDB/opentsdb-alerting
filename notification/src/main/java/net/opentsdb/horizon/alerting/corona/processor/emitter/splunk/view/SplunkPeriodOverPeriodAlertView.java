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
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodAlertView;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = SplunkPeriodOverPeriodAlertView.class)
@JsonPropertyOrder({
        "_logged_at",
        "ts", "namespace", "alert_id", "state_from", "state_to", "description",
        "url", "snoozed", "nag", "tags", "contacts", "subject", "body", "alert_type",
        "metric", "observed_value", "predicted_value",
        "lower_bad_threshold", "lower_bad_threshold_value",
        "lower_warn_threshold", "lower_warn_threshold_value",
        "upper_warn_threshold", "upper_warn_threshold_value",
        "upper_bad_threshold", "upper_bad_threshold",
})
public final class SplunkPeriodOverPeriodAlertView extends SplunkAbstractView {

    private final PeriodOverPeriodAlertView innerView;

    public SplunkPeriodOverPeriodAlertView(SplunkViewSharedData sharedData,
                                           PeriodOverPeriodAlertView innerView) {
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

    @JsonProperty("observed_value")
    public double getObservedValue() {
        return innerView.getObservedValue();
    }

    @JsonProperty("predicted_value")
    public double getPredictedValue() {
        return innerView.getPredictedValue();
    }

    @JsonProperty("lower_bad_threshold")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getLowerBadThreshold() {
        if (!innerView.hasLowerBad()) {
            return null;
        }
        return innerView.getLowerBadThreshold() + innerView.getLowerThresholdUnit();
    }

    @JsonProperty("lower_bad_threshold_value")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getLowerBadThresholdValue() {
        if (!innerView.hasLowerBad()) {
            return null;
        }
        return innerView.getLowerBadValue();
    }

    @JsonProperty("lower_warn_threshold")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getLowerWarnThreshold() {
        if (!innerView.hasLowerWarn()) {
            return null;
        }
        return innerView.getLowerWarnThreshold() + innerView.getLowerThresholdUnit();
    }

    @JsonProperty("lower_warn_threshold_value")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getLowerWarnThresholdValue() {
        if (!innerView.hasLowerWarn()) {
            return null;
        }
        return innerView.getLowerWarnValue();
    }

    @JsonProperty("upper_warn_threshold")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getUpperWarnThreshold() {
        if (!innerView.hasUpperWarn()) {
            return null;
        }
        return innerView.getUpperWarnThreshold() + innerView.getUpperThresholdUnit();
    }

    @JsonProperty("upper_warn_threshold_value")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getUpperWarnThresholdValue() {
        if (!innerView.hasUpperWarn()) {
            return null;
        }
        return innerView.getUpperWarnValue();
    }

    @JsonProperty("upper_bad_threshold")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getUpperBadThreshold() {
        if (!innerView.hasUpperBad()) {
            return null;
        }
        return innerView.getUpperBadThreshold() + innerView.getUpperThresholdUnit();
    }

    @JsonProperty("upper_bad_threshold_value")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getUpperBadThresholdValue() {
        if (!innerView.hasUpperBad()) {
            return null;
        }
        return innerView.getUpperBadValue();
    }
}
