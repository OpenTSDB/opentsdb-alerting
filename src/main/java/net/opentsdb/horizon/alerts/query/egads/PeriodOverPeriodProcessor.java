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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import net.opentsdb.horizon.core.validate.Validate;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.config.impl.PeriodOverPeriodAlertConfig;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.http.CollectorWriter;
import net.opentsdb.horizon.alerts.http.CollectorWriters;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.model.PeriodOverPeriodAlertEvent;
import net.opentsdb.horizon.alerts.model.tsdb.Datum;
import net.opentsdb.horizon.alerts.model.tsdb.IMetric;
import net.opentsdb.horizon.alerts.model.tsdb.Metric;
import net.opentsdb.horizon.alerts.model.tsdb.Tags;
import net.opentsdb.horizon.alerts.model.tsdb.YmsStatusEvent;
import net.opentsdb.horizon.alerts.processor.impl.StatusWriter;
import net.opentsdb.horizon.alerts.query.StateTimeBasedExecutor;
import net.opentsdb.horizon.alerts.query.tsdb.TSDBClient;
import net.opentsdb.horizon.alerts.snooze.SnoozeFilter;
import net.opentsdb.horizon.alerts.state.AlertStateChange;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public final class PeriodOverPeriodProcessor
        extends StateTimeBasedExecutor<PeriodOverPeriodAlertConfig> {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(PeriodOverPeriodProcessor.class);

    private static final String EGADS_NODE_ID = "egads-alerts";
    private static final long DISPLAY_WINDOW_SEC = 25L * 60L; // 25 minutes.

    private static final long MIN_PRIMING_PERIOD_SEC = 60L * 60L; // 1 hour.
    private static final long PRIMING_SLACK_SEC_HOURLY = 10L * 60L; // 10 minutes.
    private static final long PRIMING_SLACK_SEC_WEAKLY = 60L * 60L; // 1 hour.
    private static final long PRIMING_INTERVAL_SEC = 60L * 60L; // 1 hour.

    private static final boolean MODE_PRIME = true;
    private static final boolean MODE_EVALUATE = !MODE_PRIME;

    /* ------------ Fields ------------ */

    private final TSDBClient tsdbClient;
    private final StatusWriter statusWriter;
    private final CollectorWriter collectorWriter;

    private String namespace;
    private long alertId;
    private String alertName;
    private String alertType;
    private EgadsQueryTemplate queryTemplate;
    private EgadsResponseParser responseParser;
    // Display window in seconds. EGADs data granularity is 1 minute.
    private long displayWindowSec;
    // Query baseline period.
    private long baselinePeriodSec;
    // How many seconds into the future priming calls should be made.
    // Based on the baselinePeriodSec
    private long primingSlackSec;

    private long lastPrimedEndTimeSec = 0;

    /* ------------ Constructor ------------ */

    public PeriodOverPeriodProcessor(final PeriodOverPeriodAlertConfig alertConfig) {
        this(
                alertConfig,
                new TSDBClient(
                        new EnvironmentConfig().getTsdbEndpoint(),
                        new EnvironmentConfig().getTSDBAuthProvider()
                ),
                new StatusWriter(alertConfig, new SnoozeFilter()),
                CollectorWriters.getPopCollectorWriter()
        );
    }

    public PeriodOverPeriodProcessor(
            final PeriodOverPeriodAlertConfig alertConfig,
            final TSDBClient tsdbClient,
            final StatusWriter statusWriter,
            final CollectorWriter collectorWriter) {
        super(alertConfig);
        Validate.paramNotNull(tsdbClient, "tsdbClient cannot be null");
        Validate.paramNotNull(statusWriter, "statusWriter cannot be null");
        Validate.paramNotNull(collectorWriter, "collectorWriter cannot be null");
        this.tsdbClient = tsdbClient;
        this.statusWriter = statusWriter;
        this.collectorWriter = collectorWriter;
        this.responseParser = EgadsResponseParser.create(EGADS_NODE_ID);
        displayWindowSec = DISPLAY_WINDOW_SEC;
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean prepAndValidate(final PeriodOverPeriodAlertConfig alertConfig,
                                   final AlertStateStore alertStateStore) {
        namespace = alertConfig.getNamespace();
        alertId = alertConfig.getAlertId();
        alertName = alertConfig.getAlertName();
        alertType = alertConfig.getAlertType().getString();
        queryTemplate =
                DefaultEgadsQueryTemplate.create(
                        alertConfig.getQueryJson(),
                        alertConfig.getEgadsNodeId(),
                        EGADS_NODE_ID
                );
        baselinePeriodSec = queryTemplate.getBaselinePeriodSec();
        primingSlackSec = computePrimingSlackSec(baselinePeriodSec);
        return true;
    }

    private long computePrimingSlackSec(long baselinePeriodSec) {
        // Constants in seconds.
        final long ZERO = 0L;
        final long ONE_HOUR = 60L * 60L;
        final long ONE_WEAK = 7L * 24L * 60L * 60L;

        if (ZERO <= baselinePeriodSec && baselinePeriodSec < ONE_HOUR) {
            return 0L;
        } else if (ONE_HOUR <= baselinePeriodSec && baselinePeriodSec < ONE_WEAK) {
            return PRIMING_SLACK_SEC_HOURLY;
        }
        return PRIMING_SLACK_SEC_WEAKLY;
    }

    @Override
    public AlertEventBag execute(final long endTime,
                                 final TimeUnit timeUnit,
                                 final AlertStateStore stateStore)
            throws AlertException {
        final String request = buildRequest(endTime, timeUnit, MODE_EVALUATE);
        LOG.debug("tsdb request: alert_id={}, end_time={}, request=<<{}>>.",
                alertId, endTime, request);
        final String responsePayload = tsdbClient.getResponse(request, alertId);
        LOG.debug("tsdb response: alert_id={}, end_time={}, response=<<{}>>.",
                alertId, endTime, responsePayload);

        final EgadsResponse response;
        try {
            response =  responseParser.parse(responsePayload);
        } catch (Exception e) {
            LOG.error("failed to parse response: alert_id={}, response=<<{}>>",
                    alertId, responsePayload);
            throw new AlertException(e.getMessage());
        }

        final List<AlertEvent> alerts = processResponse(response, stateStore);
        LOG.debug("processed response result: alert_id={}, end_time={}, alerts={}",
                alertId, endTime, alerts);

        tryPrimeNextModel(endTime, timeUnit);

        return new AlertEventBag(alerts, getAlertConfig());
    }

    private String buildRequest(final long endTime,
                                final TimeUnit timeUnit,
                                boolean isPriming) {
        long endTimeSec = TimeUnit.SECONDS.convert(endTime, timeUnit);
        long startTimeSec = endTimeSec - displayWindowSec;
        return queryTemplate.format(startTimeSec, endTimeSec, isPriming);
    }

    private List<AlertEvent> processResponse(final EgadsResponse response,
                                             final AlertStateStore stateStore) {
        final List<AlertEvent> alerts = Lists.newArrayList();
        final List<EgadsDataItem> dataItems = response.getDataItems();
        for (EgadsDataItem dataItem : dataItems) {
            try {
                Optional<AlertEvent> maybeAlert =
                        evaluate(response, stateStore, dataItem);
                maybeAlert.ifPresent(alerts::add);
            } catch (Exception e) {
                LOG.error("data item evaluation failed: alert_id={}, data_item={}",
                        alertId, dataItem);
            }
            tryPostMetrics(dataItem);
        }
        return alerts;
    }

    /**
     * Evaluate the given instance of {@link EgadsDataItem}.
     *
     * @param dataItem instance of {@link EgadsDataItem}
     * @return generated alert event.
     */
    private Optional<AlertEvent> evaluate(final EgadsResponse response,
                                          final AlertStateStore stateStore,
                                          final EgadsDataItem dataItem) {
        final Optional<EgadsAlert> maybeEgadsAlert = dataItem.getLastAlert();
        final AlertState newState = evaluateNewState(maybeEgadsAlert);

        final AlertStateChange stageChange =
                stateStore.raiseAlert(
                        namespace,
                        alertId,
                        dataItem.getTags(),
                        newState
                );

        tryWriteStatus(dataItem, maybeEgadsAlert, newState);

        if (!stageChange.raiseAlert()) {
            // No need to raise an alert for this EGADs evaluation.
            return Optional.empty();
        }

        final PeriodOverPeriodAlertEvent alert =
                buildAlert(
                        response,
                        dataItem,
                        maybeEgadsAlert,
                        stageChange
                );
        return Optional.of(alert);
    }

    /**
     * Evaluate the new state from the possible {@link EgadsAlert} instance.
     *
     * @param maybeAlert optional of the last alert in the <em>dataItem</em>
     * @return the new state for the given data item.
     */
    private AlertState evaluateNewState(final Optional<EgadsAlert> maybeAlert) {
        if (maybeAlert.isPresent()) {
            EgadsAlert alert = maybeAlert.get();

            // The alert is evaluated for the end of the query range,
            // hence it is new data. Assume that we saw all alerts with old
            // timestamps before.
            //
            // The egads alerts state is the new state.
            return alert.getAlertState();
        }

        return AlertState.GOOD;
    }

    private void tryWriteStatus(final EgadsDataItem dataItem,
                                final Optional<EgadsAlert> maybeEgadsAlert,
                                final AlertState newState) {
        try {
            AlertUtils.writeStatus(
                    statusWriter,
                    alertName,
                    alertType,
                    Instant.now().getEpochSecond(),
                    namespace,
                    newState,
                    alertId,
                    dataItem.getTags(),
                    formatStatusMessage(dataItem, maybeEgadsAlert, newState)
            );
        } catch (Exception e) {
            LOG.error("write status failed: alert_id={}, new_state={}, data_item={}",
                    alertId, newState, dataItem);
        }
    }

    private String formatStatusMessage(
            final EgadsDataItem dataItem,
            final Optional<EgadsAlert> maybeEgadsAlert,
            final AlertState state) {
        if (state == AlertState.GOOD) {
            return String.format("Observed value %.4f is within recovery bounds from predicted %.4f.",
                    dataItem.getLastObservedValue(),
                    dataItem.getLastPredictedValue());
        }

        // Well, if not recovery, then there is supposed to be an EgadsAlert.
        final EgadsAlert egadsAlert = maybeEgadsAlert.get();

        final String formatStr;
        switch (egadsAlert.getThresholdType()) {
            case UPPER_BAD:
                formatStr = "Observed value %.4f breached the upper bad threshold %.4f with predicted %.4f";
                break;
            case LOWER_BAD:
                formatStr = "Observed value %.4f breached the lower bad threshold %.4f with predicted %.4f";
                break;
            case LOWER_WARN:
                formatStr = "Observed value %.4f breached the lower warn threshold %.4f with predicted %.4f";
                break;
            case UPPER_WARN:
                formatStr = "Observed value %.4f breached the upper warn threshold %.4f with predicted %.4f";
                break;
            default:
                // On a loose end, why not.
                return egadsAlert.getMessage();
        }

        return String.format(formatStr,
                egadsAlert.getObservedValue(),
                egadsAlert.getThresholdValue(),
                dataItem.getLastPredictedValue());
    }

    private PeriodOverPeriodAlertEvent buildAlert(
            final EgadsResponse response,
            final EgadsDataItem dataItem,
            final Optional<EgadsAlert> maybeEgadsAlert,
            final AlertStateChange stateChange) {
        final PeriodOverPeriodAlertEvent alert = new PeriodOverPeriodAlertEvent();

        // Default fields for corona.model.AlertEvent
        {
            alert.setNamespace(namespace);
            alert.setAlertId(alertId);
            alert.setAlertRaisedTimestamp(Instant.now().getEpochSecond());
            alert.setTags(dataItem.getTags());
            alert.setOriginSignal(stateChange.getPreviousState());
            alert.setCurrentSignal(stateChange.getCurrentState());
            alert.setAlertHash(
                    AlertUtils.getHashForNAMT(namespace, alertId, dataItem.getTags())
            );
            alert.setNag(stateChange.isNag());
        }

        // Fields for information display.
        {
            alert.setMetric(response.getMetricName().orElseThrow(
                    () -> new RuntimeException(String.format(
                            "metric name is always expected here: alert_id=%d", alertId
                    ))
            ));
            alert.setObservedValue(dataItem.getLastObservedValue());
            alert.setPredictedValue(dataItem.getLastPredictedValue());
            dataItem.getLastUpperWarnValue()
                    .ifPresent(alert::setUpperWarnValue);
            dataItem.getLastUpperBadValue()
                    .ifPresent(alert::setUpperBadValue);
            dataItem.getLastLowerWarnValue()
                    .ifPresent(alert::setLowerWarnValue);
            dataItem.getLastLowerBadValue()
                    .ifPresent(alert::setLowerBadValue);

            // Override some with values from egads alert.
            if (maybeEgadsAlert.isPresent()) {
                final EgadsAlert egadsAlert = maybeEgadsAlert.get();
                alert.setObservedValue(egadsAlert.getObservedValue());
                alert.setBreachedThresholdValue(
                        egadsAlert.getThresholdValue(),
                        egadsAlert.getThresholdType()
                );
            }
        }

        // Config settings.
        {
            final PeriodOverPeriodAlertConfig config = getAlertConfig();
            alert.setUpperWarnThreshold(config.getUpperWarnThreshold());
            alert.setUpperBadThreshold(config.getUpperBadThreshold());
            alert.setUpperThresholdUnit(config.getUpperThresholdUnit());
            alert.setLowerWarnThreshold(config.getLowerWarnThreshold());
            alert.setLowerBadThreshold(config.getLowerBadThreshold());
            alert.setLowerThresholdUnit(config.getLowerThresholdUnit());
        }

        // Data for visualization.
        {
            alert.setTimestampsSec(dataItem.getTimestampsSec());
            alert.setObservedValues(dataItem.getObservedValues());
            alert.setPredictedValues(dataItem.getPredictedValues());
            dataItem.getUpperWarnValues().ifPresent(alert::setUpperWarnValues);
            dataItem.getUpperBadValues().ifPresent(alert::setUpperBadValues);
            dataItem.getLowerWarnValues().ifPresent(alert::setLowerWarnValues);
            dataItem.getLowerBadValues().ifPresent(alert::setLowerBadValues);
        }

        return alert;
    }

    // --------------- Metric-posting related methods --------------- //

    private static final String APPLICATION = "ALERTS";
    private static final String M_LOWER_BAD_LIMIT = "lowerbadlimit";
    private static final String M_LOWER_WARN_LIMIT = "lowerwarnlimit";
    private static final String M_OBSERVED = "observed";
    private static final String M_PROJECTED = "projected";
    private static final String M_UPPER_BAD_LIMIT = "upperbadlimit";
    private static final String M_UPPER_WARN_LIMIT = "upperwarnlimit";
    private static final String TK_ALERT_ID = "_alert_id";
    private static final String TK_ALERT_NAME = "_alert_name";
    private static final String TK_ALERT_TYPE = "_alert_type";
    private static final String TK_THRESHOLD_MODEL = "_threshold_model";
    private static final String TV_OLYMPIC_SCORING = "OlympicScoring";

    private void tryPostMetrics(final EgadsDataItem dataItem) {
        final Map<String, String> tags = buildTags(dataItem.getTags());
        final Map<String, IMetric> metrics = buildMetrics(dataItem);

        final Datum datum = Datum.newBuilder()
                .withCluster(namespace)
                .withApplication(APPLICATION)
                .withTags(new Tags(tags))
                .withMetrics(metrics)
                .withTimestamp(dataItem.getLastTimestampSec())
                .build();

        final YmsStatusEvent event = new YmsStatusEvent();
        event.setData(datum);
        try {
            collectorWriter.sendStatusEvent(event);
        } catch (Exception e) {
            LOG.error("send status event failed: alert_id={}, event={}", alertId, event, e);
        }
    }

    private Map<String, String> buildTags(final Map<String, String> original) {
        final Map<String, String> tags = new HashMap<>(original);
        tags.put(TK_ALERT_ID, String.valueOf(alertId));
        tags.put(TK_ALERT_NAME, String.valueOf(alertName));
        tags.put(TK_ALERT_TYPE, String.valueOf(alertType));
        tags.put(TK_THRESHOLD_MODEL, TV_OLYMPIC_SCORING);
        return tags;
    }

    private Map<String, IMetric> buildMetrics(final EgadsDataItem dataItem) {
        final Map<String, IMetric> metrics = new HashMap<>();

        metrics.put(M_OBSERVED, new Metric(dataItem.getLastObservedValue()));
        metrics.put(M_PROJECTED, new Metric(dataItem.getLastPredictedValue()));

        dataItem.getLastLowerBadValue().ifPresent(v -> {
            metrics.put(M_LOWER_BAD_LIMIT, new Metric(v));
        });
        dataItem.getLastLowerWarnValue().ifPresent(v -> {
            metrics.put(M_LOWER_WARN_LIMIT, new Metric(v));
        });
        dataItem.getLastUpperBadValue().ifPresent(v -> {
            metrics.put(M_UPPER_BAD_LIMIT, new Metric(v));
        });
        dataItem.getLastUpperWarnValue().ifPresent(v -> {
            metrics.put(M_UPPER_WARN_LIMIT, new Metric(v));
        });

        return metrics;
    }

    // --------------- Model-priming related methods --------------- //

    /*
     * Egads queries are expensive in terms of time (and compute, but this
     * component doesn't care about it). It might take a minute or more for
     * a model to be build.
     *
     * To avoid the cost during alert evaluation, a priming query call is made
     * to start preparing a model for future evaluations. Priming calls are
     * made for models with period more than 1 hour inclusive.
     *
     * Query mode is set to `PREDICT`, which is managed by the EgadsQueryTemplate
     * implementations.
     *
     * Priming calls have a future timestamp relative to the `endTime` passed
     * to the processor. The amount of shift is called `slack`. The slack
     * depends on the Egads model period (mp) setting and the following rules
     * hold:
     *     0 sec  <  mp < 1 hour -> no priming calls
     *     1 hour <= mp < 1 week -> 10 minutes in the future.
     *     1 week <= mp          -> 1 hour in the future.
     *
     * To avoid overloading TSDB, priming is done with 1 hour frequency.
     * Important part is to make priming calls with aligned periods, e.g.
     * end time is always the 17-th minute of every hour. Note, that the call
     * itself doesn't have to be at the exact minute, it can be made a bit
     * later, but the query range has to be aligned.
     */

    private final String MSG_423_LOCKED = "423 Locked";
    private final String EMPTY = "";

    private void tryPrimeNextModel(final long endTime, final TimeUnit timeUnit) {
        final long endTimeSec = TimeUnit.SECONDS.convert(endTime, timeUnit);
        if (baselinePeriodSec < MIN_PRIMING_PERIOD_SEC
                || !isTimeToPrime(endTimeSec)) {
            // No priming for models smaller than MIN_PRIMING_PERIOD_SEC, or
            // before it is priming time.
            return;
        }

        long primingTimeSec = nextPrimingEndTimeSec(endTimeSec);
        final String request =
                buildRequest(
                        primingTimeSec + primingSlackSec,
                        TimeUnit.SECONDS,
                        MODE_PRIME
                );

        LOG.debug("prime time: alert_id={}, prime_end_time_sec={}, last_primed_time_sec={}, request=<<{}>>.",
                alertId, primingTimeSec, lastPrimedEndTimeSec, request);

        try {
            final String responsePayload = tsdbClient.getResponse(request, alertId);
            LOG.debug("prime tsdb response: alert_id={}, end_time={}, response=<<{}>>.",
                    alertId, endTime, responsePayload);
        } catch (AlertException e) {
            final Throwable cause = e.getCause();
            final String causeMsg = cause == null ? EMPTY : cause.getMessage();
            final String msg = e.getMessage();
            if ((causeMsg == null || !causeMsg.contains(MSG_423_LOCKED))
                    && (msg == null || !msg.contains(MSG_423_LOCKED))) {
                LOG.error("priming call failed: alert_id=" + alertId + ".", e);
                return;
            }
        } catch (Exception e) {
            LOG.error("priming unexpected exception: alert_id=" + alertId + ".", e);
            return;
        }

        // Update the state for the next run.
        lastPrimedEndTimeSec = primingTimeSec;
    }

    private boolean isTimeToPrime(final long endTimeSec) {
        return endTimeSec - lastPrimedEndTimeSec >= PRIMING_INTERVAL_SEC;
    }

    /**
     * Compute next priming time based on the given end time.
     * <p>
     * This method does not account for priming slack.
     * <p>
     * Note: if last priming was done more than 2 priming intervals before,
     * then we reinitialize priming with current `endTime` to catchup with
     * current queries.
     *
     * @param endTimeSec end time in seconds
     * @return next priming end time in seconds.
     */
    private long nextPrimingEndTimeSec(final long endTimeSec) {
        final long lastPrimedSec = lastPrimedEndTimeSec;

        if (lastPrimedSec == 0L
                || endTimeSec - lastPrimedSec > 2L * PRIMING_INTERVAL_SEC) {
            // Use the current end time if:
            // - no priming was done before; or
            // - last priming time was too long ago for whatever reason.
            return endTimeSec;
        }

        return lastPrimedSec + PRIMING_INTERVAL_SEC;
    }

}
