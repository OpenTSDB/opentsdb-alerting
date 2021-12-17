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

package net.opentsdb.horizon.alerts.query.tsdb;

import com.fasterxml.jackson.databind.JsonNode;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.Monitoring;
import net.opentsdb.horizon.alerts.config.SuppressMetricConfig;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.enums.ComparatorType;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.model.SingleMetricAlertEvent;
import net.opentsdb.horizon.alerts.model.SummarySingleMetricAlertEvent;
import net.opentsdb.horizon.alerts.processor.Conditional;
import net.opentsdb.horizon.alerts.processor.impl.StatusWriter;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import net.opentsdb.horizon.alerts.query.QueryConstants;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.opentsdb.query.QueryMode;
import net.opentsdb.query.QueryNodeConfig;
import net.opentsdb.query.SemanticQuery;
import net.opentsdb.query.filter.NamedFilter;
import net.opentsdb.query.serdes.SerdesOptions;
import net.opentsdb.utils.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.opentsdb.horizon.alerts.query.QueryConstants.HEARTBEAT_NODE;
import static net.opentsdb.horizon.alerts.query.QueryConstants.HEARTBEAT_THRESHOLD_NODE;
import static net.opentsdb.horizon.alerts.query.tsdb.TSDV3Constants.NumericSummaryType;
import static net.opentsdb.horizon.alerts.query.tsdb.TSDV3Constants.DATA;
import static net.opentsdb.horizon.alerts.query.tsdb.TSDV3Constants.TAGS;
import static net.opentsdb.horizon.alerts.query.tsdb.TSDV3Constants.SOURCE;
import static net.opentsdb.horizon.alerts.query.tsdb.TSDV3Constants.RESULTS;

