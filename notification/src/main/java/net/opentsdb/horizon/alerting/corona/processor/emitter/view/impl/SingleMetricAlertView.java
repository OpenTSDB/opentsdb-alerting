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

import java.util.Arrays;
import java.util.Objects;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;

@Getter
public class SingleMetricAlertView extends AlertView {

    /* ------------ Constants ------------ */

    private static final long[] EMPTY_LONGS = new long[]{};

    private static final double[] EMPTY_DOUBLES = new double[]{};

    private static final String NULL = "<null>";

    /* ------------ Fields ------------ */

    /**
     * Fully qualified (with namespace) metric name or expression
     * representation.
     *
     * @return full metric name.
     */
    private final String metric;

    /**
     * Metric value.
     *
     * @return metric value.
     */
    private final double metricValue;

    /**
     * Display values to display.
     *
     * @return display values.
     */
    private final double[] displayValues;

    /**
     * Comparator, {@code <=, <, >, =>, =}, for the threshold condition.
     *
     * @return comparator representation.
     */
    private final String comparator;

    /**
     * Threshold value.
     *
     * @return threshold value.
     */
    private final double threshold;

    /**
     * At least once, at all times, on average, in total.
     *
     * @return time sampler representation.
     */
    private final String timeSampler;

    /**
     * Evaluation window in minutes.
     *
     * @return evaluation window size in minutes.
     */
    private final int evaluationWindowMin;

    /* ------------ Constructors ------------ */

    public SingleMetricAlertView(final Builder<?> builder)
    {
        super(builder);
        this.metric = builder.metric;
        this.metricValue = builder.metricValue;
        this.displayValues = builder.displayValues == null ?
                EMPTY_DOUBLES : builder.displayValues;
        this.comparator = builder.comparator;
        this.threshold = builder.threshold;
        this.timeSampler = builder.timeSampler;
        this.evaluationWindowMin = builder.evaluationWindowMin;
    }

    /* ------------ Methods ------------ */

    @Override
    public String getDescription(final String emphasisStart,
                                 final String emphasisStop)
    {
        if (Views.of(State.MISSING).equals(getStateTo())) {
            return String.format(
                    "%s%s%s no data received for at least %s%d minutes%s",
                    emphasisStart,
                    metric,
                    emphasisStop,
                    emphasisStart,
                    evaluationWindowMin,
                    emphasisStop
            );
        } else {
            return String.format(
                    "%s%s %s %f %s%s in the last %s%d minutes%s",
                    emphasisStart,
                    metric,
                    comparator,
                    threshold,
                    timeSampler,
                    emphasisStop,
                    emphasisStart,
                    evaluationWindowMin,
                    emphasisStop
            );
        }
    }

    @Override
    public boolean equals(final Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        SingleMetricAlertView that = (SingleMetricAlertView) o;
        return Double.compare(that.metricValue, metricValue) == 0 &&
                Double.compare(that.threshold, threshold) == 0 &&
                evaluationWindowMin == that.evaluationWindowMin &&
                Objects.equals(metric, that.metric) &&
                Objects.equals(comparator, that.comparator) &&
                Objects.equals(timeSampler, that.timeSampler);
    }

    @Override
    public int hashCode()
    {
        int hash = Arrays.hashCode(displayValues);
        return hash * 31 +
                Objects.hash(
                        metric,
                        metricValue,
                        comparator,
                        threshold,
                        timeSampler,
                        evaluationWindowMin
                );
    }

    @Override
    public String toString()
    {
        return "SingleMetricAlertView{" +
                super.toString() +
                ", metric='" + metric + '\'' +
                ", metricValue=" + metricValue +
                ", displayValues=" + Arrays.toString(displayValues) +
                ", comparator='" + comparator + '\'' +
                ", threshold=" + threshold +
                ", timeSampler='" + timeSampler + '\'' +
                ", evaluationWindowMin=" + evaluationWindowMin +
                '}';
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends AlertView.Builder<B>
    {

        private String metric;

        private double metricValue = Double.NaN;

        private double[] displayValues;

        private String comparator;

        private double threshold = Double.NaN;

        private String timeSampler;

        private int evaluationWindowMin = Integer.MIN_VALUE;

        private Builder() {}

        public B setMetric(final String metric)
        {
            this.metric = metric;
            return self();
        }

        public B setMetricValue(final double metricValue)
        {
            this.metricValue = metricValue;
            return self();
        }

        public B setDisplayValues(final double ...displayValues)
        {
            this.displayValues = displayValues;
            return self();
        }

        public B setComparator(final String comparator)
        {
            this.comparator = comparator;
            return self();
        }

        public B setThreshold(final double threshold)
        {
            this.threshold = threshold;
            return self();
        }

        public B setTimeSampler(final String timeSampler)
        {
            this.timeSampler = timeSampler;
            return self();
        }

        public B setEvaluationWindowMin(final int evaluationWindowMin)
        {
            this.evaluationWindowMin = evaluationWindowMin;
            return self();
        }

        @Override
        public B reset()
        {
            super.reset();
            metric = null;
            metricValue = Double.NaN;
            displayValues = null;
            comparator = null;
            threshold = Double.NaN;
            timeSampler = null;
            evaluationWindowMin = Integer.MIN_VALUE;
            return self();
        }

        /**
         * Build a {@link SingleMetricAlertView} instance.
         * <p>
         * The builder is NOT reset after creation. Use {@link #reset()}.
         *
         * @return {@link SingleMetricAlertView} instance.
         */
        public SingleMetricAlertView build()
        {
            return new SingleMetricAlertView(this);
        }
    }

    private static final class BuilderImpl extends Builder<BuilderImpl> {

        @Override
        protected BuilderImpl self()
        {
            return this;
        }
    }

    public static Builder<?> builder()
    {
        return new BuilderImpl();
    }
}
