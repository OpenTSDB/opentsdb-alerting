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

package net.opentsdb.horizon.alerting.corona.model.alert.impl;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.Strings;
import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alert.ThresholdType;
import net.opentsdb.horizon.alerting.corona.model.alert.ThresholdUnit;

@Getter
public class PeriodOverPeriodAlert extends Alert {

    private static final String METRIC_UNKNOWN = "<unknown>";

    private final String metric;
    private final double observedValue;
    private final double predictedValue;
    private final double upperWarnValue;
    private final double upperBadValue;
    private final double lowerWarnValue;
    private final double lowerBadValue;
    private final ThresholdType breachedThresholdType;

    // Config settings.

    private final double upperWarnThreshold;
    private final double upperBadThreshold;
    private final ThresholdUnit upperThresholdUnit;
    private final double lowerWarnThreshold;
    private final double lowerBadThreshold;
    private final ThresholdUnit lowerThresholdUnit;

    // Data for visualization.

    private final long[] timestampsSec;
    private final double[] observedValues;
    private final double[] predictedValues;
    private final double[] upperWarnValues;
    private final double[] upperBadValues;
    private final double[] lowerWarnValues;
    private final double[] lowerBadValues;

    private PeriodOverPeriodAlert(Builder<?> builder) {
        super(builder);
        this.metric = Strings.isNullOrEmpty(builder.metric)
                ? METRIC_UNKNOWN
                : builder.metric;
        this.observedValue = builder.observedValue;
        this.predictedValue = builder.predictedValue;
        this.upperWarnValue = builder.upperWarnValue;
        this.upperBadValue = builder.upperBadValue;
        this.lowerWarnValue = builder.lowerWarnValue;
        this.lowerBadValue = builder.lowerBadValue;
        this.breachedThresholdType = builder.breachedThresholdType == null
                ? ThresholdType.UNKNOWN
                : builder.breachedThresholdType;

        this.upperWarnThreshold = builder.upperWarnThreshold;
        this.upperBadThreshold = builder.upperBadThreshold;
        this.upperThresholdUnit = builder.upperThresholdUnit == null
                ? ThresholdUnit.UNKNOWN
                : builder.upperThresholdUnit;
        this.lowerWarnThreshold = builder.lowerWarnThreshold;
        this.lowerBadThreshold = builder.lowerBadThreshold;
        this.lowerThresholdUnit = builder.lowerThresholdUnit == null
                ? ThresholdUnit.UNKNOWN
                : builder.lowerThresholdUnit;

        this.timestampsSec = builder.timestampsSec;
        this.observedValues = builder.observedValues;
        this.predictedValues = builder.predictedValues;
        this.upperWarnValues = builder.upperWarnValues;
        this.upperBadValues = builder.upperBadValues;
        this.lowerWarnValues = builder.lowerWarnValues;
        this.lowerBadValues = builder.lowerBadValues;
    }

    public Optional<double[]> getUpperWarnValues() {
        return Optional.ofNullable(upperWarnValues);
    }

    public Optional<double[]> getUpperBadValues() {
        return Optional.ofNullable(upperBadValues);
    }

    public Optional<double[]> getLowerWarnValues() {
        return Optional.ofNullable(lowerWarnValues);
    }

