/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.query.egads;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;

import net.opentsdb.horizon.core.validate.Validate;

public final class DefaultEgadsDataItem implements EgadsDataItem {

    private final long[] timestampsSec;
    private final double[] observedValues;
    private final double[] predictedValues;
    private final EgadsAlert[] alerts;
    private final double[] upperWarnValues;
    private final double[] upperBadValues;
    private final double[] lowerWarnValues;
    private final double[] lowerBadValues;
    private final SortedMap<String, String> tags;

    private DefaultEgadsDataItem(DefaultBuilder builder) {
        Validate.paramNotNull(builder.timestampsSec, "timestampsSec");
        Validate.paramNotNull(builder.observedValues, "observedValues");
        Validate.paramNotNull(builder.predictedValues, "predictedValues");
        { // Validate
            final int len = builder.timestampsSec.length;
            Validate.isTrue(len > 0, "no timestamps given");
            Validate.isTrue(len == builder.observedValues.length,
                    "length mismatch for %s, exp=%d actual=%d",
                    "observed", len, builder.observedValues.length);
            Validate.isTrue(len == builder.predictedValues.length,
                    "length mismatch for %s, exp=%d actual=%d",
                    "predicted", len, builder.predictedValues.length);
            if (builder.upperWarnValues != null) {
                Validate.isTrue(len == builder.upperWarnValues.length,
                        "length mismatch for %s, exp=%d actual=%d",
                        "upperWarn", len, builder.upperWarnValues.length);
            }
            if (builder.upperBadValues != null) {
                Validate.isTrue(len == builder.upperBadValues.length,
                        "length mismatch for %s, exp=%d actual=%d",
                        "upperBad", len, builder.upperBadValues.length);
            }
            if (builder.lowerWarnValues != null) {
                Validate.isTrue(len == builder.lowerWarnValues.length,
                        "length mismatch for %s, exp=%d actual=%d",
                        "lowerWarn", len, builder.lowerWarnValues.length);
            }
            if (builder.lowerBadValues != null) {
                Validate.isTrue(len == builder.lowerBadValues.length,
                        "length mismatch for %s, exp=%d actual=%d",
                        "lowerBad", len, builder.lowerBadValues.length);
            }
        }

        this.timestampsSec = builder.timestampsSec;
        this.observedValues = builder.observedValues;
        this.predictedValues = builder.predictedValues;
        this.alerts = builder.alerts == null
                ? EgadsAlert.emptyAlerts()
                : builder.alerts;
        this.upperWarnValues = builder.upperWarnValues;
        this.upperBadValues = builder.upperBadValues;
        this.lowerWarnValues = builder.lowerWarnValues;
        this.lowerBadValues = builder.lowerBadValues;
        this.tags = builder.tags == null
                ? Collections.emptySortedMap()
                : Collections.unmodifiableSortedMap(builder.tags);
    }

    @Override
    public long[] getTimestampsSec() {
        return timestampsSec;
    }

    @Override
    public double[] getObservedValues() {
        return observedValues;
    }

    @Override
    public double[] getPredictedValues() {
        return predictedValues;
    }

    @Override
    public EgadsAlert[] getAlerts() {
        return alerts;
    }

    @Override
    public Optional<double[]> getUpperWarnValues() {
        return Optional.ofNullable(upperWarnValues);
    }

    @Override
    public Optional<double[]> getUpperBadValues() {
        return Optional.ofNullable(upperBadValues);
    }

    @Override
    public Optional<double[]> getLowerWarnValues() {
        return Optional.ofNullable(lowerWarnValues);
    }

    @Override
    public Optional<double[]> getLowerBadValues() {
        return Optional.ofNullable(lowerBadValues);
    }

