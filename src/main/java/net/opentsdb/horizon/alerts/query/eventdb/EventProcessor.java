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

package net.opentsdb.horizon.alerts.query.eventdb;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import net.opentsdb.horizon.alerts.processor.impl.StatusWriter;
import net.opentsdb.horizon.alerts.snooze.SnoozeFilter;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.config.impl.EventAlertConfig;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.model.Event;
import net.opentsdb.horizon.alerts.model.EventAlertEvent;
import net.opentsdb.horizon.alerts.query.StateTimeBasedExecutor;
import net.opentsdb.horizon.alerts.state.AlertStateChange;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerts.query.tsdb.TSDBClient;

import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;

public class EventProcessor extends StateTimeBasedExecutor<EventAlertConfig> {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(EventProcessor.class);

    /* ------------ Fields ------------ */

    private String namespace;

    private long alertId;

    private String alertName;

    private String alertTypeString;

    private int threshold;

    private int slidingWindowSec;

    private String queryNamespace;

    private String queryFilter;

    private QueryTemplate queryTemplate;

    private final TSDBClient client;

    private StatusWriter statusWriter;

    /* ------------ Constructor ------------ */

    public EventProcessor(final EventAlertConfig alertConfig,
                          final TSDBClient tsdbClient) {
        super(alertConfig);
        this.client = tsdbClient;
    }