    public Optional<double[]> getLowerBadValues() {
        return Optional.ofNullable(lowerBadValues);
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        PeriodOverPeriodAlert alert = (PeriodOverPeriodAlert) o;
        return Double.compare(alert.observedValue, observedValue) == 0 &&
                Double.compare(alert.predictedValue, predictedValue) == 0 &&
                Double.compare(alert.upperWarnValue, upperWarnValue) == 0 &&
                Double.compare(alert.upperBadValue, upperBadValue) == 0 &&
                Double.compare(alert.lowerWarnValue, lowerWarnValue) == 0 &&
                Double.compare(alert.lowerBadValue, lowerBadValue) == 0 &&
                Double.compare(alert.upperWarnThreshold, upperWarnThreshold) == 0 &&
                Double.compare(alert.upperBadThreshold, upperBadThreshold) == 0 &&
                Double.compare(alert.lowerWarnThreshold, lowerWarnThreshold) == 0 &&
                Double.compare(alert.lowerBadThreshold, lowerBadThreshold) == 0 &&
                Objects.equals(metric, alert.metric) &&
                breachedThresholdType == alert.breachedThresholdType &&
                upperThresholdUnit == alert.upperThresholdUnit &&
                lowerThresholdUnit == alert.lowerThresholdUnit &&
                Arrays.equals(timestampsSec, alert.timestampsSec) &&
                Arrays.equals(observedValues, alert.observedValues) &&
                Arrays.equals(predictedValues, alert.predictedValues) &&
                Arrays.equals(upperWarnValues, alert.upperWarnValues) &&
                Arrays.equals(upperBadValues, alert.upperBadValues) &&
                Arrays.equals(lowerWarnValues, alert.lowerWarnValues) &&
                Arrays.equals(lowerBadValues, alert.lowerBadValues);
    }

