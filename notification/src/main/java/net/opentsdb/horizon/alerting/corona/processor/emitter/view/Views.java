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

package net.opentsdb.horizon.alerting.corona.processor.emitter.view;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import javax.annotation.concurrent.ThreadSafe;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alert.Summary;
import net.opentsdb.horizon.alerting.corona.model.alert.WindowSampler;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.EventAlert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.HealthCheckAlert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.PeriodOverPeriodAlert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricAlert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricSimpleAlert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricSummaryAlert;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventMessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthCheckAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthCheckMessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodMessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricMessageKitView;

public class Views {

    private static volatile Views INSTANCE;

    private static final SimpleDateFormat DATE_TIME_FORMATTER;

    static {
        DATE_TIME_FORMATTER = new SimpleDateFormat("MM/dd/yyyy:HH:mm:ssz");
        DATE_TIME_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /* ------------ Static Methods ------------ */

    public static Views get()
    {
        return INSTANCE;
    }

    /**
     * Initialize the {@code Views} singleton in a not thread-safe way.
     * <p>
     * Note: Not thread safe.
     *
     * @param config {@code Views} configuration.
     */
    public static void initialize(final Config config)
    {
        INSTANCE = new Views(config);
    }

    public static MessageKitView<?, ?> of(final MessageKit messageKit)
    {
        switch (messageKit.getAlertGroup().getGroupKey().getAlertType()) {
            case SINGLE_METRIC:
                return new SingleMetricMessageKitView(messageKit);
            case HEALTH_CHECK:
                return new HealthCheckMessageKitView(messageKit);
            case EVENT:
                return new EventMessageKitView(messageKit);
            case PERIOD_OVER_PERIOD:
                return new PeriodOverPeriodMessageKitView(messageKit);
        }
        throw new IllegalArgumentException(
                "Unsupported messageKit=" + messageKit);
    }

    public static SingleMetricAlertView of(final SingleMetricAlert alert)
    {
        return SingleMetricAlertViewBuilder.INSTANCE.toView(alert);
    }

    public static HealthCheckAlertView of(final HealthCheckAlert alert)
    {
        return HealthCheckAlertViewBuilder.INSTANCE.toView(alert);
    }

    public static EventAlertView of(final EventAlert alert)
    {
        return new EventAlertView(alert);
    }

    public static PeriodOverPeriodAlertView of(final PeriodOverPeriodAlert alert) {
        return new PeriodOverPeriodAlertView(alert);
    }

    public static String of(final WindowSampler sampler)
    {
        switch (sampler) {
            case ALL_OF_THE_TIMES:
                return "at all times";
            case AT_LEAST_ONCE:
                return "at least once";
            case SUMMARY:
                // Not a mistake. We never expect SUMMARY to be displayed.
            default:
                return "<unexpected: " + sampler + ">";
        }
    }

    public static String of(final Summary summary)
    {
        switch (summary) {
            case SUM:
                return "in total";
            case AVG:
                return "on average";
            default:
                return "<unexpected: " + summary + ">";
        }
    }

    public static String of(final State state)
    {
        switch (state) {
            case GOOD:
                return "good";
            case BAD:
                return "bad";
            case WARN:
                return "warn";
            case UNKNOWN:
                return "unknown";
            case MISSING:
                return "missing";
            default:
                return "<unexpected: " + state + ">";
        }
    }

    public static String of(final ViewType type)
    {
        switch (type) {
            case RECOVERY:
                return "recovery";
            case BAD:
                return "bad";
            case WARN:
                return "warn";
            case UNKNOWN:
                return "unknown";
            case MISSING:
                return "missing";
            default:
                return "<unexpected: " + type + ">";
        }
    }

    private static long fiveMinuteRoundDownMs(final long timestampMs)
    {
        return timestampMs - timestampMs % 300_000L;
    }

    /* ------------ Fields ------------ */

    private final String horizonUrl;

    private final String splunkUrl;

    private final String splunkIndex;

    private final String splunkLocale;

    /* ------------ Constructor ------------ */

    private Views(Config config)
    {
        horizonUrl = config.horizonUrl;
        splunkUrl = config.splunkUrl;
        splunkIndex = config.splunkIndex;
        splunkLocale = config.splunkLocale;
    }

    /* ------------ Methods ------------ */

    public String alertEditUrl(final long alertId)
    {
        return horizonUrl + "/a/" + alertId + "/edit";
    }

    public String alertViewUrl(final long alertId)
    {
        return horizonUrl + "/a/" + alertId + "/view";
    }

    public String alertSplunkUrl(final long alertId)
    {
        return  splunkUrl + "/" + splunkLocale + "/app/search/search?"
                + "q=search"
                + "%20index%3D" + splunkIndex
                + "%20alert_id%3D" + alertId;
    }

    public String alertSplunkUrl(final long alertId, final long timestampMs)
    {
        final long earliestMs = fiveMinuteRoundDownMs(timestampMs) - 5L * 60_000L;
        // Opportunistically hope that the alert has already
        // been written to Splunk.
        final long latestMs = earliestMs + 15L * 60_000L;

        final String earliest = DATE_TIME_FORMATTER.format(new Date(earliestMs));
        final String latest = DATE_TIME_FORMATTER.format(new Date(latestMs));

        final String query = " index=" + splunkIndex
                + " alert_id=" + alertId
                + " earliest=" + earliest
                + " latest=" + latest
                + " timeformat=%m/%d/%Y:%H:%M:%S%Z";

        // TODO: Change to specify "StandardCharsets.UTF_8" when migrating to JDK11 or later.
        try {
            return splunkUrl + "/" + splunkLocale + "/app/search/search?q=search" + URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("utf8 encoding should be known");
        }
    }

    /* ------------ Configuration ------------ */

    public static class Config
    {

        private String horizonUrl;

        private String splunkUrl;

        private String splunkIndex;

        private String splunkLocale;

        private Config() { }

        public Config setHorizonUrl(final String horizonUrl)
        {
            this.horizonUrl = horizonUrl;
            return this;
        }

        public Config setSplunkUrl(final String splunkUrl)
        {
            this.splunkUrl = splunkUrl;
            return this;
        }

        public Config setSplunkIndex(final String splunkIndex)
        {
            this.splunkIndex = splunkIndex;
            return this;
        }

        public Config setSplunkLocale(final String splunkLocale)
        {
            this.splunkLocale = splunkLocale;
            return this;
        }
    }

    public static Config config()
    {
        return new Config();
    }

    /* ------------ Static Classes ------------ */

    @ThreadSafe
    private enum SingleMetricAlertViewBuilder {

        INSTANCE;

        private static final String BREACHED_VALUE_INDEX = "BREACHED_VALUE_INDEX";

        private static final long[] EMPTY_TIMESTAMPS = new long[]{};
        private static final double[] EMPTY_VALUES = new double[]{};

        /* ------------ Fields ------------ */

        private final ThreadLocal<SingleMetricAlertView.Builder<?>> viewBuilder =
                ThreadLocal.withInitial(SingleMetricAlertView::builder);


        /* ------------ Methods ------------ */

        private String getSummaryTimeSampler(
                final SingleMetricSummaryAlert alert)
        {
            return Views.of(alert.getSummary());
        }

        private String getTimeSampler(final SingleMetricAlert alert)
        {
            if (alert.getClass() == SingleMetricSummaryAlert.class) {
                return getSummaryTimeSampler((SingleMetricSummaryAlert) alert);
            }

            return Views.of(alert.getSampler());
        }

        /**
         * Return opportunistic alert value.
         *
         * This method <em>must</em> be used only when other methods
         * of figuring out which value triggered the alert not working.
         *
         * @param vals list of display values
         * @return opportunistic alert value.
         */
        private double getMetricValueFromValues(final double[] vals) {
            if (vals == null || vals.length == 0) {
                return Double.NaN;
            }
            for (int i = vals.length-1; i >=0; --i) {
                final double val = vals[i];
                if (!Double.isNaN(val)) {
                    return val;
                }
            }
            return Double.NaN;
        }

        /**
         * Returns metric value.
         * <p>
         * TODO: This value will come from corona alert evaluator.
         * Stabbing with an opportunistic code for now.
         *
         * @param vals evaluation window values
         * @return metric value that triggered the alert.
         */
        private double getMetricValue(final Alert alert,
                                      final double[] vals)
        {
            final State alertState = alert.getState();
            if (alertState == State.MISSING || alertState == State.UNKNOWN) {
                return Double.NaN;
            }

            final Map<String, String> properties = alert.getProperties();
            if (!properties.containsKey(BREACHED_VALUE_INDEX)) {
                return getMetricValueFromValues(vals);
            }
            final int idx;
            try {
                idx = Integer.parseInt(properties.get(BREACHED_VALUE_INDEX));
            } catch (NumberFormatException e) {
                return getMetricValueFromValues(vals);
            }
            return vals[idx];
        }

        private void setSummaryAlertFields(
                final SingleMetricAlertView.Builder<?> builder,
                final SingleMetricSummaryAlert alert)
        {
            final double[] vals = alert.getSummaryValues();
            final long[] oldTimestamps = alert.getTimestampsSec();

            if (oldTimestamps.length >= vals.length) {
                final long[] newTimestamps =
                        Arrays.copyOfRange(
                                oldTimestamps,
                                oldTimestamps.length - vals.length,
                                oldTimestamps.length
                        );

                builder.setMetricValue(getMetricValue(alert, vals))
                        .setTimestampsSec(newTimestamps)
                        .setDisplayValues(vals);
            } else {
                // TODO: This is a patch to an issue with upstream,
                //       when corona evaluator sends a malformed alert.
                //       Cannot figure out why in corona code, hence
                //       fixing it here.
                builder.setMetricValue(getMetricValue(alert, vals))
                        .setTimestampsSec(EMPTY_TIMESTAMPS)
                        .setDisplayValues(EMPTY_VALUES);
            }
        }

        private void setSimpleAlertFields(
                final SingleMetricAlertView.Builder<?> builder,
                final SingleMetricSimpleAlert alert)
        {
            final double[] vals = alert.getValuesInWindow();

            builder.setMetricValue(getMetricValue(alert, vals))
                    .setTimestampsSec(alert.getTimestampsSec())
                    .setDisplayValues(vals);
        }

        protected SingleMetricAlertView toView(final SingleMetricAlert alert)
        {
            final SingleMetricAlertView.Builder builder = viewBuilder.get()
                    .reset()
                    .setEvaluationWindowMin(alert.getWindowSizeSec() / 60)
                    .setTimestampMs(alert.getTimestampSec() * 1000L)
                    .setComparator(alert.getComparator().getOperator())
                    .setThreshold(alert.getThreshold())
                    .setTimeSampler(getTimeSampler(alert))
                    .setMetric(alert.getMetric())
                    .setStateTo(Views.of(alert.getState()))
                    .setStateFrom(Views.of(alert.getStateFrom()))
                    .setTagsAndSort(alert.getTags())
                    .setNamespace(alert.getNamespace())
                    .setIsSnoozed(alert.isSnoozed())
                    .setIsNag(alert.isNag())
                    .setType(determineType(alert.getState()));

            if (alert instanceof SingleMetricSummaryAlert) {
                final SingleMetricSummaryAlert summaryAlert =
                        (SingleMetricSummaryAlert) alert;

                setSummaryAlertFields(builder, summaryAlert);
            } else {
                final SingleMetricSimpleAlert simpleAlert =
                        (SingleMetricSimpleAlert) alert;

                setSimpleAlertFields(builder, simpleAlert);
            }

            try {
                return builder.build();
            } finally {
                builder.reset();
            }
        }

        public static ViewType determineType(State state) {
            switch(state)
            {
                case BAD:
                    return ViewType.BAD;
                case GOOD:
                    return ViewType.RECOVERY;
                case WARN:
                    return ViewType.WARN;
                case MISSING:
                    return ViewType.MISSING;
                case UNKNOWN:
                    return ViewType.UNKNOWN;
                default:
                    return ViewType.UNDEFINED;
            }
        }
    }

    @ThreadSafe
    private enum HealthCheckAlertViewBuilder {

        INSTANCE;

        /* ------------ Fields ------------ */

        private final ThreadLocal<HealthCheckAlertView.Builder<?>> viewBuilder =
                ThreadLocal.withInitial(HealthCheckAlertView::builder);

        protected HealthCheckAlertView toView(final HealthCheckAlert alert)
        {
            return viewBuilder.get()
                    .reset()
                    .setTimestampMs(alert.getTimestampSec() * 1000L)
                    .setThreshold(alert.getThreshold())
                    .setStateTo(Views.of(alert.getState()))
                    .setStateFrom(Views.of(alert.getStateFrom()))
                    .setTagsAndSort(alert.getTags())
                    .setNamespace(alert.getNamespace())
                    .setTimestampsSec(alert.getTimestampsSec())
                    .setDataNamespace(alert.getDataNamespace())
                    .setApplication(alert.getApplication())
                    .setStates(alert.getStates())
                    .setIsMissingRecovery(alert.isMissingRecovery())
                    .setMissingIntervalSec(alert.getMissingIntervalSec())
                    .setIsSnoozed(alert.isSnoozed())
                    .setIsNag(alert.isNag())
                    .setStatusMessage(alert.getDetails())
                    .build();
        }
    }
}
