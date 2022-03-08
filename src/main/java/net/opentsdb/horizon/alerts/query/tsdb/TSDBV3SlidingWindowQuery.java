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

import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.config.SuppressMetricConfig;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.query.StateTimeBasedExecutor;
import net.opentsdb.horizon.alerts.processor.impl.StatusWriter;
import net.opentsdb.horizon.alerts.snooze.SnoozeFilter;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import net.opentsdb.horizon.alerts.query.QueryConstants;
import net.opentsdb.query.QueryNodeConfig;
import net.opentsdb.query.SemanticQuery;
import net.opentsdb.query.TimeSeriesDataSourceConfig;
import net.opentsdb.query.execution.serdes.JsonV2QuerySerdesOptions;
import net.opentsdb.query.filter.NamedFilter;
import net.opentsdb.query.processor.expressions.ExpressionConfig;
import net.opentsdb.query.serdes.SerdesOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.opentsdb.horizon.alerts.processor.Conditional;
import net.opentsdb.horizon.alerts.processor.impl.HeartbeatSuppressConditional;
import net.opentsdb.horizon.alerts.processor.impl.NoOpConditional;

import static net.opentsdb.horizon.alerts.enums.WindowSampler.SUMMARY;

/**
 * TODO: Move TSD logic to a dataStore and make this class of type singleMetric.
 * TODO: Unifed processor interface ?? Since I seem to be doing that everywhere.
 */
public class TSDBV3SlidingWindowQuery extends StateTimeBasedExecutor<MetricAlertConfig> {

    private volatile List<QueryNodeConfig> executionGraph;

    private volatile List<QueryNodeConfig> baseExecutionGraph;

    private volatile List<NamedFilter> filters;

    private static final Logger LOG = LoggerFactory.getLogger(TSDBV3SlidingWindowQuery.class);

    private volatile String namespace;

    private MetricAlertConfig metricAlertConfig;

    private final TSDBClient tsdbClient;

    private volatile String metricId = null;

    private volatile String metricSource = null;

    private volatile String heartBeatMetricSource = null;

    private volatile String heartBeatMetricId = null;

    /** Final metric name used for display purposes.
     * This might come as a fully-qualified metric name, user-provided alias,
     * or raw expression (e.g. 'q1.m1 - m2').
     */
    private volatile String metricName = null;

    private volatile String heartBeatMetricNodeId = null;

    private volatile String metricSourceFromConfig = null;

    private EnvironmentConfig environmentConfig = new EnvironmentConfig();

    private volatile SerdesOptions serdes = null;

    private final long alertId;

    private boolean summaries = false;

    private AlertStateStore alertStateStore;

    private StatusWriter statusWriter;

    public TSDBV3SlidingWindowQuery(MetricAlertConfig alertConfig) {
        this(alertConfig, null);
    }

    public TSDBV3SlidingWindowQuery(MetricAlertConfig alertConfig, TSDBClient tsdbClient) {
        super(alertConfig);
        this.alertId = alertConfig.getAlertId();
        if(Objects.nonNull(tsdbClient)) {
            this.tsdbClient = tsdbClient;
        } else {
            this.tsdbClient =
                    new TSDBClient(environmentConfig.getTsdbEndpoint(),
                            environmentConfig.getTSDBAuthProvider());
        }
    }

