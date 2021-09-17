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

package net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import net.opentsdb.horizon.alerting.core.validate.Validate;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alert.ThresholdUnit;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.PeriodOverPeriodAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;

public class PeriodOverPeriodAlertView extends AlertView {

    private final PeriodOverPeriodAlert alert;

    public PeriodOverPeriodAlertView(PeriodOverPeriodAlert alert) {
        Validate.paramNotNull(alert, "alert");
        this.alert = alert;
    }

    public String getMetric() {
        return alert.getMetric();
    }

    public double getObservedValue() {
        return alert.getObservedValue();
    }

    public double getPredictedValue() {
        return alert.getPredictedValue();
    }

    public double getLowerWarnValue() {
        return alert.getLowerWarnValue();
    }

    public double getLowerBadValue() {
        return alert.getLowerBadValue();
    }

    public double getUpperWarnValue() {
        return alert.getUpperWarnValue();
    }

    public double getUpperBadValue() {
        return alert.getUpperBadValue();
    }

    public double getLowerBadThreshold() {
        return alert.getLowerBadThreshold();
    }

    public double getLowerWarnThreshold() {
        return alert.getLowerWarnThreshold();
    }

    public double getUpperBadThreshold() {
        return alert.getUpperBadThreshold();
    }

    public double getUpperWarnThreshold() {
        return alert.getUpperWarnThreshold();
    }

    /**
     * @return true if the upper bad threshold is configured.
     */
    public boolean hasUpperBad() {
        return !Double.isNaN(alert.getUpperBadThreshold());
    }

    /**
     * @return true if the upper warn threshold is configured.
     */
    public boolean hasUpperWarn() {
        return !Double.isNaN(alert.getUpperWarnThreshold());
    }

    /**
     * @return true if the lower bad threshold is configured.
     */
    public boolean hasLowerBad() {
        return !Double.isNaN(alert.getLowerBadThreshold());
    }

    /**
     * @return true if the lower warn threshold is configured.
     */
    public boolean hasLowerWarn() {
        return !Double.isNaN(alert.getLowerWarnThreshold());
    }

    /**
     * @return true if either bad or warn upper thresholds are set.
     */
    public boolean hasUpperThresholds() {
        return hasUpperBad() || hasUpperWarn();
    }

    /**
     * @return true if either bad or warn lower thresholds are set.
     */
    public boolean hasLowerThresholds() {
        return hasLowerBad() || hasLowerWarn();
    }

    /**
     * @return true if both upper and lower thresholds are set (any combination
     * of bad and warn).
     */
    public boolean hasUpperAndLowerThresholds() {
        return hasUpperThresholds() && hasLowerThresholds();
    }

    public String getUpperThresholdUnit() {
        return getThresholdUnit(alert.getUpperThresholdUnit());
    }

    public String getLowerThresholdUnit() {
        return getThresholdUnit(alert.getLowerThresholdUnit());
    }

    public PeriodOverPeriodAlert getAlert() {
        return alert;
    }

    private String getThresholdUnit(ThresholdUnit unit) {
        switch (unit) {
            case VALUE:
                return " units";
            case PERCENT:
                return "%";
        }
        return " <unknown unit>";
    }

    @Override
    public String getNamespace() {
        return alert.getNamespace();
    }

    @Override
    public long[] getTimestampsSec() {
        return alert.getTimestampsSec();
    }

    @Override
    public long getTimestampMs() {
        return alert.getTimestampSec() * 1_000L;
    }

    @Override
    public String getStateFrom() {
        return Views.of(alert.getStateFrom());
    }

    @Override
    public String getStateTo() {
        return Views.of(alert.getState());
    }

    @Override
    public boolean isSnoozed() {
        return alert.isSnoozed();
    }

    @Override
    public boolean isNag() {
        return alert.isNag();
    }

    @Override
    public SortedMap<String, String> getSortedTags() {
        final Map<String, String> tags = alert.getTags();
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySortedMap();
        }