public class TsdbV3ResultProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TsdbV3ResultProcessor.class);

    private static final Set<String> EMPTY_HEARTBEAT_TAGSET = Collections.unmodifiableSet(new HashSet<>());

    static {
        EMPTY_HEARTBEAT_TAGSET.add(QueryConstants.GROUP_BY_ALL);
    }


    public static String runQueryAndGetResponse(TSDBClient tsdbClient, long alertId,
                                                String namespace,
                                                List<QueryNodeConfig> executionGraph,
                                                List<NamedFilter> filters, SerdesOptions serdes,
                                        long normalizedEndTimeInSecs, long windowSizeInSecs) throws AlertException {

        SemanticQuery query = SemanticQuery.newBuilder()
                .setMode(QueryMode.SINGLE)
                .setStart(String.valueOf(normalizedEndTimeInSecs - windowSizeInSecs))
                .setEnd(String.valueOf(normalizedEndTimeInSecs))
                .setFilters(filters)
                .setExecutionGraph(executionGraph)
                .addSerdesConfig(serdes)
                .build();

        String json = JSON.serializeToString(query);

        LOG.debug("id: {} Running query JSON replace null: {}",alertId,json);
        if(json.contains("\"filterId\":null,")) {
            json = json.replaceAll("\"filterId\":null,","");
        } else if(json.contains(",\"filterId\":null")) {
            json = json.replaceAll(",\"filterId\":null","");
        }
        LOG.info("id: {} Running query JSON after replace null: {}",alertId,json);

        try {
            long start = System.currentTimeMillis();
            final String response = tsdbClient.getResponse(json, alertId);
            long end = System.currentTimeMillis();
            long diff = end - start;

            Monitoring.get().timeTsdQueryRunTime(diff,alertId,namespace);
            return response;
        } catch (AlertException e) {
            LOG.error("Error running tsd query for alertd: {} namespace: {}",alertId,namespace);
            Monitoring.get().countTsdbErrors(1,alertId,namespace);
            throw e;
        }

    }

    public static void updateWithValues(final String response,
                                        final MetricAlertConfig metricAlertConfig,
                                        final String metricSource,
                                        final String metricId,
                                        final AlertEventBag alertEventBag) {


        try {

            final Map<Map<String, String>, AlertEvent> collect = alertEventBag.getAlertEvents()
                    .stream()
                    .collect(Collectors.toMap(AlertEvent::getTags, Function.identity()));

            final Map<String,JsonNode>  nodeMap = getSourceNodes(response);

            final JsonNode metricSourceNode = getStartsWith(nodeMap,metricSource);

            final JsonNode metricIdNode = getStartsWith(nodeMap,metricId);

            final JsonNode metricValuesNode = getMetricValuesNode(metricSourceNode,metricIdNode);


            final Iterator<JsonNode> dataNodes = metricValuesNode.get(DATA).elements();


            final long alertId = metricAlertConfig.getAlertId();

            long[] startEndInterval = AlertUtils.getStartEndAndIntervalFromTimeSpec(metricValuesNode);
            if(startEndInterval == null) {
                return;
            }
            long[] timestampsFromInterval = AlertUtils.getTimestampsFromInterval
                    (startEndInterval[0],startEndInterval[1],startEndInterval[2],0);

            if(timestampsFromInterval.length > 0) {

                while (dataNodes.hasNext()) {
                    final JsonNode next = dataNodes.next();
                    final TreeMap<String, String> tagMap = new TreeMap<>();
                    final List<Double> valueForTheTimeseries = new ArrayList<>();

                    if (next.has(TSDV3Constants.NumericType)) {
                        fillValuesFromDataNode(next, tagMap, valueForTheTimeseries);
                    } else {
                        continue;
                    }

                    if (collect.containsKey(tagMap)) {
                        // Fetch Values
                        final AlertEvent alertEvent = collect.get(tagMap);
                        final long alertHash = alertEvent.getAlertHash();
                        if (alertEvent instanceof SingleMetricAlertEvent) {
                            final SingleMetricAlertEvent singleMetricAlertEvent
                                    = (SingleMetricAlertEvent) alertEvent;

                            final double[] doublesFromCurrent = valueForTheTimeseries.stream().
                                    mapToDouble(Double::doubleValue).toArray();
                            LOG.debug("id: {} alert hash: {} values in window: {}",alertId, alertHash,
                                    Arrays.toString(singleMetricAlertEvent.getValuesInWindow()));
                            if (singleMetricAlertEvent.getValuesInWindow().length == 0) {
                                singleMetricAlertEvent.absorbValues(doublesFromCurrent);

                            } else {
                                final double[] valuesInWindow = singleMetricAlertEvent.getValuesInWindow();
                                LOG.debug("id: {} alert hash: {} doubles current in window: {}",alertId, alertHash,
                                        Arrays.toString(doublesFromCurrent));
                                final double[] baseTimeseries = new double[doublesFromCurrent.length];

                                final int base_length = doublesFromCurrent.length - valuesInWindow.length;

                                System.arraycopy(doublesFromCurrent, 0, baseTimeseries, 0, base_length);

                                System.arraycopy(valuesInWindow, 0, baseTimeseries, base_length, valuesInWindow.length);

                                singleMetricAlertEvent.absorbValues(baseTimeseries);

                                LOG.debug("id: {} alert hash: {} final base in window: {}",alertId, alertHash,
                                        Arrays.toString(baseTimeseries));
                            }
                            //To make sure we generate a compatible list of timestamps
                            timestampsFromInterval = AlertUtils.getTimestampsFromInterval
                                    (startEndInterval[0],startEndInterval[1],startEndInterval[2],
                                            singleMetricAlertEvent.getValuesInWindow().length);

                            singleMetricAlertEvent.setTimestamps(timestampsFromInterval);

                            if(!(singleMetricAlertEvent instanceof SummarySingleMetricAlertEvent)) {
                                final double[] valuesInWindow = singleMetricAlertEvent.getValuesInWindow();
                                final double threshold = singleMetricAlertEvent.getThreshold();
                                final ComparatorType comparator = singleMetricAlertEvent.getComparator();

                                final int breachIndex = getBreachIndex(valuesInWindow, threshold,
                                        comparator.getOperator());

                                singleMetricAlertEvent.setBreachingIndex(breachIndex);

                            }

                        }

                        if (alertEvent instanceof SummarySingleMetricAlertEvent) {


                            final SummarySingleMetricAlertEvent summarySingleMetricAlertEvent =
                                    (SummarySingleMetricAlertEvent) alertEvent;
                            final double[] baseTimeseries = summarySingleMetricAlertEvent.getValuesInWindow();
                            LOG.debug("id: {} alert hash: {} value for alert: {}",alertId, alertHash,
                                    Arrays.toString(summarySingleMetricAlertEvent.getSummaryValues()));
                            LOG.debug("id: {} alert hash: {} values in window tw: {}",alertId, alertHash,
                                    Arrays.toString(baseTimeseries));
                            final double[] summaryValues = AlertUtils.slidingWindow(baseTimeseries,
                                    summarySingleMetricAlertEvent.getSummaryType(),
                                    metricAlertConfig.getSlidingWindowInSecs(),
                                    startEndInterval[2]);
                            if (summarySingleMetricAlertEvent.getSummaryValues().length > 0) {
                                summaryValues[summaryValues.length - 1]
                                        = summarySingleMetricAlertEvent.getSummaryValues()[0];
                            }
                            LOG.debug("id: {} alert hash: {} timestamp values in window tw: {}",alertId, alertHash,
                                    Arrays.toString(timestampsFromInterval));
                            final double[] finalbaseTimeseries = new double[summaryValues.length];
                            final long[] timestamps = new long[summaryValues.length];
                            int diff = baseTimeseries.length - summaryValues.length;
                            System.arraycopy(baseTimeseries, diff, finalbaseTimeseries, 0, summaryValues.length);
                            System.arraycopy(timestampsFromInterval, diff, timestamps, 0, summaryValues.length);
                            LOG.debug("id: {} alert hash: {} summary values in window tw: ",
                                    Arrays.toString(summaryValues), alertHash);
                            LOG.debug("id: {} alert hash: {} final base time values in window tw: ",
                                    Arrays.toString(finalbaseTimeseries), alertHash);
                            LOG.debug("id: {} alert hash: {} timestamp values in window tw: ",
                                    Arrays.toString(timestamps), alertHash);
                            summarySingleMetricAlertEvent.setSummaryValues(summaryValues);
                            summarySingleMetricAlertEvent.absorbValues(finalbaseTimeseries);
                            summarySingleMetricAlertEvent.setTimestamps(timestamps);

                            summarySingleMetricAlertEvent.setBreachingIndex(summaryValues.length - 1);

                            //System.exit(0);
                        }
                        //Summary
                    }
                }
            }

        } catch (IOException | AlertException e) {
            LOG.error("Error parsing result node: ", e);
            Monitoring.get().countPostProcessingError(metricAlertConfig.getAlertId(),
                    metricAlertConfig.getNamespace());
        }

    }

    public static int getBreachIndex(double[] valuesInWindow, double threshold, String operator) {

        final int length = valuesInWindow.length;
        int index = length != 0 ? length -1 : -1;

        for(int i = 0; i < length; i++) {
            final double nextVal = valuesInWindow[i];
            if(nextVal != Double.NaN) {
                if(compare(nextVal, threshold, operator)) {
                    index = i;
                }
            }
        }

        return index;
    }

    private static JsonNode getMetricValuesNode(JsonNode metricSourceNode, JsonNode metricIdNode) {

        if(metricSourceNode != null) {
            return metricSourceNode;
        } else {
            return metricIdNode;
        }

    }


    public static Map<String,JsonNode> getSourceNodes(final String response) throws IOException {

        Map<String,JsonNode> nodes = new HashMap<>();

        JsonNode root = AlertUtils.parseJsonTree(response);
        final Iterator<JsonNode> results = root.get(TSDV3Constants.RESULTS).elements();


        while (results.hasNext()) {

            final JsonNode next = results.next();
            nodes.put(next.get(TSDV3Constants.SOURCE).asText(),next);

        }

        return nodes;

    }


    public static AlertEventBag processForNonSummaries(final String response,
                                                       final StatusWriter statusWriter,
                                                       final MetricAlertConfig metricAlertConfig,
                                                       final String metricSource,
                                                       final String metricId,
                                                       final String metricName,
                                                       final AlertStateStore alertStateStore,
                                                       final Conditional heartbeatConditional) throws AlertException {

        try {
            final List<AlertEvent> events = new ArrayList<>();

            final AlertEventBag alertEventBag = new AlertEventBag(events, metricAlertConfig);

            final Map<String, JsonNode> sourceNodes = getSourceNodes(response);
            final JsonNode badAlertNode = getStartsWith(sourceNodes, QueryConstants.BAD_ALERT_NODE);

            final JsonNode warnAlertNode = getStartsWith(sourceNodes, QueryConstants.WARN_ALERT_NODE);

            final JsonNode recoveryAlertNode = getStartsWith(sourceNodes, QueryConstants.RECOVERY_ALERT_NODE);

            final JsonNode metricSourceNode = getStartsWith(sourceNodes,metricSource);

            final JsonNode metricIdNode = getStartsWith(sourceNodes,metricId);

            final JsonNode metricValuesNode = getMetricValuesNode(metricSourceNode, metricIdNode);

            if(metricValuesNode == null || !metricValuesNode.hasNonNull(DATA)) {
                LOG.info("id: {} metric values object not returned AlertEventBag: {}",metricAlertConfig.getAlertId(),alertEventBag);
                return alertEventBag;
            }

            long temporalThreshold = metricAlertConfig.getTemporalThreshold();

            final Long2LongOpenHashMap temporalThresholdMap;

            final Long2LongOpenHashMap recoveryTemporalThresholdMap;

            final Long2LongOpenHashMap statusToWriteMap = new Long2LongOpenHashMap();

            final Long2LongOpenHashMap statusToExcludeMap = new Long2LongOpenHashMap();

            if(metricAlertConfig.isDeriveTemporalThresholdForAlert()
                    || metricAlertConfig.isDeriveTemporalThresholdForRecovery()) {

                if(metricAlertConfig.isDeriveTemporalThresholdForAlert()) {

                    temporalThresholdMap = getThresholdsMap(metricValuesNode, metricAlertConfig);
                    if(metricAlertConfig.isDeriveTemporalThresholdForRecovery()) {
                        recoveryTemporalThresholdMap = temporalThresholdMap;
                    } else {
                        recoveryTemporalThresholdMap = null;
                    }
                } else {
                    recoveryTemporalThresholdMap = getThresholdsMap(metricValuesNode, metricAlertConfig);
                    temporalThresholdMap = null;
                }

            } else {
                temporalThresholdMap = null;
                recoveryTemporalThresholdMap = null;
            }

            //check for missing and recovery
            final long[] startEndInterval = AlertUtils.
                    getStartEndAndIntervalFromTimeSpec(metricValuesNode);
            LOG.debug("starend: {}, {}",Arrays.toString(startEndInterval), metricValuesNode.toString());

            if(startEndInterval == null) {

                LOG.error("id: {} Received null startEndInterval for : {}",metricAlertConfig.getAlertId(),
                        response);

                return alertEventBag;
            }

            final long[] timestampsFromInterval = AlertUtils.getTimestampsFromInterval
                    (startEndInterval[0], startEndInterval[1], startEndInterval[2],0);

            final long valuesRequiredInWindow;
            if(metricAlertConfig.isRequireFullWindow()) {
                // no evaluation unless there is a full window
                final long slidingWindowInSecs = metricAlertConfig.getSlidingWindowInSecs();
                final int reportingInterval = metricAlertConfig.getReportingInterval();
                if(slidingWindowInSecs < reportingInterval) {
                    LOG.error("alert id: {} slidingwindow {} is less " +
                                    "than reporting interval {}", metricAlertConfig.getAlertId(),
                            slidingWindowInSecs, reportingInterval);
                    return alertEventBag;
                } else if(startEndInterval[2] > reportingInterval){

                    valuesRequiredInWindow = slidingWindowInSecs/startEndInterval[2];

                } else {

                    valuesRequiredInWindow = slidingWindowInSecs/reportingInterval;
                }

            } else {
                valuesRequiredInWindow = -1;
            }

            final double[] nanValues = new double[timestampsFromInterval.length];
            Arrays.fill(nanValues, Double.NaN);
            //This will capture timeseries looked at in this run.
            final Long2LongOpenHashMap timeseries = new Long2LongOpenHashMap();
            if(metricAlertConfig.isMissingEnabled()) {

                events.addAll(checkMissingAndRecovery(metricAlertConfig, statusWriter, statusToExcludeMap,
                        metricValuesNode, alertStateStore,
                        timestampsFromInterval,nanValues,timeseries,startEndInterval, heartbeatConditional));
            } else {
                //Update at least latestNonNanTimestamp
                updateStateWithDatapoints(metricAlertConfig, alertStateStore,
                        metricValuesNode, timestampsFromInterval, nanValues, startEndInterval);
            }

            if(badAlertNode != null) {
                events.addAll(evalNodeResult(badAlertNode,
                        AlertState.BAD,metricAlertConfig,
                        temporalThreshold,
                        alertStateStore,temporalThresholdMap,
                        statusToWriteMap,
                        valuesRequiredInWindow,
                        heartbeatConditional));
            }
            if(warnAlertNode != null) {
                events.addAll(evalNodeResult(warnAlertNode,
                        AlertState.WARN,metricAlertConfig,
                        temporalThreshold,
                        alertStateStore,temporalThresholdMap,
                        statusToWriteMap,
                        valuesRequiredInWindow,
                        heartbeatConditional));

            }

            if(recoveryAlertNode != null) {
                events.addAll(evalNodeResult(recoveryAlertNode,
                        AlertState.GOOD,metricAlertConfig,
                        temporalThreshold,
                        alertStateStore,recoveryTemporalThresholdMap,
                        statusToWriteMap,
                        valuesRequiredInWindow,
                        heartbeatConditional));

            }
            if(metricValuesNode != null) {

                fillValueDetails(metricValuesNode,
                        metricName,
                        events,
                        metricAlertConfig,
                        startEndInterval[1],
                        statusToWriteMap,
                        statusToExcludeMap,
                        statusWriter);
            }

            //Do rest of the missing alerts

            if(metricAlertConfig.isMissingEnabled()) {
                events.addAll(checkMissingFromState(alertStateStore,
                        timeseries, metricAlertConfig, statusWriter, statusToExcludeMap,
                        timestampsFromInterval, nanValues, heartbeatConditional));
            } else if(metricAlertConfig.isAutoRecover()) {
                events.addAll(checkAutoRecoveryFromState(alertStateStore,
                        metricAlertConfig, statusWriter, startEndInterval[1],
                        timestampsFromInterval, nanValues, heartbeatConditional));
            }

            events.forEach(event -> AlertUtils.setMetricName(event, metricName));
            //fillDetails()

            LOG.info("id: {} AlertEventBag: {}",metricAlertConfig.getAlertId(),alertEventBag);
            return alertEventBag;

        } catch (Exception e) {
            LOG.error("id: {} Error evaluating alert, ",metricAlertConfig.getAlertId(),e);
            Monitoring.get().countProcessingNonSummariesError(metricAlertConfig.getAlertId(),
                    metricAlertConfig.getNamespace());
            throw  new AlertException("Error evaluating alert",e);
        }
    }

    private static void updateStateWithDatapoints(MetricAlertConfig metricAlertConfig,
                                                  AlertStateStore alertStateStore,
                                                  JsonNode metricValuesNode,
                                                  final long[] timestampsInput,
                                                  final double[] nanValuesInput,
                                                  final long[] startEndInterval) {

        final Iterator<JsonNode> dataNodes = metricValuesNode.get(DATA).elements();

        while(dataNodes.hasNext()) {

            final JsonNode next = dataNodes.next();
            final TreeMap<String, String> tagMap = new TreeMap<>();
            final List<Double> valueForTheTimeseries = new ArrayList<>();

            final ValueContainer valueContainer = parseNodeForTimeSeries(next, metricAlertConfig, tagMap, valueForTheTimeseries, timestampsInput,
                    nanValuesInput, startEndInterval);

            final long hashForNAMT = AlertUtils.getHashForNAMT(metricAlertConfig.getNamespace(),
                    metricAlertConfig.getAlertId(), tagMap);

            LOG.debug("alert id: {} Updating with data point: {} {} {}" , metricAlertConfig.getAlertId(),
                    hashForNAMT, tagMap, metricAlertConfig.getNamespace());
            updateStateWithDatapoint(valueContainer.latestNonNanTimestamp,alertStateStore,metricAlertConfig,
                    hashForNAMT, tagMap, valueContainer.timestamps);

        }

    }

    private static List<AlertEvent> checkAutoRecoveryFromState(AlertStateStore alertStateStore,
                                                               MetricAlertConfig metricAlertConfig,
                                                               StatusWriter statusWriter,
                                                               long endTimeInSecs,
                                                               long[] timestampsFromInterval,
                                                               double[] nanValues,
                                                               final Conditional heartbeatConditional) {

        final int autoRecoveryInterval = metricAlertConfig.getAutoRecoveryInterval();

        final LongIterator iteratorForStoredData = alertStateStore.getIteratorForStoredData();
        final List<AlertEvent> events = new ArrayList<>();
        while (iteratorForStoredData.hasNext()) {
            final long hash = iteratorForStoredData.nextLong();
            final SortedMap<String, String> tagsMap = alertStateStore.getTags(hash);

            final long lastSeenTime = alertStateStore.getLastSeenTime(hash);
            LOG.debug("alert id: {} auto recovery check for {} {}", metricAlertConfig.getAlertId(),
                    hash, lastSeenTime);
            if (heartbeatConditional.checkCondition(tagsMap)) {
                if ((endTimeInSecs - lastSeenTime) > autoRecoveryInterval) {
                    // We havent seen this in this run
                    final AlertEvent alertEvent;
                    final SortedMap<String, String> tagMap = alertStateStore.getTags(hash);
                    if (tagMap != null && tagMap.size() != 0) {
                        alertEvent = createAutoRecoveryAlertEvent(hash, AlertState.GOOD,
                                timestampsFromInterval, metricAlertConfig,
                                tagMap, nanValues, alertStateStore, lastSeenTime);
                    } else {
                        alertEvent = null;
                        LOG.error("alert id: {} Empty tag map in local state for!! {} {}", metricAlertConfig.getAlertId(),
                                hash, tagMap);
                    }
                    if (alertEvent != null) {

                        AlertUtils.writeStatus(statusWriter,
                                metricAlertConfig,
                                alertEvent.getAlertRaisedTimestamp(),
                                metricAlertConfig.getNamespace(),
                                AlertState.GOOD,
                                metricAlertConfig.getAlertId(),
                                tagMap, alertEvent.getAlertDetails());

                        events.add(alertEvent);
                    }
                }
            } else {
                LOG.info("id: {} (checkAutoRecoveryFromState) heartbeatMap contains val for hash: {} {}",
                        metricAlertConfig.getAlertId(),
                        hash,
                        tagsMap);
            }

        }

        return events;

    }

    private static List<AlertEvent> checkMissingFromState(final AlertStateStore alertStateStore,
                                                          final Long2LongOpenHashMap timeseries,
                                                          final MetricAlertConfig metricAlertConfig,
                                                          final StatusWriter statusWriter,
                                                          final Long2LongMap statusToExcludeMap,
                                                          final long[] timestamps,
                                                          final double[] nanValues,
                                                          final Conditional conditional) {
        final LongIterator iteratorForStoredData = alertStateStore.getIteratorForStoredData();
        final List<AlertEvent> events = new ArrayList<>();
        while (iteratorForStoredData.hasNext()) {
            final long hash = iteratorForStoredData.nextLong();
            final SortedMap<String, String> tagsMap = alertStateStore.getTags(hash);

            if (conditional.checkCondition(tagsMap)) {
                if (!timeseries.containsKey(hash)) {
                    // We havent seen this in this run
                    final AlertEvent alertEvent;
                    final SortedMap<String, String> tagMap = alertStateStore.getTags(hash);
                    if (tagMap != null && tagMap.size() != 0) {
                        alertEvent = createMissingAlertEvent(hash, AlertState.MISSING,
                                timestamps, metricAlertConfig, statusWriter, statusToExcludeMap,
                                tagMap, nanValues, alertStateStore, alertStateStore.getLastSeenTime(hash));

                    } else {
                        alertEvent = null;
                        LOG.error("alert id: {} Empty tag map!! {} {}", metricAlertConfig.getAlertId(), hash);
                    }
                    if (alertEvent != null) {
                        events.add(alertEvent);
                    }
                }
            } else {
                LOG.info("id: {} heartbeatMap (checkMissingFromState) contains val for hash: {} {}",
                        metricAlertConfig.getAlertId(),
                        hash,
                        tagsMap);
            }
        }

        return events;


    }

    private static List<AlertEvent> checkMissingAndRecovery(final MetricAlertConfig metricAlertConfig,
                                                            final StatusWriter statusWriter,
                                                            final Long2LongMap statusToExcludeMap,
                                                            final JsonNode metricValuesNode,
                                                            final AlertStateStore alertStateStore,
                                                            final long[] timestampsInput,
                                                            final double[] nanValuesInput,
                                                            final Long2LongOpenHashMap timeseriesEvaluated,
                                                            long[] startEndInterval,
                                                            final Conditional heartbeatConditional) {

        final Iterator<JsonNode> dataNodes = metricValuesNode.get(DATA).elements();
        final List<AlertEvent> alertEvents = new ArrayList<>();
        while(dataNodes.hasNext()) {

            final JsonNode next = dataNodes.next();
            final TreeMap<String,String> tagMap = new TreeMap<>();
            final List<Double> valueForTheTimeseries = new ArrayList<>();

            ValueContainer valueContainer = parseNodeForTimeSeries(next, metricAlertConfig, tagMap,
                    valueForTheTimeseries,
                    timestampsInput, nanValuesInput, startEndInterval);
            final long latestNonNanTimestamp = valueContainer.latestNonNanTimestamp;
            final long[] timestamps = valueContainer.timestamps;
            final double[] nanValues = valueContainer.nanValues;

            final long hashForNAMT = AlertUtils.getHashForNAMT(metricAlertConfig.getNamespace(),
                    metricAlertConfig.getAlertId(), tagMap);

            final AlertEvent alertEvent;
            //Check missing
            if (heartbeatConditional.checkCondition(tagMap)) {
                if (latestNonNanTimestamp == -1) {

                    final long lastSeenTime;

                    if (alertStateStore.getLastSeenTime(hashForNAMT) != -1) {
                        lastSeenTime = alertStateStore.getLastSeenTime(hashForNAMT);
                    } else {
                        lastSeenTime = timestamps[0];
                    }
                    alertEvent = createMissingAlertEvent(hashForNAMT, AlertState.MISSING,
                            timestamps, metricAlertConfig, statusWriter, statusToExcludeMap,
                            tagMap, nanValues,
                            alertStateStore,
                            lastSeenTime);

                } else if (latestNonNanTimestamp != -1 &&
                        alertStateStore.getCurrentState(hashForNAMT) == AlertState.MISSING) {

                    //check missing recovery
                    alertEvent = createMissingAlertEvent(hashForNAMT, AlertState.GOOD,
                            timestamps, metricAlertConfig, statusWriter, statusToExcludeMap,
                            tagMap,
                            valueForTheTimeseries.stream().mapToDouble(Double::doubleValue).toArray(),
                            alertStateStore,
                            latestNonNanTimestamp);

                } else {
                    alertEvent = null;
                }
                if (alertEvent != null) {
                    alertEvents.add(alertEvent);
                }
            } else {
                LOG.info("id: {} heartbeatMap (checkMissingAndRecovery) contains val for hash: {} {}", metricAlertConfig.getAlertId(), hashForNAMT, tagMap);
            }

            updateStateWithDatapoint(latestNonNanTimestamp, alertStateStore, metricAlertConfig, hashForNAMT,tagMap, timestamps);

            timeseriesEvaluated.put(hashForNAMT,hashForNAMT);

        }

        return alertEvents;
    }

    private static class ValueContainer {

        private final long latestNonNanTimestamp;
        private final long[] timestamps;
        private final double[] nanValues;

        private ValueContainer(long latestNonNanTimestamp, long[] timestamps, double[] nanValues) {
            this.latestNonNanTimestamp = latestNonNanTimestamp;
            this.timestamps = timestamps;
            this.nanValues = nanValues;
        }
    }

    private static ValueContainer parseNodeForTimeSeries(JsonNode next,
                                                 MetricAlertConfig metricAlertConfig,
                                                 TreeMap<String,String> tagMap,
                                                 List<Double> valueForTheTimeseries,
                                                  long[] timestampsInput,
                                                   double[] nanValuesInput,
                                                  long[] startEndInterval) {

        fillValuesFromDataNode(next,tagMap,valueForTheTimeseries);
        
        final long[] timestamps;
        final double[] nanValues;

        //Get first non nan timestamp
        int index = valueForTheTimeseries.size();

        if(index == timestampsInput.length) {
            //because this is a thing
            timestamps = timestampsInput;
            nanValues = nanValuesInput;
        } else {
            timestamps = AlertUtils.getTimestampsFromInterval(startEndInterval[0],
                    startEndInterval[1],startEndInterval[2],valueForTheTimeseries.size());
            nanValues = new double[timestamps.length];
            Arrays.fill(nanValues,Double.NaN);
        }

        final long latestNonNanTimestamp = getLatestNonNanTimestamp(valueForTheTimeseries, timestamps);

        LOG.debug("missing Timestamps: {} {}" ,Arrays.toString(timestamps),Arrays.toString(nanValues));
        return new ValueContainer(latestNonNanTimestamp,timestamps, nanValues);
    }

    private static long getLatestNonNanTimestamp(List<Double> valueForTheTimeseries, long[] timestamps) {
        int index = valueForTheTimeseries.size();

        double val = Double.NaN;
        while(Double.isNaN(val) && (index > -1)) {
            //LOG.info("In loop: index: {}", index);
            index--;
            if(index == -1) {
                break;
            }
            val = valueForTheTimeseries.get(index);
        }

        final long latestNonNanTimestamp;
        if(index != -1) {
            latestNonNanTimestamp = timestamps[index];
        } else {
            latestNonNanTimestamp = -1;
        }

        return latestNonNanTimestamp;

    }

    private static void updateStateWithDatapoint(final long latestNonNanTimestamp,
                                                 final AlertStateStore alertStateStore,
                                                 final MetricAlertConfig metricAlertConfig,
                                                 final long hashForNAMT,
                                                 final SortedMap<String, String> tagMap,
                                                 final long[] timestamps) {

        LOG.info("alert id: {} Updating with data point: {} latest: {} storeIdentity: {}" , metricAlertConfig.getAlertId(),
                hashForNAMT, latestNonNanTimestamp, alertStateStore.getStoreAlertIdentity());

        //update state with last seen
        if(latestNonNanTimestamp == -1) {
            if(alertStateStore.getLastSeenTime(hashForNAMT) == -1) {
                alertStateStore.updateDataPoint(
                        metricAlertConfig.getNamespace(),
                        metricAlertConfig.getAlertId(), tagMap, timestamps[0]);
            }
        } else {
            alertStateStore.updateDataPoint(
                    metricAlertConfig.getNamespace(),
                    metricAlertConfig.getAlertId(), tagMap, latestNonNanTimestamp);
        }
    }


    private static AlertEvent createAutoRecoveryAlertEvent(final long hashForNAMT,
                                                           final AlertState alertState,
                                                           final long[] timestamps,
                                                           final MetricAlertConfig metricAlertConfig,
                                                           final SortedMap<String, String> tagMap,
                                                           final double[] values,
                                                           final AlertStateStore alertStateStore,
                                                           final long lastSeenTime) {

        final AlertEvent alertEvent = AlertUtils.createAlertEvent(hashForNAMT, metricAlertConfig , alertState,
                String.valueOf(timestamps[timestamps.length - 1]),alertStateStore, tagMap);

        if(alertEvent != null) {
            if (alertEvent instanceof SummarySingleMetricAlertEvent) {
                ((SummarySingleMetricAlertEvent) alertEvent).setTimestamps(timestamps);
                ((SummarySingleMetricAlertEvent) alertEvent).setSummaryValues(values);
            } else {
                ((SingleMetricAlertEvent) alertEvent).setTimestamps(timestamps);
                ((SingleMetricAlertEvent) alertEvent).setValuesInWindow(values);
            }
            alertEvent.setAlertDetails(AlertUtils.
                    getMessageForAutoRecovery(alertEvent.getOriginSignal(), lastSeenTime));
        }


        return alertEvent;
    }

    private static AlertEvent createMissingAlertEvent(final long hashForNAMT,
                                                      final AlertState alertState,
                                                      final long[] timestamps,
                                                      final MetricAlertConfig metricAlertConfig,
                                                      final StatusWriter statusWriter,
                                                      final Long2LongMap statusToExcludeMap,
                                                      final SortedMap<String, String> tagMap,
                                                      final double[] values,
                                                      final AlertStateStore alertStateStore,
                                                      final long lastSeenTime) {

        final AlertEvent alertEvent = AlertUtils.createAlertEvent(hashForNAMT, metricAlertConfig , alertState,
                String.valueOf(timestamps[timestamps.length - 1]),alertStateStore, tagMap);

        final String status_msg;

        if(alertState == AlertState.MISSING) {
            status_msg = AlertUtils.getMessageForMissing(lastSeenTime);
        } else if(alertState == AlertState.GOOD){
            status_msg = AlertUtils.getMessageForMissingRecovery(lastSeenTime);
        } else {
            //Should not happen
            status_msg = null;
        }

        // Set values
        if(alertEvent != null) {
            if(alertEvent instanceof SummarySingleMetricAlertEvent) {
                ((SummarySingleMetricAlertEvent) alertEvent).setTimestamps(timestamps);
                ((SummarySingleMetricAlertEvent) alertEvent).setSummaryValues(values);
            } else {
                ((SingleMetricAlertEvent) alertEvent).setTimestamps(timestamps);
                ((SingleMetricAlertEvent) alertEvent).setValuesInWindow(values);
            }
            alertEvent.setAlertDetails(status_msg);

        }

        //Report missing state to auradb
        if(!statusToExcludeMap.containsKey(hashForNAMT)) {
            AlertUtils.writeStatus(statusWriter,
                    metricAlertConfig,
                    timestamps[timestamps.length - 1],
                    metricAlertConfig.getNamespace(),
                    alertState,
                    metricAlertConfig.getAlertId(),
                    tagMap, status_msg);
            statusToExcludeMap.put(hashForNAMT,alertState.getId());
        }

        return alertEvent;
    }

    private static JsonNode getStartsWith(Map<String, JsonNode> sourceNodes, String requested) {

        for (String key : sourceNodes.keySet()) {
            if(key.startsWith(requested)) {
                return sourceNodes.get(key);
            }
        }

        return null;
    }

    private static Long2LongOpenHashMap getThresholdsMap(JsonNode metricValuesNode, MetricAlertConfig metricAlertConfig) {
        final Iterator<JsonNode> dataNodes = metricValuesNode.get(DATA).elements();
        final Long2LongOpenHashMap long2LongOpenHashMap;

        if(dataNodes.hasNext()) {
            long2LongOpenHashMap = new Long2LongOpenHashMap();
        } else {
            return null;
        }

        while(dataNodes.hasNext()) {
            final JsonNode next = dataNodes.next();
            final TreeMap<String, String> tagMap = new TreeMap<>();
            final List<Double> valueForTheTimeseries = new ArrayList<>();
            if(next.has(TSDV3Constants.NumericSummaryType)) {
                fillValuesAndTagsForSummaryType(next,tagMap,valueForTheTimeseries, true);
            } else if(next.has(TSDV3Constants.NumericType)) {
                //exclude nans when counting
                fillValuesAndTagsForNonSummaryType(next,tagMap,valueForTheTimeseries, true);
            }

            final long hashForNAMT = AlertUtils.getHashForNAMT(metricAlertConfig.getNamespace(),
                    metricAlertConfig.getAlertId(), tagMap);
            if(valueForTheTimeseries.size() < 20) {
                LOG.info("id: {} Received in temporal values: {} for {}", metricAlertConfig.getAlertId()
                        , valueForTheTimeseries, hashForNAMT);
            } else {
                LOG.info("id: {} Received in temporal values: {} for {} ", metricAlertConfig.getAlertId()
                        , valueForTheTimeseries.size(), hashForNAMT);
            }
            long2LongOpenHashMap.put(hashForNAMT,valueForTheTimeseries.size());
        }
        return long2LongOpenHashMap;
    }

    /**
     * TODO: common elements between this and fillDetails
     * @param dataNode
     * @param alertState
     * @param metricAlertConfig
     * @param temporalThreshold
     * @param alertStateStore
     * @return
     */
    private static List<AlertEvent> evalNodeResult(final JsonNode dataNode,
                                                   final AlertState alertState,
                                                   final MetricAlertConfig metricAlertConfig,
                                                   final long temporalThreshold,
                                                   final AlertStateStore alertStateStore,
                                                   final Long2LongMap temporalThresholdMap,
                                                   final Long2LongMap statusesToWriteMap,
                                                   final long valuesRequiredInWindow,
                                                   final Conditional heartbeatConditional) {
        final String namespace = metricAlertConfig.getNamespace();
        final long alertId = metricAlertConfig.getAlertId();
        LOG.debug("Eval : "+ alertId + " type: "+ alertState.name());

        final Iterator<JsonNode> dataNodes = dataNode.get(DATA).elements();
        final List<AlertEvent> alertEvents = new ArrayList<>();
        long count = 0;
        long countOfBreached = 0;
        long alertsRaised = 0;
        while(dataNodes.hasNext()) {
            count++;
            final JsonNode next = dataNodes.next();

            final JsonNode valueNode = next.get(TSDV3Constants.NumericSummaryType).get(DATA).get(0);

            final String tsField = valueNode.fieldNames().next();

            final long  tVal = valueNode.get(tsField).get(0).asLong(0);

            final JsonNode tagsNode = next.get(TSDV3Constants.TAGS);
            //AlertEvent event = new AlertEvent();
            TreeMap<String,String> tagMap = new TreeMap<>();
            tagsNode.fieldNames().forEachRemaining(key -> tagMap.put(key,tagsNode.get(key).asText()));
            if(tagMap.isEmpty()) {
                //Group by all.
                tagMap.put(QueryConstants.GROUP_BY_ALL, QueryConstants.GROUP_BY_ALL);
            }
            final long hashForNAMT = AlertUtils.getHashForNAMT(namespace,alertId,tagMap);

            //require full window

            if(valuesRequiredInWindow > 0) {
                final long valuesInWindow = temporalThresholdMap.get(hashForNAMT);
                if(valuesInWindow != 0) {
                    if(valuesRequiredInWindow > valuesInWindow) {
                        //Not enough values in window for
                        LOG.info("id: {} Skipping, as not " +
                                "enough values in window required: {} found: {} for {} state to evaluate: {}", alertId, valuesRequiredInWindow,
                                valuesInWindow, hashForNAMT, alertState.name());
                        continue;
                    }
                } else {
                    //Should not happen
                    LOG.info("In should not happen");
                    LOG.info("id: {} Received no values in window {} required: {} for {} state to evaluate: {}", alertId, valuesInWindow,
                            valuesRequiredInWindow, hashForNAMT, alertState.name());
                }

            }

            final long thre;

            if(temporalThresholdMap != null) {
                boolean changedBecauseZero = false;
                if(temporalThresholdMap.containsKey(hashForNAMT)) {
                    thre = temporalThresholdMap.get(hashForNAMT) == 0 ? 1: temporalThresholdMap.get(hashForNAMT);
                    if(temporalThresholdMap.get(hashForNAMT) == 0) {
                        changedBecauseZero = true;
                    }
                } else {
                    thre = temporalThreshold;
                }
                LOG.info("id: {} state to evaluate: {} for: {} Expected num of values to breach {}, actual number of breaching values is {}",
                         alertId, alertState.name(), hashForNAMT,
                        thre, tVal);
            } else {
                LOG.info("id: {} state to evaluate: {} for: {} (not dynamic) Expected num of values to breach {}, actual number of breaching values is {}  ",alertId, alertState.name(), 
                         hashForNAMT,
                        temporalThreshold,tVal);
                thre = temporalThreshold;
            }
            //Create alert
            if (tVal >= thre) {
                if (heartbeatConditional.checkCondition(tagMap)) {
                    countOfBreached++;
                    AlertEvent alertEvent = AlertUtils.createAlertEvent(hashForNAMT, metricAlertConfig,
                            alertState, tsField, alertStateStore, tagMap);
                    if (!statusesToWriteMap.containsKey(hashForNAMT)) {
                        // Do not duplicate
                        statusesToWriteMap.put(hashForNAMT, alertState.getId());
                    }

                    if (alertEvent != null) {
                        alertsRaised++;
                        alertEvents.add(alertEvent);
                    }
                } else {
                    LOG.info("id: {} heartbeatMap contains val for hash: {} {}", metricAlertConfig.getAlertId(), hashForNAMT, tagMap);
                }
            }

        }

        reportAlertStats(count, countOfBreached,namespace, alertId, alertState);

        return alertEvents;
    }

    private static void evalHeartbeat(final JsonNode dataNode,
                                      final MetricAlertConfig metricAlertConfig,
                                      final long temporalThreshold,
                                      final Long2LongMap temporalThresholdMap,
                                      final Long2BooleanMap heartbeatResultMap,
                                      final long valuesRequiredInWindow,
                                      long[] startEndInterval) {
        final String namespace = metricAlertConfig.getNamespace();
        final long alertId = metricAlertConfig.getAlertId();
        LOG.debug("Eval heartbeat: " + alertId);

        final Iterator<JsonNode> dataNodes = dataNode.get(DATA).elements();
        while (dataNodes.hasNext()) {
            final JsonNode next = dataNodes.next();

            final TreeMap<String,String> tagMap = new TreeMap<>();
            final List<Double> valueForTheTimeseries = new ArrayList<>();

            long[] timestamps = AlertUtils.getTimestampsFromInterval(startEndInterval[0],
                    startEndInterval[1],
                    startEndInterval[2],
                    0);
            fillValuesFromDataNode(next, tagMap, valueForTheTimeseries);

            final double[] valuesInCurrentWindow = valueForTheTimeseries.stream().
                    mapToDouble(Double::doubleValue).toArray();

            final JsonNode valueNode = next.get(NumericSummaryType).get(DATA).get(0);

            final String tsField = valueNode.fieldNames().next();

            final JsonNode tagsNode = next.get(TAGS);

            final long tVal = valueNode.get(tsField).get(0).asLong(0);

            final TreeMap<String, String> tagsMap = new TreeMap<>();
            tagsNode.fieldNames().forEachRemaining(key -> tagsMap.put(key, tagsNode.get(key).asText()));

            if (tagsMap.isEmpty()) {
                //Group by all.
                tagsMap.put(QueryConstants.GROUP_BY_ALL, QueryConstants.GROUP_BY_ALL);
            }
            final long hashForNAMT = AlertUtils.getHashForNAMT(namespace, alertId, tagsMap);
            heartbeatResultMap.put(hashForNAMT, false);

            //require full window for heartbeat
            if (valuesRequiredInWindow > 0) {
                final long valuesInWindow = temporalThresholdMap.get(hashForNAMT);
                if (valuesInWindow != 0) {
                    if (valuesRequiredInWindow > valuesInWindow) {
                        //Not enough values in window for
                        LOG.info("id: {} Skipping heartbeat values, as not " +
                                        "enough values in window required: {} found: {} for {} state to evaluate", alertId, valuesRequiredInWindow,
                                valuesInWindow, hashForNAMT);
                        continue;
                    }
                } else {
                    //Should not happen
                    LOG.info("Warn id: {} Received no values in heartbeat window {} required: {} for {} state to evaluate", alertId, valuesInWindow,
                            valuesRequiredInWindow, hashForNAMT);
                }
            }

            final long thre;
            if (temporalThresholdMap != null) {
                if (temporalThresholdMap.containsKey(hashForNAMT)) {
                    thre = temporalThresholdMap.get(hashForNAMT) == 0 ? 1 : temporalThresholdMap.get(hashForNAMT);
                } else {
                    thre = temporalThreshold;
                }
                LOG.info("id: {} hearbeatfor: {} Expected num of values to breach {}, actual number of breaching values is {}",
                        alertId, hashForNAMT, thre, tVal);
            } else {
                LOG.info("id: {} hearbeat to evaluate for: {} (not dynamic) Expected num of values to breach {}, actual number of breaching values is {}  ", alertId,
                        hashForNAMT,
                        temporalThreshold, tVal);
                thre = temporalThreshold;
            }
            if (tVal >= thre) {
                LOG.info("Suppressing heartbeat metric id: {} with suppress config: {} with timestamps: {} values in window: {} tval: {} thre: {}",
                        alertId,
                        metricAlertConfig.getSuppressMetricConfig().toString(),
                        timestamps,
                        valuesInCurrentWindow,
                        tVal,
                        thre);
                heartbeatResultMap.put(hashForNAMT, true);
            }
        }
    }

    /**
     * TODO: Common elements between this and evalNodeResult
     * @param metricValuesNode
     * @param events
     * @param metricAlertConfig
     * @return
     */
    private static List<AlertEvent> fillValueDetails(final JsonNode metricValuesNode,
                                                     final String metricName,
                                                     final List<AlertEvent> events,
                                                     final MetricAlertConfig metricAlertConfig,
                                                     final long endTimeInSecs,
                                                     final Long2LongMap statusesToWrite,
                                                     final Long2LongMap statusesToExclude,
                                                     final StatusWriter statusWriter) throws AlertException {

        final Map<Long, AlertEvent> collect = events.stream()
                .collect(Collectors.toMap(e -> e.getAlertHash(), Function.identity()));
        final Iterator<JsonNode> dataNodes = metricValuesNode.get(DATA).elements();

        final String namespace = metricAlertConfig.getNamespace();

        final long alertId = metricAlertConfig.getAlertId();

        while(dataNodes.hasNext()) {
            final JsonNode next = dataNodes.next();
            final TreeMap<String,String> tagMap = new TreeMap<>();
            final List<Double> valueForTheTimeseries = new ArrayList<>();

            fillValuesFromDataNode(next,tagMap,valueForTheTimeseries);

            final long hashForNAMT = AlertUtils.getHashForNAMT(namespace,
                    alertId, tagMap);
            if(collect.containsKey(hashForNAMT)) {
                final AlertState eventType = collect.get(hashForNAMT).getSignal();
                final String details = getDetailsString(metricName,eventType,
                        valueForTheTimeseries,metricAlertConfig).trim();
                final AlertEvent alertEvent = collect.get(hashForNAMT);
                alertEvent.setAlertDetails(details);
                if(alertEvent.getClass()
                        == SingleMetricAlertEvent.class) {
                    long[] startEndInterval = AlertUtils.
                            getStartEndAndIntervalFromTimeSpec(metricValuesNode);
                    if(startEndInterval != null) {
                        final long[] timestampsFromInterval = AlertUtils.getTimestampsFromInterval
                                (startEndInterval[0], startEndInterval[1], startEndInterval[2],valueForTheTimeseries.size());
                        if (timestampsFromInterval.length == valueForTheTimeseries.size()) {
                            ((SingleMetricAlertEvent) alertEvent).
                                    setTimestamps(timestampsFromInterval);
                            ((SingleMetricAlertEvent) alertEvent).
                                    setValuesInWindow(valueForTheTimeseries.stream()
                                            .mapToDouble(Double::doubleValue).toArray());
                        }
                    }
                }
            }

            //Write status to auradb
            if(statusesToWrite.containsKey(hashForNAMT) &&
                    !statusesToExclude.containsKey(hashForNAMT) ) {

                final AlertState state = AlertState.fromId(statusesToWrite.get(hashForNAMT));
                final String status_message = getDetailsString(metricName, state,
                            valueForTheTimeseries, metricAlertConfig).trim();
                    AlertUtils.writeStatus(statusWriter,
                            metricAlertConfig,
                            endTimeInSecs,
                            namespace,
                            state,
                            alertId,
                            tagMap,
                            status_message);
                    statusesToExclude.put(hashForNAMT, 1l);
            }

        }
        return events;

    }

    private static void fillValuesFromDataNode(JsonNode next, TreeMap<String,String> tagMap, List<Double> valueForTheTimeseries) {
        if(next.has(TSDV3Constants.NumericSummaryType)) {
            fillValuesAndTagsForSummaryType(next,tagMap,valueForTheTimeseries);
        } else if(next.has(TSDV3Constants.NumericType)) {
            fillValuesAndTagsForNonSummaryType(next,tagMap,valueForTheTimeseries);
        }
    }

    private static void fillValuesAndTagsForNonSummaryType(final JsonNode next,
                                                           final TreeMap<String, String> tagMap,
                                                           final List<Double> valueForTheTimeseries) {
        fillValuesAndTagsForNonSummaryType(next, tagMap, valueForTheTimeseries, false);
    }

    /**
     * Generally this method is called in conjunction with time.
     * So, nans should never be exculded in those cases.
     * Nans should only be excluded if temporal threshold calc happening
     * @param next
     * @param tagMap
     * @param valueForTheTimeseries
     * @param excludeNans
     */
    private static void fillValuesAndTagsForNonSummaryType(final JsonNode next,
                                                           final TreeMap<String, String> tagMap,
                                                           final List<Double> valueForTheTimeseries,
                                                           final boolean excludeNans) {
        final Iterator<JsonNode> elements = next.get(TSDV3Constants.NumericType).elements();
        while (elements.hasNext()){
            final double v = elements.next().asDouble();
            if(!excludeNans) {
                valueForTheTimeseries.add(v);
            } else {
                if(!Double.isNaN(v)) {
                    valueForTheTimeseries.add(v);
                }
            }
        }

        final JsonNode tagsNode = next.get(TSDV3Constants.TAGS);
        //AlertEvent event = new AlertEvent();
        tagsNode.fieldNames().forEachRemaining(key -> tagMap.put(key,tagsNode.get(key).asText()));
        if(tagMap.isEmpty()) {
            //Group by all.
            tagMap.put(QueryConstants.GROUP_BY_ALL, QueryConstants.GROUP_BY_ALL);
        }
    }

    private static String getDetailsString(final String metricName,
                                           final AlertState eventType,
                                           final List<Double> valueForTheTimeseries,
                                           final MetricAlertConfig metricAlertConfig) {


        switch (eventType) {
            case WARN:

                return getThresholdDetails(metricName,metricAlertConfig.getWarnThreshold(),
                        metricAlertConfig.getComparisonOperator(), valueForTheTimeseries,
                        metricAlertConfig.getSamplerStringForMessage());
            case BAD:

                return getThresholdDetails(metricName,metricAlertConfig.getBadThreshold(),
                        metricAlertConfig.getComparisonOperator(),valueForTheTimeseries,
                        metricAlertConfig.getSamplerStringForMessage());
            case GOOD:
                return getThresholdDetails(metricName,metricAlertConfig.getRecoveryThreshold(),
                        metricAlertConfig.getFlippedComparisionOperator(),valueForTheTimeseries,
                        metricAlertConfig.getFlippedSamplerStringForMessage());

            case MISSING:
                return "NO DATA";

            default:

                throw new AssertionError("Unsupported event type");
        }


    }

    private static String getThresholdDetails(final String metricName,
                                              final double threshold,
                                              final String comparator,
                                              final List<Double> valueForTheTimeseries, String sampler) {

        final Iterator<Double> iterator = valueForTheTimeseries.iterator();
        double finalValue = iterator.next();
        while (iterator.hasNext()) {
            final Double next = iterator.next();
            if(compare(next,threshold,comparator)) {
                finalValue = next;
            }
        }
        String strToFormat = AlertUtils.getWordFromComparator(comparator);


        return String.format(QueryConstants.ALERT_OUTPUT_STRING,
                metricName,
                AlertUtils.stripTrailingZeros(AlertUtils.soothMetricValue(finalValue)),
                strToFormat,
                threshold,
                sampler);

    }

    private static boolean compare(double next, double threshold, String comparison_operator) {

        switch (comparison_operator.trim()) {
            case "<":
                return next < threshold;
            case ">":
                return next > threshold;
            case "<=":
                return next <= threshold;
            case ">=":
                return next >= threshold;
            default:
                throw new AssertionError("Wrong comparator");
        }
    }

    public static AlertEventBag processForSummaries(final String response,
                                                    final StatusWriter statusWriter,
                                                    final MetricAlertConfig metricAlertConfig,
                                                    final String metricSource,
                                                    final String metricId,
                                                    final String metricName,
                                                    final AlertStateStore alertStateStore,
                                                    final long normalizedEndTimeInSecs,
                                                    final long sliding_window_in_secs,
                                                    final Conditional heartbeatSuppressConditional) throws AlertException {

        try {
            List<AlertEvent> alertEvents = new ArrayList<>();
            String namespace = metricAlertConfig.getNamespace();
            long alertId = metricAlertConfig.getAlertId();
            final JsonNode root = AlertUtils.parseJsonTree(response);

            JsonNode summaryNode = null;

            final Iterator<JsonNode> results = root.get(TSDV3Constants.RESULTS).elements();

            while (results.hasNext()) {

                final JsonNode next = results.next();

                if (next.get(TSDV3Constants.SOURCE).asText().startsWith(metricSource)) {
                    summaryNode = next;
                }

            }
            final long startTime = normalizedEndTimeInSecs - sliding_window_in_secs;

            //final long[] timestampsFromInterval = AlertUtils.getTimestampsFromInterval
              //      (startEndInterval[0], startEndInterval[1], startEndInterval[2],0);

            //This will capture timeseries looked at in this run.
            final Long2LongOpenHashMap timeseries = new Long2LongOpenHashMap();

            if (summaryNode == null) {
                throw new AlertException("Unable to get the summary node: " + response);
            }
            final Long2LongOpenHashMap statusWriterState = new Long2LongOpenHashMap();
            final Iterator<JsonNode> dataNodes = summaryNode.get(DATA).elements();
            while (dataNodes.hasNext()) {
                final JsonNode next = dataNodes.next();
                final TreeMap<String, String> tagMap = new TreeMap<>();
                final List<Double> valueForTheTimeseries = new ArrayList<>();

                if (next.has(TSDV3Constants.NumericSummaryType)) {
                    fillValuesAndTagsForSummaryType(next, tagMap, valueForTheTimeseries);
                }
                // Should always be one or zero.
                //Assert.assertTrue(valueForTheTimeseries.size() <= 1);
                if (valueForTheTimeseries.size() > 1) {
                    LOG.error("Summary has more than one value for {} in response {}", tagMap.toString(), response);
                    continue;
                }
                final JsonNode valueNode = next.get(TSDV3Constants.NumericSummaryType).get(DATA).get(0);

                final String tsField = valueNode.fieldNames().next();
                final long timestamp = Long.parseLong(tsField) + metricAlertConfig.getSlidingWindowInSecs();

                final double tVal = valueForTheTimeseries.get(0);
                final long hashForNAMT = AlertUtils.getHashForNAMT(namespace,alertId,tagMap);
                boolean shouldSuppress = false;
                if (!heartbeatSuppressConditional.checkCondition(tagMap)) {
                    LOG.info("id: {} heartbeatMap (processForSummaries) contains val for hash: {} {}", alertId, hashForNAMT, tagMap);
                    shouldSuppress = true;
                }
                LOG.info("id: {} Received tVal: {} for hash: {} {}",alertId, tVal, hashForNAMT, tagMap);
                if(metricAlertConfig.isMissingEnabled()) {
                    final AlertEvent missingAlertEvent;
                    if (tVal == Double.NaN) {
                        //Missing

                        final long lastSeenTime;

                        if (alertStateStore.getLastSeenTime(hashForNAMT) != -1) {
                            lastSeenTime = alertStateStore.getLastSeenTime(hashForNAMT);
                        } else {
                            lastSeenTime = timestamp;
                        }
                        if (!shouldSuppress) {
                            missingAlertEvent = createMissingAlertEvent(hashForNAMT,
                                    AlertState.MISSING, new long[]{timestamp},
                                    metricAlertConfig, statusWriter,
                                    statusWriterState, tagMap, new double[]{tVal}, alertStateStore, lastSeenTime);
                        } else {
                            missingAlertEvent = null;
                        }

                        if(alertStateStore.getLastSeenTime(hashForNAMT) == -1) {
                            alertStateStore.updateDataPoint(namespace,alertId,tagMap,timestamp);
                        }
                    } else {
                        if (alertStateStore.getCurrentState(hashForNAMT) == AlertState.MISSING) {
                            //check missing recovery
                            if (!shouldSuppress) {
                                missingAlertEvent = createMissingAlertEvent(hashForNAMT,
                                        AlertState.GOOD, new long[]{timestamp},
                                        metricAlertConfig, statusWriter, statusWriterState,
                                        tagMap, new double[]{tVal}, alertStateStore, timestamp);
                            }
                            else {
                                missingAlertEvent = null;
                            }
                        } else {
                            missingAlertEvent = null;
                        }
                        alertStateStore.updateDataPoint(namespace, alertId, tagMap, timestamp);
                    }
                    timeseries.put(hashForNAMT, hashForNAMT);
                    if(missingAlertEvent != null) {
                        alertEvents.add(missingAlertEvent);
                        continue;
                        //Skip rest of the execution
                    }
                } else  {
                    //Update data point
                    if(tVal == Double.NaN) {
                        if(alertStateStore.getLastSeenTime(hashForNAMT) == -1) {
                            alertStateStore.updateDataPoint(namespace,alertId,tagMap,timestamp);
                        }
                    } else {
                        alertStateStore.updateDataPoint(namespace, alertId, tagMap, timestamp);
                    }
                }

                int countdown = 3;
                while(countdown > 0 && tVal != Double.NaN) {
                    long countOfBreached = 0;
                    long alertsRaised = 0;
                    double threshold = Double.NaN;
                    AlertState alertState = AlertState.GOOD;
                    String comparator = null;
                    if(countdown == 3 && metricAlertConfig.isHasBadThreshold()) {
                        threshold = metricAlertConfig.getBadThreshold();
                        alertState = AlertState.BAD;
                        comparator = metricAlertConfig.getComparisonOperator();
                    } else if(countdown == 2 && metricAlertConfig.isHasWarnThreshold()) {
                        threshold = metricAlertConfig.getWarnThreshold();
                        alertState = AlertState.WARN;
                        comparator = metricAlertConfig.getComparisonOperator();
                    } else if(countdown == 1 && metricAlertConfig.isHasRecoveryThreshold()){
                        threshold = metricAlertConfig.getRecoveryThreshold();
                        alertState = AlertState.GOOD;
                        comparator = metricAlertConfig.getFlippedComparisionOperator();
                    } else {
                        countdown--;
                        continue;
                    }

                    if (compare(tVal,threshold,comparator)) {
                        countOfBreached++;
                        AlertEvent alertEvent = null;
                        if (!shouldSuppress) { 
                            alertEvent = AlertUtils.createAlertEvent(hashForNAMT, metricAlertConfig,
                                alertState, tsField, alertStateStore, tagMap);
                            AlertUtils.updateAlertValues(valueForTheTimeseries,alertEvent,true);
                        }

                        final String status_msg = getDetailsString(metricName,alertState,
                                valueForTheTimeseries,metricAlertConfig).trim();

                        if (alertEvent != null) {

                            AlertUtils.setMetricName(alertEvent,metricName);
                            alertEvent.setAlertDetails(status_msg);
                            alertEvents.add(alertEvent);
                        }

                        if(!shouldSuppress && !statusWriterState.containsKey(hashForNAMT)) {
                            AlertUtils.writeStatus(statusWriter,
                                    metricAlertConfig,
                                    normalizedEndTimeInSecs,
                                    namespace,
                                    alertState,
                                    alertId,
                                    tagMap,
                                    status_msg);
                            statusWriterState.put(hashForNAMT,1l);
                        }

                    }

                    reportAlertStats(1, countOfBreached,
                            namespace, alertId, alertState);

                    countdown--;
                }

            }

            //Rest of the missing data points
            if(metricAlertConfig.isMissingEnabled()) {
                alertEvents.addAll(checkMissingFromState(alertStateStore, timeseries, metricAlertConfig, statusWriter,
                        statusWriterState,
                        new long[]{startTime},new double[]{Double.NaN}, heartbeatSuppressConditional));
            } else if(metricAlertConfig.isAutoRecover()) {
                alertEvents.addAll(checkAutoRecoveryFromState(alertStateStore, metricAlertConfig, statusWriter,
                        normalizedEndTimeInSecs,
                        new long[]{startTime},new double[]{Double.NaN}, heartbeatSuppressConditional));
            }

            AlertEventBag alertEventBag = new AlertEventBag(alertEvents, metricAlertConfig);
            LOG.debug("id: {} AlertEventBag: {}", metricAlertConfig.getAlertId(), alertEventBag);
            return alertEventBag;


        } catch (Exception e) {
            LOG.error("id: {} Error parsing tsdb response as json, ", metricAlertConfig.getAlertId(), e);
            Monitoring.get().countProcessingSummariesError(metricAlertConfig.getAlertId(),
                    metricAlertConfig.getNamespace());
            throw new AlertException("Error parsing tsdb response as json for summaries", e);
        }

    }

    public static Long2BooleanMap processHeartBeatForSummaries(final String response,
                                                               final SuppressMetricConfig suppressMetricConfig,
                                                               final String metricSourceId,
                                                               final MetricAlertConfig metricAlertConfig) {
        final JsonNode root;
        JsonNode summaryNode = null;
        final String namespace = metricAlertConfig.getNamespace();
        final long alertId = metricAlertConfig.getAlertId();

        String comparator = suppressMetricConfig.getComparatorType().getOperator();
        Double threshold = suppressMetricConfig.getThreshold();

        Long2BooleanMap resultMap = new Long2BooleanOpenHashMap();

        try {
            root = AlertUtils.parseJsonTree(response);
            final Iterator<JsonNode> results = root.get(RESULTS).elements();
            while (results.hasNext()) {
                final JsonNode next = results.next();
                if (next.get(SOURCE).asText().startsWith(metricSourceId)) {
                    summaryNode = next;
                }
            }

            final Iterator<JsonNode> heartBeatNodes = summaryNode.get(DATA).elements();

            while (heartBeatNodes.hasNext()) {
                final JsonNode next = heartBeatNodes.next();
                final List<Double> timeSeriesValues = new ArrayList<>();
                final TreeMap<String, String> tagsMap = new TreeMap<>();

                if (next.has(NumericSummaryType)) {
                    fillValuesAndTagsForSummaryType(next, tagsMap, timeSeriesValues);
                }

                if (timeSeriesValues.size() > 1) {
                    LOG.error("Summary has more than one value for {} in response for heartbeat {}",
                            tagsMap.toString(), response);
                    continue;
                }

                final double tVal = timeSeriesValues.get(0);
                final long hashForNAMT = AlertUtils.getHashForNAMT(namespace, alertId, tagsMap);
                LOG.info("id: {} (Heartbeat summaries) Received tVal: {} for hash: {} {}", alertId, tVal, hashForNAMT, tagsMap);

                resultMap.put(hashForNAMT, false);
                if (suppressMetricConfig.getComparatorType().equals(ComparatorType.MISSING)) {
                    if (Double.isNaN(tVal)) {
                        LOG.info("id: {} (Heartbeat missing) Received tVal: {} for hash: {} {}", alertId, tVal, hashForNAMT, tagsMap);
                        resultMap.put(hashForNAMT, true);
                    }
                } else {
                    if (!Double.isNaN(tVal)) {
                        if (compare(tVal, threshold, comparator)) {
                            LOG.info("id: {} (Heartbeat summary) Received tVal: {} for hash: {} {}", alertId, tVal, hashForNAMT, tagsMap);
                            resultMap.put(hashForNAMT, true);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            LOG.error("id: {} (Heartbeat) Error parsing tsdb response as json, ", alertId, e);
        }
        return resultMap;
    }

    public static Long2BooleanMap processHeartbeatForNonSummaries(final String response,
                                                                  final SuppressMetricConfig suppressMetricConfig,
                                                                  final String metricSourceId,
                                                                  final MetricAlertConfig metricAlertConfig) throws AlertException {
        try {

            Long2BooleanMap resultMap = new Long2BooleanOpenHashMap();
            final Map<String, JsonNode> sourceNodes = getSourceNodes(response);

            final JsonNode metricSourceNode = getStartsWith(sourceNodes, metricSourceId);
            final JsonNode metricHeartbeatNode = getStartsWith(sourceNodes, HEARTBEAT_NODE + ":" + HEARTBEAT_THRESHOLD_NODE);
            if (metricSourceNode == null || !metricSourceNode.hasNonNull(DATA)) {
                return resultMap;
            }
            if (metricHeartbeatNode == null || !metricHeartbeatNode.hasNonNull(DATA)) {
                return resultMap;
            }
            final Long2LongOpenHashMap recoveryTemporalThresholdMap;

            recoveryTemporalThresholdMap = getThresholdsMap(metricSourceNode, metricAlertConfig);

            long[] startEndInterval = AlertUtils.getStartEndAndIntervalFromTimeSpec(metricSourceNode);

            if(startEndInterval == null) {
                return resultMap;
            }
            final long valuesRequiredInWindow;
            if (suppressMetricConfig.getIsRequiredFullWindow()) {
                // no evaluation unless there is a full window
                final int reportingInterval = suppressMetricConfig.getReportingInterval();
                final long slidingWindowInSecs = metricAlertConfig.getSlidingWindowInSecs();
                if(slidingWindowInSecs < reportingInterval) {
                    LOG.error("alert id: {} slidingwindow {} is less " +
                                    "than reporting interval {} for the heartbear", metricAlertConfig.getAlertId(),
                            slidingWindowInSecs, reportingInterval);
                    return resultMap;
                } else if(startEndInterval[2] > reportingInterval){
                    valuesRequiredInWindow = slidingWindowInSecs / startEndInterval[2];
                } else {
                    valuesRequiredInWindow = slidingWindowInSecs/reportingInterval;
                }
            } else {
                valuesRequiredInWindow = -1;
            }

            evalHeartbeat(metricHeartbeatNode,
                    metricAlertConfig,
                    suppressMetricConfig.getSuppressMetricTemporalThreshold(),
                    recoveryTemporalThresholdMap,
                    resultMap,
                    valuesRequiredInWindow,
                    startEndInterval);

            if(!metricSourceNode.hasNonNull(DATA)) {
                LOG.info("id: {} metric values object not returned", metricAlertConfig.getAlertId());
            }

            LOG.debug("startend: {}, {}", Arrays.toString(startEndInterval), metricSourceNode.toString());

            LOG.info("id: {} Heartbeat resultMap: {}", metricAlertConfig.getAlertId(), resultMap);
            return resultMap;

        } catch (Exception e) {
            LOG.error("id: {} Heartbeat evaluating alert, ", metricAlertConfig.getAlertId(),e);
            Monitoring.get().countHeartBeatProcessingNonSummariesError(metricAlertConfig.getAlertId(),
                    metricAlertConfig.getNamespace());
            throw  new AlertException("Error evaluating alert", e);
        }
    }

    private static void reportAlertStats(long count, long countOfBreached,
                                         String namespace,
                                         long alertId, AlertState alertState) {
        Monitoring.get().reportSeriesInState(countOfBreached,namespace,
                alertId,alertState);
        Monitoring.get().countTimeseriesEvaluated(count,namespace,
                alertId,alertState);
    }

    private static void fillValuesAndTagsForSummaryType(final JsonNode next,
                                                        final TreeMap<String,String> tagMap,
                                                        final List<Double> valueForTheTimeseries) {
        fillValuesAndTagsForSummaryType(next, tagMap, valueForTheTimeseries, false);
    }

    /**
     * Generally this method is called in conjunction with time.
     * So, nans should never be exculded in those cases.
     * Nans should only be excluded if temporal threshold calc happening
     * @param next
     * @param tagMap
     * @param valueForTheTimeseries
     * @param excludeNans
     */
    private static void fillValuesAndTagsForSummaryType(final JsonNode next,
                                                        final TreeMap<String,String> tagMap,
                                                        final List<Double> valueForTheTimeseries,
                                                        final boolean excludeNans) {
        final JsonNode valueNode = next.get(TSDV3Constants.NumericSummaryType).get(DATA).get(0);

        final String tsField = valueNode.fieldNames().next();

        final double v = valueNode.get(tsField).get(0).asDouble();
        if(!excludeNans) {
            valueForTheTimeseries.add(v);
        } else {
            if(!Double.isNaN(v)) {
                valueForTheTimeseries.add(v);
            }
        }
        final JsonNode tagsNode = next.get(TSDV3Constants.TAGS);
        //AlertEvent event = new AlertEvent();

        tagsNode.fieldNames().forEachRemaining(key -> tagMap.put(key, tagsNode.get(key).asText()));
        if (tagMap.isEmpty()) {
            //Group by all.
            tagMap.put(QueryConstants.GROUP_BY_ALL, QueryConstants.GROUP_BY_ALL);
        }
    }

    public static Set<String> getEmptyHeartbeatTagset() {
        return EMPTY_HEARTBEAT_TAGSET;
    }

}
