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

package net.opentsdb.horizon.alerts.query.auradb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.Monitoring;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.impl.HealthCheckConfig;
import net.opentsdb.horizon.alerts.config.impl.HealthCheckConfigFields;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.enums.AlertType;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.model.HealthCheckAlertEvent;
import net.opentsdb.horizon.alerts.query.StateTimeBasedExecutor;
import net.opentsdb.horizon.alerts.processor.impl.StatusWriter;
import net.opentsdb.horizon.alerts.query.tsdb.TSDBClient;
import net.opentsdb.horizon.alerts.snooze.SnoozeFilter;
import net.opentsdb.horizon.alerts.state.AlertStateChange;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AuraDBProcessor extends StateTimeBasedExecutor<HealthCheckConfig> {

    private HealthCheckConfig healthCheckConfig;

    private AlertStateStore alertStateStore = null;

    private String queryJson = null;

    private final TSDBClient auraDBClient;

    private String namespace;

    private String namespaceForData;

    private long alertId;

    private EnvironmentConfig environmentConfig = new EnvironmentConfig();

    private StatusWriter statusWriter;

    private final String[] namespaceIdAlertConfigTypeTags;

    public AuraDBProcessor(HealthCheckConfig alertConfig) {
        super(alertConfig);
        auraDBClient = new TSDBClient(environmentConfig.getTsdbEndpoint(), environmentConfig.getTSDBAuthProvider());
        namespaceIdAlertConfigTypeTags = Monitoring.getTagsNamespaceAlertIdConfigType(alertConfig);
    }

    @Override
    public boolean prepAndValidate(HealthCheckConfig alertConfig,
                                   AlertStateStore alertStateStore) throws AlertException {
        final EnvironmentConfig environmentConfig = new EnvironmentConfig();
        this.statusWriter = new StatusWriter(alertConfig, new SnoozeFilter());
        this.healthCheckConfig = alertConfig;

        namespace = healthCheckConfig.getNamespace();
        alertId = healthCheckConfig.getAlertId();
        final JsonNode metaQuery = alertConfig.getQueryJson();
        final JsonNode baseStatusQuery;
        try {
            baseStatusQuery = AlertUtils.parseJsonTree(environmentConfig.getBaseStatusJson());

        } catch (IOException e) {
            throw new AlertException("error loading base status json",e);
        }

        final ObjectNode tsDataSource = (ObjectNode) baseStatusQuery.get("executionGraph").get(0);

        namespaceForData = metaQuery.get("namespace").asText();

        tsDataSource.set("namespace",metaQuery.get("namespace"));


        final ObjectNode statusfilter = (ObjectNode)baseStatusQuery.get("filters").get(0);

        statusfilter.set("filter",metaQuery.get("filter"));

        queryJson = baseStatusQuery.toString();

        log.info("Constructed for alert id: {} {}",alertId,queryJson);

        return true;
    }

    @Override
    public AlertEventBag execute(final long endTime,
                                 final TimeUnit timeUnit,
                                 final AlertStateStore alertStateStore) throws AlertException {
        final Long2LongOpenHashMap seenThisRun = new Long2LongOpenHashMap();
        log.info("id: {} Running status query: {}",alertId,queryJson);
        final String response = auraDBClient.getResponse(queryJson, alertId);
        this.alertStateStore = alertStateStore;
        log.debug("alert id: {} Received aura response: {}",alertId,response);
        final LocalMonitor localMonitor = new LocalMonitor(
                getAlertConfig(), this.namespaceIdAlertConfigTypeTags);
        try {
            final JsonNode root = AlertUtils.parseJsonTree(response);

            final List<AlertEvent> alertEventList = new ArrayList<>();

            final AlertEventBag alertEventBag = new AlertEventBag(alertEventList,healthCheckConfig);
            final JsonNode resultNode = root.get("results").get(0);
            final Iterator<JsonNode> statuses;
            if(resultNode.has("data") && resultNode.get("data").size() != 0) {
                statuses = resultNode.get("data").elements();
            } else {
                return alertEventBag;
            }

                while(statuses.hasNext()) {
                    localMonitor.totalStatuses++;

                    final JsonNode status = statuses.next();

                    final int type = status.get("statusType").asInt();

                    if(type != 0) {
                        //Received some other type than check.
                        continue;
                    }

                    if(!status.hasNonNull("message")) {
                        continue;
                    }

                    final String message = status.get("message").asText();

                    final TreeMap<String,String> tags = new TreeMap<>();
                    final AlertState[] alertStates;
                    final long[] timestamps;

                    final String application = status.get("application").asText();
                    addNsAndApp(tags, namespaceForData, application);
                    final JsonNode tagsNode = status.get("tags");
                    final Iterator<String> tagKeys = tagsNode.fieldNames();
                    while(tagKeys.hasNext()) {
                        final String tagKey = tagKeys.next();
                        tags.put(tagKey, tagsNode.get(tagKey).asText());
                    }
                    final JsonNode statusHistory = status.get("statusCodeArray");
                    final JsonNode statusHistoryTimestamps = status.get("timestampArray");
                    final int statusHistoryLength = statusHistory.size();
                    final int statusHistoryTimestampLength = statusHistoryTimestamps.size();


                    if(statusHistoryLength != statusHistoryTimestampLength) {
                        //issue, skip Status
                        continue;
                    }
                    if(statusHistoryLength == 0) {
                        continue;
                    }
                    alertStates = new AlertState[statusHistoryLength];
                    timestamps = new long[statusHistoryLength];

                    for(int i = 0; i < statusHistoryLength; i++) {
                        alertStates[i] = AlertState.fromId(statusHistory.get(i).asLong());
                        // Ideally should be converting by using time unit.. but perf
                        timestamps[i] = statusHistoryTimestamps.get(i).asLong();
                    }

                    final long lastSeen = timestamps[statusHistoryLength -1];
                    final long hashForNAMT = AlertUtils.getHashForNAMT(namespace, alertId, tags);
                    final long storedLastSeenTime = alertStateStore.getLastSeenTime(hashForNAMT);
                    seenThisRun.put(hashForNAMT, hashForNAMT);
                    log.debug("alert id: {} last seen: str: {} rec: {} , {}",alertId,storedLastSeenTime, lastSeen, tags);
                    log.info("alert id: {} Received datapoints {} in status for {}", alertId,
                            Arrays.toString(alertStates), hashForNAMT);
                    final boolean bypassEvaluation;
                    if(lastSeen <= storedLastSeenTime || storedLastSeenTime == -1) {
                        // Missing alert check
                        if(healthCheckConfig.isMissingEnabled()) {
                            log.info("Evaluating missing: {} , {}",alertId, tags);
                            final AlertEvent alertEvent  = evaluateForMissing(hashForNAMT,
                                        tags, alertStates, timestamps, lastSeen, endTime);

                            if(alertEvent != null) {

                                alertEvent.setAlertDetails(AlertUtils.getMessageForMissing(lastSeen));
                                setProp(HealthCheckConfigFields.MISSING_SINCE,
                                        lastSeen, alertEvent);
                                alertEventList.add(alertEvent);
                                bypassEvaluation = true;
                            } else {
                                bypassEvaluation = false;
                            }
                            localMonitor.evaluatedMissing++;
                        } else {
                            bypassEvaluation = false;
                        }
                        //No update, skip
                        if(storedLastSeenTime != -1) {
                            localMonitor.noUpdate++;
                            continue;
                        } else {
                            localMonitor.firstTime++;
                        }
                    } else {
                        bypassEvaluation = false;
                    }

                    if(!bypassEvaluation) {
                        localMonitor.evaluated++;
                        //Eval
                        AlertEvent alertEvent = null;
                        final String messageToSet;
                        // Check if missing recovery
                        if (healthCheckConfig.isMissingEnabled()) {
                            alertEvent = recoverAlertFromMissing(hashForNAMT, tags, alertStates,
                                    timestamps, false, lastSeen, storedLastSeenTime);
                            localMonitor.evaluatedMissingRecovery++;
                        }

                        if (alertEvent != null) {
                            messageToSet = AlertUtils.getMessageForMissingRecovery(lastSeen);
                            setProp(HealthCheckConfigFields.RECOVERED_SINCE,
                                    lastSeen, alertEvent);
                        } else {
                            messageToSet = message;
                        }

                        if (alertEvent == null && healthCheckConfig.isHasBadThreshold()) {
                            localMonitor.evaluatedBad++;
                            alertEvent = evaluate(AlertState.BAD, hashForNAMT, healthCheckConfig.getBadThreshold(),
                                    tags, alertStates, timestamps);
                        }
                        if (alertEvent == null && healthCheckConfig.isHasWarnThreshold()) {
                            localMonitor.evaluatedWarn++;
                            alertEvent = evaluate(AlertState.WARN, hashForNAMT, healthCheckConfig.getWarnThreshold(),
                                    tags, alertStates, timestamps);
                        }
                        if (alertEvent == null && healthCheckConfig.isHasUnknownThreshold()) {
                            localMonitor.evaluatedUnknown++;
                            alertEvent = evaluate(AlertState.UNKNOWN, hashForNAMT, healthCheckConfig.getUnknownThreshold(),
                                    tags, alertStates, timestamps);
                        }
                        if (alertEvent == null && healthCheckConfig.isHasRecoveryThreshold()) {
                            localMonitor.evaluatedGood++;
                            alertEvent = evaluate(AlertState.GOOD, hashForNAMT, healthCheckConfig.getRecoveryThreshold(),
                                    tags, alertStates, timestamps);
                        }

                        if (alertEvent != null) {
                            alertEvent.setAlertDetails(messageToSet);
                            alertEventList.add(alertEvent);
                        }
                        if (Objects.nonNull(alertStateStore.getCurrentState(hashForNAMT))) {
                            //First can be indeterminate
                            AlertUtils.writeStatus(statusWriter,
                                    healthCheckConfig,
                                    endTime,
                                    healthCheckConfig.getNamespace(),
                                    alertStateStore.getCurrentState(hashForNAMT),
                                    healthCheckConfig.getAlertId(),
                                    tags, messageToSet);
                        }
                    }


                    addNsAndApp(tags,namespaceForData,application);
                    alertStateStore.updateDataPoint(namespace, alertId, tags, lastSeen);

                }

                //Missing
                if(healthCheckConfig.isMissingEnabled()) {
                    final LongIterator iteratorForStoredData = alertStateStore.getIteratorForStoredData();

                    while (iteratorForStoredData.hasNext()) {
                        final long hash = iteratorForStoredData.nextLong();

                        // Look for results which havent come.
                        if(!seenThisRun.containsKey(hash)) {

                            final long lastSeenTime = alertStateStore.getLastSeenTime(hash);
                            final SortedMap<String, String> tags = alertStateStore.getTags(hash);
                            if(tags == null) {

                                log.debug("alert id: {} Tags null for hash: {} with last seen {}", alertId,
                                        hash, lastSeenTime);

                                continue;
                            }

                            AlertState[] alertStates = new AlertState[1];
                            alertStates[0] = AlertState.MISSING;
                            long[] timestamps = new long[1];
                            timestamps[0] = Instant.now().getEpochSecond();
                            final AlertEvent alertEvent =
                                    evaluateForMissing(hash, tags, alertStates,
                                            timestamps, lastSeenTime, endTime);
                            localMonitor.evaluatedMissing++;

                            if(alertEvent != null) {
                                alertEventList.add(alertEvent);
                            }
                        }
                    }
                }

                localMonitor.reportStats();
            //}
            removeNsAndApp(alertEventBag);
            return alertEventBag;



        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void removeNsAndApp(AlertEventBag alertEventBag) {
        final List<AlertEvent> alertEvents = alertEventBag.getAlertEvents();

        for(AlertEvent alertEvent : alertEvents) {
            AlertUtils.removeNsAndAppFromMap(alertEvent.getTags());
        }
    }

    private void addNsAndApp(TreeMap<String, String> tags, String namespaceForData, String application) {
        if(!tags.containsKey(HealthCheckConfigFields.NAMESPACE_FOR_DATA)) {
            tags.put(HealthCheckConfigFields.NAMESPACE_FOR_DATA,namespaceForData);
        }
        if(!tags.containsKey(HealthCheckConfigFields.APPLICATION_FOR_DATA)) {
            tags.put(HealthCheckConfigFields.APPLICATION_FOR_DATA,application);
        }
    }

    private AlertEvent evaluateForMissing(long hash,
                                          SortedMap<String, String> tags,
                                          AlertState[] alertStates,
                                          long[] timestamps,
                                          long storedLastSeenTime,
                                          long endTime) {

        final long currentTimeSecs = Instant.now().getEpochSecond();
        final int intervalForMissing = (int)(currentTimeSecs - storedLastSeenTime);
        if((intervalForMissing) >
                healthCheckConfig.getMissingIntervalInSec()) {

            AlertUtils.writeStatus(statusWriter,
                    healthCheckConfig,
                    endTime,
                    healthCheckConfig.getNamespace(),
                    AlertState.MISSING,
                    healthCheckConfig.getAlertId(),
                    tags, AlertUtils.getMessageForMissing(storedLastSeenTime));

            return createAlertEvent(AlertState.MISSING, hash, tags, alertStates, timestamps, 1,
                    intervalForMissing,false);
        }

        return null;

    }

    private AlertEvent recoverAlertFromMissing(long hash, TreeMap<String, String> tags,
                                               AlertState[] alertStates, long[] timestamps, boolean autoRecovery,
                                               long lastSeen, long storedLastSeenTime) {

        if(alertStateStore.getCurrentState(hash) != AlertState.MISSING) {
            //Recover
            return null;
        }
        final int intervalSinceDataWentMissing;
        if(storedLastSeenTime == -1) {
            intervalSinceDataWentMissing = healthCheckConfig.getMissingIntervalInSec();
        } else {
            intervalSinceDataWentMissing = (int) (lastSeen - storedLastSeenTime);
        }

        return createAlertEvent(AlertState.GOOD, hash, tags, alertStates, timestamps, 1,
                intervalSinceDataWentMissing, autoRecovery);


    }

    private AlertEvent evaluate(AlertState state, long hash,
                                double threshold, TreeMap<String, String> tags,
                                AlertState[] alertStates, long[] timestamps) {

        int size = alertStates.length;

        final int thres;
        if(threshold > size) {
            Monitoring.get().countTSNotEnoughForEval(this.alertId, this.namespace,
                    String.valueOf(AlertType.HEALTH_CHECK), String.valueOf(state));
            return null;
        } else {
            thres = (int)threshold;
        }
        int obs = 0 ;
        for(int i = (size-1); i >= (size - thres) ; i--) {
            if(alertStates[i].equals(state)) {
                obs++;
            }
        }

        if(obs == thres) {
            //yay

            return createAlertEvent(state, hash, tags, alertStates, timestamps,
                    thres, 0, false);

        }

        return null;
    }


    private AlertEvent createAlertEvent(AlertState state, long hash,
                                        SortedMap<String, String> tags,
                                        AlertState[] alertStates,
                                        long[] timestamps,
                                        int thres, int interval,
                                        boolean isAutoRecovery) {

        int size = alertStates.length;

        final AlertStateChange stageChange = alertStateStore.raiseAlert(namespace, alertId, tags, state);

        if (stageChange.raiseAlert()) {
            final long ts;
            if(size != 0) {
                ts = timestamps[size - 1];
            } else {
                ts = Instant.now().getEpochSecond();
            }
            final AlertEvent alertEvent = AlertUtils.createAlertEvent(hash, String.valueOf(ts)
                    , tags, state, healthCheckConfig);
            final String app = tags.get(HealthCheckConfigFields.APPLICATION_FOR_DATA);
            final String ns = tags.get(HealthCheckConfigFields.NAMESPACE_FOR_DATA);

            AlertUtils.removeNsAndAppFromMap(tags);

            annotateAlertEvent((HealthCheckAlertEvent) alertEvent,
                    stageChange,
                    ns,app,alertStates,timestamps,thres, interval,isAutoRecovery);

            return alertEvent;
        }
        return null;
    }

    private void setProp(String property,long lastSeenTime, AlertEvent alertEvent) {
        Map<String, String> additionalProperties = alertEvent.getAdditionalProperties();
        if(additionalProperties == null) {
            additionalProperties = new HashMap<>();
            alertEvent.setAdditionalProperties(additionalProperties);
        }
        additionalProperties.put(property, String.valueOf(lastSeenTime));
    }

    //Convert to builder...
    private void annotateAlertEvent(HealthCheckAlertEvent alertEvent1,
                                    AlertStateChange stageChange,
                                    String namespaceForData,
                                    String application,
                                    AlertState[] alertStates,
                                    long[] timestamps,
                                    int threshold,
                                    int interval,
                                    boolean autoRecovery) {

        alertEvent1.setNag(stageChange.isNag());
        alertEvent1.setOriginSignal(stageChange.getPreviousState());
        alertEvent1.setApplication(application);
        alertEvent1.setNamespaceForData(namespaceForData);
        alertEvent1.setStatuses(alertStates);
        alertEvent1.setTimestamps(timestamps);
        alertEvent1.setIntervalInSeconds(interval);
        alertEvent1.setThreshold(threshold);
        if(stageChange.getPreviousState() == AlertState.MISSING &&
                stageChange.getCurrentState() == AlertState.GOOD) {
            alertEvent1.setMissingRecovery(true);
        } else {
            alertEvent1.setMissingRecovery(false);
        }
    }

    private static class LocalMonitor {

        private final String[] namespaceIdAlertConfigTypeTags;
        private final AlertConfig alertConfig;
        private int totalStatuses = 0;
        private int firstTime = 0;
        private int noUpdate = 0;
        private int evaluated = 0;
        private int evaluatedBad = 0;
        private int evaluatedWarn = 0;
        private int evaluatedGood = 0;
        private int evaluatedUnknown = 0;
        private int evaluatedMissing = 0;
        private int evaluatedMissingRecovery = 0;

        private LocalMonitor(AlertConfig alertConfig,
                             String[] namespaceIdAlertConfigTypeTags) {
            this.alertConfig = alertConfig;
            this.namespaceIdAlertConfigTypeTags = namespaceIdAlertConfigTypeTags;
        }


        public void reportStats() {

            final String namespace = alertConfig.getNamespace();
            final long alertId = alertConfig.getAlertId();

            log.info("alert id: {} Statuses Total: {} first: {} no update: {} evaluated: {} ",
                    alertId, totalStatuses, firstTime, noUpdate, evaluated);

            final Monitoring monitoring = Monitoring.get();

            monitoring.countTimeseriesEvaluated(evaluatedBad, namespace, alertId, AlertState.BAD);
            monitoring.countTimeseriesEvaluated(evaluatedWarn, namespace, alertId, AlertState.WARN);
            monitoring.countTimeseriesEvaluated(evaluatedGood, namespace, alertId, AlertState.GOOD);
            monitoring.countTimeseriesEvaluated(evaluatedUnknown, namespace, alertId, AlertState.UNKNOWN);
            monitoring.countTimeseriesEvaluated(evaluatedMissing, namespace, alertId, AlertState.MISSING);

            monitoring.countTotalStatusesSeen(totalStatuses, namespaceIdAlertConfigTypeTags);
            monitoring.countStatusNotUpdated(noUpdate, namespaceIdAlertConfigTypeTags);
            monitoring.countMissingRecovery(evaluatedMissingRecovery, namespaceIdAlertConfigTypeTags);
        }

    }
}
