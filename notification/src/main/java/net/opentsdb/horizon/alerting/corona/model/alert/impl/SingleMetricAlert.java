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

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.Comparator;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alert.WindowSampler;

public abstract class SingleMetricAlert extends Alert {

    /* ------------ Constants ------------ */

    private static final double[] EMPTY_DOUBLE = new double[0];

    private static final long[] EMPTY_LONG = new long[0];

    /* ------------ Fields ------------ */

    @Getter
    private final double[] valuesInWindow;

    @Getter
    private final long[] timestampsSec;

    @Getter
    private final double threshold;

    @Getter
    private final Comparator comparator;

    @Getter
    private final String metric;

    @Getter
    private final int windowSizeSec;

    @Getter
    private final WindowSampler sampler;

    /* ------------ Constructor ------------ */

    protected SingleMetricAlert(final Builder<?, ?> builder)
    {
        super(builder);
        Objects.requireNonNull(builder.comparator, "comparator cannot be null");
        Objects.requireNonNull(builder.sampler, "sampler cannot be null");
        this.valuesInWindow = builder.valuesInWindow == null ?
                EMPTY_DOUBLE : builder.valuesInWindow;
        this.timestampsSec = builder.timestampsSec == null ?
                EMPTY_LONG : builder.timestampsSec;
        if (this.valuesInWindow.length != this.timestampsSec.length) {
            throw new IllegalArgumentException(
                    "valuesInWindow and timestampsSec has to be the same length");
        }
        this.threshold = builder.threshold;
        this.comparator = builder.comparator;
        this.metric = builder.metric == null ? "n/a" : builder.metric;
        this.windowSizeSec = builder.windowSizeSec;
        this.sampler = builder.sampler;
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        SingleMetricAlert that = (SingleMetricAlert) o;
        return Arrays.equals(valuesInWindow, that.valuesInWindow) &&
                Arrays.equals(timestampsSec, that.timestampsSec) &&
                Double.compare(threshold, that.threshold) == 0 &&
                comparator == that.comparator &&
                Objects.equals(metric, that.metric) &&
                windowSizeSec == that.windowSizeSec &&
                sampler == that.sampler;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(valuesInWindow);
        result = 31 * result + Arrays.hashCode(timestampsSec);
        result = 31 * result +
                Objects.hash(
                        threshold,
                        comparator,
                        metric,
                        windowSizeSec,
                        sampler
                );
        return result;
    }

    @Override
    public String toString()
    {
        return super.toString() +
                ", valuesInWindow=" + Arrays.toString(valuesInWindow) +
                ", timestampsSec=" + Arrays.toString(timestampsSec) +
                ", threshold=" + threshold +
                ", comparator=" + comparator +
                ", metric=" + metric +
                ", windowSizeSec=" + windowSizeSec +
                ", sampler=" + sampler;
    }

    /* ------------ Builders ------------ */

    protected static abstract class Builder<A extends Alert, B extends Builder<A, B>>
            extends Alert.Builder<A, B>
    {

        private double[] valuesInWindow;

        private long[] timestampsSec;

        private double threshold = Double.NaN;

        private Comparator comparator;

        private String metric;

        private int windowSizeSec;

        private WindowSampler sampler;

        protected Builder()
        {
            super(AlertType.SINGLE_METRIC);
        }

        public B setValuesInWindow(final double... valuesInWindow)
        {
            this.valuesInWindow = valuesInWindow;
            return self();
        }

        public B setTimestampsSec(final long... timestampsSec)
        {
            this.timestampsSec = timestampsSec;
            return self();
        }

        public B setThreshold(final double threshold)
        {
            this.threshold = threshold;
            return self();
        }

        public B setComparator(final Comparator comparator)
        {
            this.comparator = comparator;
            return self();
        }

        public B setMetric(final String metric)
        {
            this.metric = metric;
            return self();
        }

        public B setWindowSizeSec(final int windowSizeSec)
        {
            this.windowSizeSec = windowSizeSec;
            return self();
        }

        public B setSampler(final WindowSampler sampler)
        {
            this.sampler = sampler;
            return self();
        }
    }
}