    public EventProcessor(final EventAlertConfig alertConfig) {
        this(
                alertConfig,
                new TSDBClient(
                        new EnvironmentConfig().getTsdbEndpoint(),
                        new EnvironmentConfig().getTSDBAuthProvider()
                )
        );
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean prepAndValidate(final EventAlertConfig config,
                                   final AlertStateStore stateStore) {
        namespace = config.getNamespace();
        alertId = config.getAlertId();
        alertName = config.getAlertName();
        alertTypeString = config.getAlertType().getString();
        threshold = config.getThreshold();
        slidingWindowSec = config.getSlidingWindowSec();
        queryNamespace = config.getQueryNamespace();
        queryFilter = config.getQueryFilter();
        queryTemplate =
                new QueryTemplate(
                        queryNamespace,
                        queryFilter,
                        config.getQueryGroupBy()
                );
        this.statusWriter = new StatusWriter(config, new SnoozeFilter());
        return true;
    }

    @Override
    public AlertEventBag execute(final long endTime,
                                 final TimeUnit timeUnit,
                                 final AlertStateStore stateStore)
            throws AlertException {
        final String request = buildRequest(endTime, timeUnit);
        final String responsePayload = client.getResponse(request, alertId);
        final QueryResponse response = new QueryResponse(responsePayload);
        final List<AlertEvent> alerts = processResponse(response, stateStore);

        return new AlertEventBag(alerts, getAlertConfig());
    }

    private String buildRequest(final long endTime,
                                final TimeUnit timeUnit) {
        final long endTimeSec = TimeUnit.SECONDS.convert(endTime, timeUnit);
        final long startTimeSec = endTimeSec - slidingWindowSec;
        return queryTemplate.evaluate(startTimeSec, endTimeSec);
    }

    private List<AlertEvent> processResponse(
            final QueryResponse response,
            final AlertStateStore stateStore) {
        final Long2BooleanMap observed = new Long2BooleanOpenHashMap();
        final List<AlertEvent> alerts = new ArrayList<>();

        // Process the response data-points first.
        generateAlertsFromResponse(response, stateStore, observed, alerts);

        // For the alerts that were alerting in the previous run, but not
        // updated in this run, assume data-points with value zero.
        generateRecoveryAlerts(stateStore, observed, alerts);

        // Remove alerts that are in a good state. No need to keep them.
        purgeNonAlertingStates(stateStore);

        return alerts.isEmpty() ? Collections.emptyList() : alerts;
    }

    private void generateAlertsFromResponse(final QueryResponse response,
                                            final AlertStateStore stateStore,
                                            final Long2BooleanMap observed,
                                            final List<AlertEvent> alerts) {
        response.forEach(dataPoint -> {
            final TreeMap<String, String> tags = dataPoint.getTags();
            final long alertHash =
                    AlertUtils.getHashForNAMT(namespace, alertId, tags);
            observed.put(alertHash, true);

            stateStore.updateDataPoint(
                    namespace,
                    alertId,
                    tags,
                    Instant.now().getEpochSecond()
            );

            final Optional<EventAlertEvent> maybeAlert =
                    evaluateDataPoint(
                            dataPoint.getHits(),
                            alertHash,
                            tags,
                            dataPoint::getEvent,
                            stateStore
                    );
            maybeAlert.ifPresent(alerts::add);
        });
    }

    private void generateRecoveryAlerts(final AlertStateStore stateStore,
                                        final Long2BooleanMap observed,
                                        final List<AlertEvent> alerts) {
        final LongIterator states = stateStore.getIteratorForStoredData();
        while (states.hasNext()) {
            final long alertHash = states.next();
            // Ignore already processed time-series.
            if (observed.containsKey(alertHash)) {
                continue;
            }

            final Optional<EventAlertEvent> maybeAlert =
                    evaluateDataPoint(
                            0 /* hits */,
                            alertHash,
                            stateStore.getTags(alertHash),
                            Optional::empty,
                            stateStore
                    );
            maybeAlert.ifPresent(alerts::add);
        }
    }

    private Optional<EventAlertEvent> evaluateDataPoint(
            final int hits,
            final long alertHash,
            final SortedMap<String, String> tags,
            final Supplier<Optional<Event>> eventSupplier,
            final AlertStateStore stateStore) {
        // Update state store.
        final AlertState newState = hits >= threshold ?
                AlertState.BAD : AlertState.GOOD;
        final AlertStateChange stageChange =
                stateStore.raiseAlert(
                        namespace,
                        alertId,
                        tags,
                        newState
                );


        AlertUtils.writeStatus(statusWriter,
                alertName,
                alertTypeString,
                Instant.now().getEpochSecond(),
                namespace,
                newState,
                alertId,
                tags,
                AlertUtils.getEventStatusMessage(hits, threshold, newState));

        // Should we create an alert?
        if (!stageChange.raiseAlert()) {
            return Optional.empty();
        }

        // Do not parse the event on recovery. Attempt parsing otherwise.
        final Event event = stageChange.getCurrentState() == AlertState.GOOD ?
                null : eventSupplier.get().orElse(null);

        final EventAlertEvent alert =
                buildEventAlert(
                        hits,
                        alertHash,
                        tags,
                        stageChange.getPreviousState(),
                        stageChange.getCurrentState(),
                        event
                );
        return Optional.of(alert);
    }

    private EventAlertEvent buildEventAlert(
            final int hits,
            final long alertHash,
            final SortedMap<String, String> tags,
            final AlertState previousState,
            final AlertState currentState,
            final Event event) {
        final EventAlertEvent alert = new EventAlertEvent();

        alert.setCount(hits);
        alert.setAlertHash(alertHash);
        alert.setTags(tags);
        alert.setOriginSignal(previousState);
        alert.setCurrentSignal(currentState);
        alert.setEvent(event);

        alert.setNamespace(namespace);
        alert.setAlertId(alertId);
        alert.setDataNamespace(queryNamespace);
        alert.setFilterQuery(queryFilter);
        alert.setThreshold(threshold);
        alert.setWindowSizeSec(slidingWindowSec);

        alert.setNag(false);
        alert.setAlertRaisedTimestamp(Instant.now().getEpochSecond());

        return alert;
    }

    private void purgeNonAlertingStates(AlertStateStore stateStore) {
        final LongIterator states = stateStore.getIteratorForStoredData();
        while (states.hasNext()) {
            final long alertHash = states.next();
            final AlertState state = stateStore.getCurrentState(alertHash);
            if (state == AlertState.GOOD) {
                stateStore.purgeState(alertHash);
                states.remove();
            }
        }
    }
}