    @Override
    public String toString() {
        return "PeriodOverPeriodAlert{" +
                super.toString() +
                ", metric='" + metric + '\'' +
                ", observedValue=" + observedValue +
                ", predictedValue=" + predictedValue +
                ", upperWarnValue=" + upperWarnValue +
                ", upperBadValue=" + upperBadValue +
                ", lowerWarnValue=" + lowerWarnValue +
                ", lowerBadValue=" + lowerBadValue +
                ", breachedThresholdType=" + breachedThresholdType +
                ", upperWarnThreshold=" + upperWarnThreshold +
                ", upperBadThreshold=" + upperBadThreshold +
                ", upperThresholdUnit=" + upperThresholdUnit +
                ", lowerWarnThreshold=" + lowerWarnThreshold +
                ", lowerBadThreshold=" + lowerBadThreshold +
                ", lowerThresholdUnit=" + lowerThresholdUnit +
                ", timestampsSec=" + Arrays.toString(timestampsSec) +
                ", observedValues=" + Arrays.toString(observedValues) +
                ", predictedValues=" + Arrays.toString(predictedValues) +
                ", upperWarnValues=" + Arrays.toString(upperWarnValues) +
                ", upperBadValues=" + Arrays.toString(upperBadValues) +
                ", lowerWarnValues=" + Arrays.toString(lowerWarnValues) +
                ", lowerBadValues=" + Arrays.toString(lowerBadValues) +
                '}';
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
                super.hashCode(),
                metric,
                observedValue,
                predictedValue,
                upperWarnValue,
                upperBadValue,
                lowerWarnValue,
                lowerBadValue,
                breachedThresholdType,
                upperWarnThreshold,
                upperBadThreshold,
                upperThresholdUnit,
                lowerWarnThreshold,
                lowerBadThreshold,
                lowerThresholdUnit
        );
        result = 31 * result + Arrays.hashCode(timestampsSec);
        result = 31 * result + Arrays.hashCode(observedValues);
        result = 31 * result + Arrays.hashCode(predictedValues);
        result = 31 * result + Arrays.hashCode(upperWarnValues);
        result = 31 * result + Arrays.hashCode(upperBadValues);
        result = 31 * result + Arrays.hashCode(lowerWarnValues);
        result = 31 * result + Arrays.hashCode(lowerBadValues);
        return result;
    }

    /* ------------ Builders ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends Alert.Builder<PeriodOverPeriodAlert, B> {

        private String metric;
        private double observedValue = Double.NaN;
        private double predictedValue = Double.NaN;
        private double upperWarnValue = Double.NaN;
        private double upperBadValue = Double.NaN;
        private double lowerWarnValue = Double.NaN;
        private double lowerBadValue = Double.NaN;
        private ThresholdType breachedThresholdType;

        // Config settings.

        private double upperWarnThreshold = Double.NaN;
        private double upperBadThreshold = Double.NaN;
        private ThresholdUnit upperThresholdUnit;

        private double lowerWarnThreshold = Double.NaN;
        private double lowerBadThreshold = Double.NaN;
        private ThresholdUnit lowerThresholdUnit;

        // Data for visualization.

        private long[] timestampsSec;
        private double[] observedValues;
        private double[] predictedValues;
        private double[] upperWarnValues;
        private double[] upperBadValues;
        private double[] lowerWarnValues;
        private double[] lowerBadValues;

        private Builder() {
            super(AlertType.PERIOD_OVER_PERIOD);
        }

        public B setMetric(String metric) {
            this.metric = metric;
            return self();
        }

        public B setObservedValue(double observedValue) {
            this.observedValue = observedValue;
            return self();
        }

        public B setPredictedValue(double predictedValue) {
            this.predictedValue = predictedValue;
            return self();
        }

        public B setUpperWarnValue(double upperWarnValue) {
            this.upperWarnValue = upperWarnValue;
            return self();
        }

        public B setUpperBadValue(double upperBadValue) {
            this.upperBadValue = upperBadValue;
            return self();
        }

        public B setLowerWarnValue(double lowerWarnValue) {
            this.lowerWarnValue = lowerWarnValue;
            return self();
        }

        public B setLowerBadValue(double lowerBadValue) {
            this.lowerBadValue = lowerBadValue;
            return self();
        }

        public B setBreachedThresholdType(ThresholdType breachedThresholdType) {
            this.breachedThresholdType = breachedThresholdType;
            return self();
        }

        public B setUpperWarnThreshold(double upperWarnThreshold) {
            this.upperWarnThreshold = upperWarnThreshold;
            return self();
        }

        public B setUpperBadThreshold(double upperBadThreshold) {
            this.upperBadThreshold = upperBadThreshold;
            return self();
        }

        public B setUpperThresholdUnit(ThresholdUnit upperThresholdUnit) {
            this.upperThresholdUnit = upperThresholdUnit;
            return self();
        }

        public B setLowerWarnThreshold(double lowerWarnThreshold) {
            this.lowerWarnThreshold = lowerWarnThreshold;
            return self();
        }

        public B setLowerBadThreshold(double lowerBadThreshold) {
            this.lowerBadThreshold = lowerBadThreshold;
            return self();
        }

        public B setLowerThresholdUnit(ThresholdUnit lowerThresholdUnit) {
            this.lowerThresholdUnit = lowerThresholdUnit;
            return self();
        }

        public B setTimestampsSec(long... timestampsSec) {
            this.timestampsSec = timestampsSec;
            return self();
        }

        public B setObservedValues(double... observedValues) {
            this.observedValues = observedValues;
            return self();
        }

        public B setPredictedValues(double... predictedValues) {
            this.predictedValues = predictedValues;
            return self();
        }

        public B setUpperWarnValues(double... upperWarnValues) {
            this.upperWarnValues = upperWarnValues;
            return self();
        }

        public B setUpperBadValues(double... upperBadValues) {
            this.upperBadValues = upperBadValues;
            return self();
        }

        public B setLowerWarnValues(double... lowerWarnValues) {
            this.lowerWarnValues = lowerWarnValues;
            return self();
        }

        public B setLowerBadValues(double... lowerBadValues) {
            this.lowerBadValues = lowerBadValues;
            return self();
        }

        @Override
        public PeriodOverPeriodAlert build() {
            return new PeriodOverPeriodAlert(this);
        }
    }

    private final static class BuilderImpl extends Builder<BuilderImpl> {

        @Override
        protected BuilderImpl self() {
            return this;
        }

    }

    public static Builder<?> builder() {
        return new BuilderImpl();
    }
}