    @Override
    public SortedMap<String, String> getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultEgadsDataItem that = (DefaultEgadsDataItem) o;
        return Arrays.equals(timestampsSec, that.timestampsSec) &&
                Arrays.equals(observedValues, that.observedValues) &&
                Arrays.equals(alerts, that.alerts) &&
                Arrays.equals(upperWarnValues, that.upperWarnValues) &&
                Arrays.equals(upperBadValues, that.upperBadValues) &&
                Arrays.equals(lowerWarnValues, that.lowerWarnValues) &&
                Arrays.equals(lowerBadValues, that.lowerBadValues) &&
                Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(tags);
        result = 31 * result + Arrays.hashCode(timestampsSec);
        result = 31 * result + Arrays.hashCode(observedValues);
        result = 31 * result + Arrays.hashCode(alerts);
        result = 31 * result + Arrays.hashCode(upperWarnValues);
        result = 31 * result + Arrays.hashCode(upperBadValues);
        result = 31 * result + Arrays.hashCode(lowerWarnValues);
        result = 31 * result + Arrays.hashCode(lowerBadValues);
        return result;
    }

    @Override
    public String toString() {
        return "DefaultEgadsDataItem{" +
                "timestampsSec=" + Arrays.toString(timestampsSec) +
                ", observedValues=" + Arrays.toString(observedValues) +
                ", alerts=" + Arrays.toString(alerts) +
                ", upperWarnValues=" + Arrays.toString(upperWarnValues) +
                ", upperBadValues=" + Arrays.toString(upperBadValues) +
                ", lowerWarnValues=" + Arrays.toString(lowerWarnValues) +
                ", lowerBadValues=" + Arrays.toString(lowerBadValues) +
                ", tags=" + tags +
                '}';
    }

    /**
     * Builder interface for {@link DefaultEgadsDataItem}.
     */
    public interface Builder extends EgadsDataItem.Builder<Builder> {

    }

    private final static class DefaultBuilder implements Builder {

        private long[] timestampsSec;
        private double[] observedValues;
        private double[] predictedValues;
        private EgadsAlert[] alerts;
        private double[] upperWarnValues;
        private double[] upperBadValues;
        private double[] lowerWarnValues;
        private double[] lowerBadValues;
        private SortedMap<String, String> tags;

        @Override
        public String toString() {
            return "DefaultBuilder{" +
                    "timestampsSec=" + Arrays.toString(timestampsSec) +
                    ", observedValues=" + Arrays.toString(observedValues) +
                    ", predictedValues=" + Arrays.toString(predictedValues) +
                    ", alerts=" + Arrays.toString(alerts) +
                    ", upperWarnValues=" + Arrays.toString(upperWarnValues) +
                    ", upperBadValues=" + Arrays.toString(upperBadValues) +
                    ", lowerWarnValues=" + Arrays.toString(lowerWarnValues) +
                    ", lowerBadValues=" + Arrays.toString(lowerBadValues) +
                    ", tags=" + tags +
                    '}';
        }

        @Override
        public Builder setTimestampsSec(long[] timestampsSec) {
            this.timestampsSec = timestampsSec;
            return this;
        }

        @Override
        public Builder setObservedValues(double[] observedValues) {
            this.observedValues = observedValues;
            return this;
        }

        @Override
        public Builder setPredictedValues(double[] predictedValues) {
            this.predictedValues = predictedValues;
            return this;
        }

        @Override
        public Builder setAlerts(EgadsAlert[] alerts) {
            this.alerts = alerts;
            return this;
        }

        @Override
        public Builder setUpperWarnValues(double[] upperWarnValues) {
            this.upperWarnValues = upperWarnValues;
            return this;
        }

        @Override
        public Builder setUpperBadValues(double[] upperBadValues) {
            this.upperBadValues = upperBadValues;
            return this;
        }

        @Override
        public Builder setLowerWarnValues(double[] lowerWarnValues) {
            this.lowerWarnValues = lowerWarnValues;
            return this;
        }

        @Override
        public Builder setLowerBadValues(double[] lowerBadValues) {
            this.lowerBadValues = lowerBadValues;
            return this;
        }

        @Override
        public Builder setTags(SortedMap<String, String> tags) {
            this.tags = tags;
            return this;
        }

        @Override
        public EgadsDataItem build() {
            return new DefaultEgadsDataItem(this);
        }
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }
}