    public TSDBV3SlidingWindowQuery(MetricAlertConfig alertConfig, TSDBClient tsdbClient, StatusWriter statusWriter) {
        this(alertConfig, tsdbClient);
        this.statusWriter = statusWriter;
    }
    /**
     * Called on init.
     * Not thread safe.
     * Access needs to be controlled.
     * @param metricAlertConfig
     */
    @Override
    public boolean prepAndValidate(MetricAlertConfig metricAlertConfig,
                                   AlertStateStore alertStateStore) throws AlertException {

        try {
            /**
             * This method will have the same impact if called multiple times.
             * Except that the alertStateStore is carried forward.
             */
            this.metricAlertConfig = metricAlertConfig;

            if (this.statusWriter == null) {
                this.statusWriter = new StatusWriter(metricAlertConfig, new SnoozeFilter());
            }

            this.alertStateStore = alertStateStore;

            this.namespace = metricAlertConfig.getNamespace();

            SemanticQuery query = TsdbV3QueryBuilder.fromJsonNode(metricAlertConfig.getQueryJson());

            this.executionGraph = query.getExecutionGraph();

            baseExecutionGraph = new ArrayList<>();

            filters = query.getFilters();
            final Map<String,QueryNodeConfig> queyNodeMap = getQueryNodeMap(executionGraph);
            JsonV2QuerySerdesOptions.Builder serdesOutputbuilder = null;


            final QueryNodeConfig nodeConfig = queyNodeMap.get(metricAlertConfig.getMetricId());

            metricSourceFromConfig = metricAlertConfig.getMetricId();

            metricSource = metricAlertConfig.getMetricId();
            
            heartBeatMetricSource = metricAlertConfig.getSuppressMetricConfig() == null ? null : 
                    metricAlertConfig.getSuppressMetricConfig().getMetricId();

            final QueryNodeConfig dataSourceNode = getDataSourceNodeNearestFromRoot(nodeConfig, queyNodeMap);

            //infectiousNanTrue(executionGraph);

            baseExecutionGraph.addAll(executionGraph);

            metricId = dataSourceNode.getId();

            // Use user-defined alias if available.
            metricName = metricAlertConfig.getMetricAlias();
            if (metricName == null) {
                // Alias is not available, parse metric name from data node.
                metricName = getMetricNameFromNode(dataSourceNode);
            }
            if (metricName == null) {
                // Fall back to metricId in the worst case.
                metricName = metricId;
            }

            // Add a summarizer
            if (metricAlertConfig.getWindowSampler() == SUMMARY) {
                String nodeId = String.format(QueryConstants.SUMMARIZED, metricId);
                TsdbV3QueryBuilder.addNodesForSummary(metricAlertConfig.getSummarizer(),
                        metricSource,nodeId,executionGraph);
                metricSource = nodeId;
                serdesOutputbuilder = JsonV2QuerySerdesOptions.newBuilder();
                summaries = true;

            } else {
                TsdbV3QueryBuilder.addNodesForNonSummaries(metricId, metricSource,
                        metricAlertConfig, null, executionGraph);
                serdesOutputbuilder = TsdbV3QueryBuilder.addSerdesForNonSummaries(metricAlertConfig);
                summaries = false;
            }

            // Add heartbeat metric here
            if (heartBeatMetricSource != null) {
                final QueryNodeConfig heartBeatnodeConfig = queyNodeMap.get(heartBeatMetricSource);
                final QueryNodeConfig heartBeatdataSourceNode = getDataSourceNodeNearestFromRoot(heartBeatnodeConfig, queyNodeMap);

                heartBeatMetricNodeId = TsdbV3QueryBuilder.addHeartbeatMetric(
                        heartBeatdataSourceNode.getId(),
                        heartBeatMetricSource,
                        metricAlertConfig.getSuppressMetricConfig(),
                        executionGraph,
                        metricAlertConfig.getSuppressMetricConfig().getSummaryType(),
                        serdesOutputbuilder);
            }

            LOG.debug("Added parts: {} {} {}", metricId, metricName, metricSource);
            serdes = serdesOutputbuilder
                    .addFilter(metricSource)
                    .setType(TSDV3Constants.JSONV3_QUERY_SERDES)
                    .setId(TSDV3Constants.JSONV3_QUERY_SERDES).build();

            return true;

        } catch (Exception e) {
            throw new AlertException("Unable to parse tsd config", e);
        }

    }

    private void infectiousNanTrue(final List<QueryNodeConfig> executionGraph) {

        for(int i = 0; i < executionGraph.size(); i++) {
            if(executionGraph.get(i) instanceof ExpressionConfig) {
                ExpressionConfig config = (ExpressionConfig) executionGraph.get(i);
                final ExpressionConfig.Builder builder = config.toBuilder();
                builder.setInfectiousNan(true);
                executionGraph.set(i,builder.build());
            }
        }
    }

    private static String getMetricNameFromNode(QueryNodeConfig dataSourceNode) {

        if(dataSourceNode instanceof TimeSeriesDataSourceConfig) {
            TimeSeriesDataSourceConfig config = (TimeSeriesDataSourceConfig) dataSourceNode;
            return config.getMetric().getMetric();
        } else if(dataSourceNode instanceof ExpressionConfig) {
            ExpressionConfig config = (ExpressionConfig) dataSourceNode;
            return config.getExpression();
        }

        return null;
    }

    /**
     * Method assumes that there is no
     * merging of independent sources
     * @param nodeConfig
     * @param queryNodeMap
     * @return
     */
    private static QueryNodeConfig getDataSourceNodeNearestFromRoot(final QueryNodeConfig nodeConfig,
                                                             final Map<String, QueryNodeConfig> queryNodeMap) {
        if(nodeConfig.joins() || nodeConfig instanceof TimeSeriesDataSourceConfig) {
            return nodeConfig;
        } else {
            return getDataSourceNodeNearestFromRoot(queryNodeMap.get(nodeConfig.getSources().get(0)),queryNodeMap);
        }
    }

    private static Map<String, QueryNodeConfig> getQueryNodeMap(final List<QueryNodeConfig> executionGraph) {

        return executionGraph.stream()
                .collect(Collectors.toMap(QueryNodeConfig::getId, Function.identity()));

    }