        return Collections.unmodifiableSortedMap(new TreeMap<>(tags));
    }

    @Override
    public boolean showGraph() {
        return true;
    }

    @Override
    public boolean isRecovery() {
        return alert.getState() == State.GOOD;
    }

    /**
     * @param emStart emphasis start string
     * @param emStop  emphasis stop string
     * @return alert description.
     */
    @Override
    public String getDescription(final String emStart, final String emStop) {
        if (!hasUpperThresholds() && !hasLowerThresholds()) {
            throw new IllegalStateException("No thresholds set.");
        }

        if (State.GOOD == alert.getState()) {
            return getRecoveryDescription(emStart, emStop);
        }

        switch (alert.getBreachedThresholdType()) {
            case LOWER_WARN: {
                // Observed value 1.0 below the lower warning threshold 2.0 (50% below predicted 4.0)
                final String tmpl = ("Observed value <em>%f</em> below the lower warning threshold <em>%f</em> (%f%s below predicted %f)")
                        .replace("<em>", emStart)
                        .replace("</em>", emStop);
                return String.format(tmpl,
                        alert.getObservedValue(),
                        alert.getLowerWarnValue(),
                        alert.getLowerWarnThreshold(),
                        getLowerThresholdUnit(),
                        alert.getPredictedValue());
            }
            case LOWER_BAD: {
                // Observed value -11.0 below the lower bad threshold 0.0 (100% below predicted 4.0)
                final String tmpl = ("Observed value <em>%f</em> below the lower bad threshold <em>%f</em> (%f%s below predicted %f)")
                        .replace("<em>", emStart)
                        .replace("</em>", emStop);
                return String.format(tmpl,
                        alert.getObservedValue(),
                        alert.getLowerBadValue(),
                        alert.getLowerBadThreshold(),
                        getLowerThresholdUnit(),
                        alert.getPredictedValue());
            }
            case UPPER_WARN: {
                // Observed value 7.0 above the upper warning threshold 6.0 (50% above predicted 4.0)
                final String tmpl = "Observed value <em>%f</em> above the upper warning threshold <em>%f</em> (%f%s above predicted %f)"
                        .replace("<em>", emStart)
                        .replace("</em>", emStop);
                return String.format(tmpl,
                        alert.getObservedValue(),
                        alert.getUpperWarnValue(),
                        alert.getUpperWarnThreshold(),
                        getUpperThresholdUnit(),
                        alert.getPredictedValue());
            }
            case UPPER_BAD: {
                // Observed value 11.0 above the upper bad threshold 8.0 (100% above predicted 4.0)
                final String tmpl = "Observed value <em>%f</em> above the upper bad threshold <em>%f</em> (%f%s above predicted %f)"
                        .replace("<em>", emStart)
                        .replace("</em>", emStop);
                return String.format(tmpl,
                        alert.getObservedValue(),
                        alert.getUpperBadValue(),
                        alert.getUpperBadThreshold(),
                        getUpperThresholdUnit(),
                        alert.getPredictedValue());
            }
            default:
                throw new IllegalArgumentException("Unknown threshold type: " +
                        alert.getBreachedThresholdType());
        }
    }

    private String getRecoveryDescription(final String emStart, final String emStop) {
        if (hasUpperAndLowerThresholds()) {
            final double upperThreshold = hasUpperWarn()
                    ? alert.getUpperWarnThreshold()
                    : alert.getUpperBadThreshold();
            final double lowerThreshold = hasLowerWarn()
                    ? alert.getLowerWarnThreshold()
                    : alert.getUpperBadThreshold();

            final double upperValue = hasUpperWarn()
                    ? alert.getUpperWarnValue()
                    : alert.getUpperBadValue();
            final double lowerValue = hasLowerWarn()
                    ? alert.getLowerWarnValue()
                    : alert.getLowerBadValue();

            // Observed value 10.0 within 5.0 and 20.0 (10% below and 10 units above predicted 9.0)
            String tmpl = "Observed value <em>%f</em> within <em>%f<em> and <em>%f<em> (%f%s below and %f%s above predicted %f)"
                    .replace("<em>", emStart)
                    .replace("</em>", emStop);
            return String.format(tmpl,
                    alert.getObservedValue(),
                    lowerValue,
                    upperValue,
                    lowerThreshold,
                    getLowerThresholdUnit(),
                    upperThreshold,
                    getUpperThresholdUnit(),
                    alert.getPredictedValue());
        } else if (hasUpperThresholds()) {
            final double upperThreshold = hasUpperWarn()
                    ? alert.getUpperWarnThreshold()
                    : alert.getUpperBadThreshold();

            final double upperValue = hasUpperWarn()
                    ? alert.getUpperWarnValue()
                    : alert.getUpperBadValue();

            // Observed value 7.0 below upper threshold 6.0 (50% above predicted 4.0)
            final String tmpl = "Observed value <em>%f</em> below upper threshold <em>%f</em> (%f%s above predicted %f)"
                    .replace("<em>", emStart)
                    .replace("</em>", emStop);
            return String.format(tmpl,
                    alert.getObservedValue(),
                    upperValue,
                    upperThreshold,
                    getUpperThresholdUnit(),
                    alert.getPredictedValue());
        } else if (hasLowerThresholds()) {
            final double lowerThreshold = hasLowerWarn()
                    ? alert.getLowerWarnThreshold()
                    : alert.getUpperBadThreshold();
            final double lowerValue = hasLowerWarn()
                    ? alert.getLowerWarnValue()
                    : alert.getLowerBadValue();

            // Observed value 7.0 above lower threshold 6.0 (50% below predicted 4.0)
            final String tmpl = "Observed value <em>%f</em> above lower threshold <em>%f</em> (%f%s below predicted %f)"
                    .replace("<em>", emStart)
                    .replace("</em>", emStop);
            return String.format(tmpl,
                    alert.getObservedValue(),
                    lowerValue,
                    lowerThreshold,
                    getUpperThresholdUnit(),
                    alert.getPredictedValue());
        }

        throw new IllegalStateException("unreachable");
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        PeriodOverPeriodAlertView that = (PeriodOverPeriodAlertView) o;
        return Objects.equals(alert, that.alert);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), alert);
    }

    @Override
    public String toString() {
        return "PeriodOverPeriodAlertView{" +
                "alert=" + alert +
                '}';
    }
}