    /**
     * TODO: Break this guy down. Separate the query part from this class.
     * @param endTime
     * @param timeUnit
     * @return
     * @throws AlertException
     */
    @Override
    public AlertEventBag execute(final long endTime,
                                 final TimeUnit timeUnit,
                                 final AlertStateStore alertStateStore) throws AlertException {

        long normalizedEndTimeInSecs = getNormalizedTime(endTime,timeUnit);

        if(!metricAlertConfig.isHasBadThreshold() && !metricAlertConfig.isHasWarnThreshold()) {
            return new AlertEventBag(new ArrayList<>(),metricAlertConfig);
        }
        //tsdb time is end inclusive - subtract one window.
        //
        final String response = TsdbV3ResultProcessor.runQueryAndGetResponse(tsdbClient,this.alertId,this.namespace,
                executionGraph,filters,serdes, normalizedEndTimeInSecs,
                metricAlertConfig.getSlidingWindowInSecs());
        //LOG.info("id: {} Received result query JSON",this.alertId);
        LOG.info("id: {} Received result query JSON: {}",this.alertId,response);

        final Conditional heartbeatSuppressConditional;
        final AlertEventBag alertEventBag;
        final SuppressMetricConfig suppressMetricConfig = this.metricAlertConfig.getSuppressMetricConfig();
        if (suppressMetricConfig != null) {

            final Set<String> keySet = !suppressMetricConfig.getKeySet().isEmpty()
                    ? suppressMetricConfig.getKeySet() : TsdbV3ResultProcessor.getEmptyHeartbeatTagset() ;
            LOG.info("id: {} Processing heartbeat with suppress metric config: {} and key set: {}", this.alertId, suppressMetricConfig, keySet);
            if (suppressMetricConfig.getSampler().equals(SUMMARY)) {
                Long2BooleanMap long2BooleanMap = TsdbV3ResultProcessor.processHeartBeatForSummaries(response,
                        suppressMetricConfig,
                        this.heartBeatMetricNodeId,
                        metricAlertConfig);
                heartbeatSuppressConditional = new HeartbeatSuppressConditional(long2BooleanMap, keySet, namespace, alertId);
            } else {
                Long2BooleanMap long2BooleanMap = TsdbV3ResultProcessor.processHeartbeatForNonSummaries(response,
                        suppressMetricConfig,
                        this.heartBeatMetricNodeId,
                        metricAlertConfig);
                heartbeatSuppressConditional = new HeartbeatSuppressConditional(long2BooleanMap, keySet, namespace, alertId);
            }
        } else {
            heartbeatSuppressConditional = new NoOpConditional();
        }

        if (summaries) {
            alertEventBag = TsdbV3ResultProcessor.processForSummaries(response, this.statusWriter,
                    this.metricAlertConfig, this.metricSource,
                    this.metricId, this.metricName, this.alertStateStore,
                    normalizedEndTimeInSecs, metricAlertConfig.getSlidingWindowInSecs(), heartbeatSuppressConditional);
        } else {
            alertEventBag = TsdbV3ResultProcessor.processForNonSummaries(response, this.statusWriter,
                    this.metricAlertConfig, this.metricSource,
                    this.metricId, this.metricName, this.alertStateStore, heartbeatSuppressConditional);
        }

        // Run query only when needed
        if(alertEventBag != null && !alertEventBag.getAlertEvents().isEmpty()) {
            JsonV2QuerySerdesOptions.Builder builder = JsonV2QuerySerdesOptions.newBuilder();
            SerdesOptions serdesLocal = builder
                    .addFilter(metricSourceFromConfig)
                    .setType(TSDV3Constants.JSONV3_QUERY_SERDES)
                    .setId(TSDV3Constants.JSONV3_QUERY_SERDES).build();

            final String alert_query_response = TsdbV3ResultProcessor.runQueryAndGetResponse(tsdbClient, this.alertId, this.namespace,
                    baseExecutionGraph, filters, serdesLocal, normalizedEndTimeInSecs,
                    getTimeseriesWindowLengthForNotification(metricAlertConfig.getSlidingWindowInSecs()));

            TsdbV3ResultProcessor.
                    updateWithValues(alert_query_response, metricAlertConfig,
                            metricSourceFromConfig, metricId, alertEventBag);
        }



        return alertEventBag;
    }

    private long getTimeseriesWindowLengthForNotification(long slidingWindowLength) {

        return slidingWindowLength + (60*environmentConfig.getTimeFactorForGraph());
    }

    private long getNormalizedTime(long endTime, TimeUnit timeUnit) {

        return timeUnit.convert(endTime, timeUnit);

    }

}
