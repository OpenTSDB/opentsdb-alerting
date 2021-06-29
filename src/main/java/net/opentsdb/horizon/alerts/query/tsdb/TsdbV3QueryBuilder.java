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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.config.SuppressMetricConfig;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.enums.SummaryType;
import net.opentsdb.horizon.alerts.enums.WindowSampler;
import net.opentsdb.horizon.alerts.query.QueryConstants;
import io.undertow.util.FileUtils;
import net.opentsdb.configuration.Configuration;
import net.opentsdb.core.DefaultTSDB;
import net.opentsdb.data.types.numeric.NumericType;
import net.opentsdb.query.QueryFillPolicy;
import net.opentsdb.query.QueryNodeConfig;
import net.opentsdb.query.SemanticQuery;
import net.opentsdb.query.execution.serdes.JsonV2QuerySerdesOptions;
import net.opentsdb.query.filter.DefaultNamedFilter;
import net.opentsdb.query.filter.FilterUtils;
import net.opentsdb.query.filter.QueryFilter;
import net.opentsdb.query.filter.QueryFilterFactory;
import net.opentsdb.query.interpolation.BaseInterpolatorConfig;
import net.opentsdb.query.interpolation.types.numeric.NumericInterpolatorConfig;
import net.opentsdb.query.joins.JoinConfig;
import net.opentsdb.query.pojo.FillPolicy;
import net.opentsdb.query.processor.expressions.ExpressionConfig;
import net.opentsdb.query.processor.summarizer.SummarizerConfig;
import net.opentsdb.utils.JSON;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static net.opentsdb.horizon.alerts.enums.ComparatorType.MISSING;


public class TsdbV3QueryBuilder {

    private static DefaultTSDB tsdb;
    private EnvironmentConfig config = new EnvironmentConfig();
    private static final String FILE_PLUGIN_PREFIXED = "file://";

    private static TsdbV3QueryBuilder tsdbV3Query = new TsdbV3QueryBuilder();



    private TsdbV3QueryBuilder() {
        final String cf = FILE_PLUGIN_PREFIXED+config.getTSDBConfigFile();
        System.setProperty(Configuration.CONFIG_PROVIDERS_KEY,cf);


        Configuration config = new Configuration();

        tsdb = new DefaultTSDB(config);
        try {
            //MockDataStoreFactory factory = new MockDataStoreFactory();
            //factory.initialize(tsdb, null);
            tsdb.initializeRegistry(true);
            //tsdb.getRegistry().registerPlugin(TimeSeriesDataSourceFactory.class, null, factory);
            tsdb.getMaintenanceTimer().stop();
            tsdb.getConfig().close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        tsdb.getMaintenanceTimer().stop();
    }

    public static SemanticQuery fromString(String semanticQueryJson) throws Exception {

        JsonNode jsonNode = JSON.getMapper().readTree(semanticQueryJson);
        SemanticQuery query = SemanticQuery.parse(tsdbV3Query.tsdb,jsonNode).build();

        return query;
    }

    public static SemanticQuery fromJsonNode(JsonNode node) throws Exception {
        SemanticQuery query = SemanticQuery.parse(tsdb,node).build();
        return query;
    }

    public static QueryFilter fromFilterStringForSnooze(String filter) throws IOException {
        final JsonNode child = AlertUtils.parseJsonTree(filter);

        JsonNode type_node = child.get("type");
        if (type_node == null) {
            return null;
        }

        String type = type_node.asText();
        if (Strings.isNullOrEmpty(type)) {
            return null;
        }

        QueryFilterFactory factory = tsdb.getRegistry().getPlugin(QueryFilterFactory.class, type);
        if (factory == null) {
            return null;
        }

        return DefaultNamedFilter.newBuilder().setId("id1")
                .setFilter(factory.parse(tsdb, JSON.getMapper(), child))
                .build().getFilter();
    }

    public static boolean matches(QueryFilter filter, Map<String,String> tags) {
        return FilterUtils.matchesTags(filter, tags, new HashSet<>());
    }

    public static JsonV2QuerySerdesOptions.Builder addSerdesForNonSummaries(MetricAlertConfig metricAlertConfig) {
        final JsonV2QuerySerdesOptions.Builder builder = JsonV2QuerySerdesOptions.newBuilder();

        if(metricAlertConfig.isHasBadThreshold()) {
            builder.addFilter(QueryConstants.BAD_THRESHOLD_NODE)
                    .addFilter(QueryConstants.BAD_ALERT_NODE);
        }

        if(metricAlertConfig.isHasWarnThreshold()) {
            builder.addFilter(QueryConstants.WARN_THRESHOLD_NODE)
                    .addFilter(QueryConstants.WARN_ALERT_NODE);
        }

        if(metricAlertConfig.isHasRecoveryThreshold()) {
            builder.addFilter(QueryConstants.RECOVERY_THRESHOLD_NODE)
                    .addFilter(QueryConstants.RECOVERY_ALERT_NODE);
        }
        return builder;
    }

    public static void addNodesForNonSummaries(String metricId,
                                               String metricSource,
                                               MetricAlertConfig metricAlertConfig,
                                               BaseInterpolatorConfig interpolatorConfig,
                                               List<QueryNodeConfig> executionGraph) {


        if(interpolatorConfig == null) {
             interpolatorConfig = (NumericInterpolatorConfig) NumericInterpolatorConfig.newBuilder()
                    .setFillPolicy(FillPolicy.NOT_A_NUMBER)
                    .setRealFillPolicy(QueryFillPolicy.FillWithRealPolicy.NONE)
                    .setDataType(NumericType.TYPE.toString())
                    .build();
        }

        if(metricAlertConfig.isHasBadThreshold()) {

            addNodeToExecutionGraph(QueryConstants.BAD_THRESHOLD_NODE, QueryConstants.BAD_ALERT_NODE,
                    metricId, metricSource,
                    metricAlertConfig.getComparisonOperator()
                    ,metricAlertConfig.getBadThreshold(),interpolatorConfig,executionGraph);
        }

        if(metricAlertConfig.isHasWarnThreshold()) {
            addNodeToExecutionGraph(QueryConstants.WARN_THRESHOLD_NODE, QueryConstants.WARN_ALERT_NODE,
                    metricId, metricSource,
                    metricAlertConfig.getComparisonOperator()
                    ,metricAlertConfig.getWarnThreshold(),interpolatorConfig,executionGraph);
        }

        if(metricAlertConfig.isHasRecoveryThreshold()) {
            addNodeToExecutionGraph(QueryConstants.RECOVERY_THRESHOLD_NODE, QueryConstants.RECOVERY_ALERT_NODE,
                    metricId, metricSource,
                    metricAlertConfig.getFlippedComparisionOperator()
                    ,metricAlertConfig.getRecoveryThreshold(),interpolatorConfig,executionGraph);
        }
    }

    private static void addNodeToExecutionGraph(final String badtld,
                                                final String badalert,
                                                final String metricId,
                                                final String metricSource,
                                                final String comparison_operator,
                                                final double bad_threshold,
                                                final BaseInterpolatorConfig interpolatorConfig,
                                                final List<QueryNodeConfig> executionGraph) {

        String expression = String.format(TSDV3Constants.BINARY_EXPRESSION_FORMAT, metricId,
                comparison_operator, bad_threshold);

        QueryNodeConfig badQueryThresholdNode = buildExprNode(badtld, expression, metricSource, interpolatorConfig);

        executionGraph.add(badQueryThresholdNode);

        QueryNodeConfig badAlertNode = SummarizerConfig.newBuilder()
                .addSummary(TSDV3Constants.TSDB_SUM)
                .addSource(badtld)
                .setId(badalert)
                .build();

        executionGraph.add(badAlertNode);
    }

    private static QueryNodeConfig buildExprNode(String tld,String expr, String metricSource, BaseInterpolatorConfig interpolatorConfig) {

        final QueryNodeConfig builtExpr = ExpressionConfig.newBuilder()
                .setExpression(expr)
                .setJoinConfig((JoinConfig) JoinConfig.newBuilder().setJoinType(JoinConfig.JoinType.NATURAL_OUTER).build())
                .setAs(tld)
                .addInterpolatorConfig(interpolatorConfig)
                .setId(tld)
                .addSource(metricSource).build();
        return builtExpr;
    }

    @VisibleForTesting
    static String addHeartbeatMetric(final String metricId,
                                     final String metricSource,
                                     final SuppressMetricConfig suppressMetricConfig,
                                     final List<QueryNodeConfig> executionGraph,
                                     final SummaryType summaryType,
                                     final JsonV2QuerySerdesOptions.Builder serdesOutputbuilder) {

        String nodeId = String.format(QueryConstants.SUMMARIZED, suppressMetricConfig.getMetricId());
        // Missing heartbeat type
        if (suppressMetricConfig.getComparatorType() == MISSING) {
            addNodesForSummary(SummaryType.SUM, metricSource, nodeId, executionGraph);
            serdesOutputbuilder.addFilter(nodeId);
            return nodeId;
        } else if (suppressMetricConfig.getSampler() == WindowSampler.SUMMARY) {
            addNodesForSummary(summaryType, metricSource, nodeId, executionGraph);
            serdesOutputbuilder.addFilter(nodeId);
            return nodeId;
        } else {
            addNodeToExecutionGraph(QueryConstants.HEARTBEAT_THRESHOLD_NODE, QueryConstants.HEARTBEAT_NODE,
                    metricId,
                    metricSource,
                    suppressMetricConfig.getComparatorType().getOperator(),
                    suppressMetricConfig.getThreshold(),
                    (NumericInterpolatorConfig) NumericInterpolatorConfig.newBuilder()
                            .setFillPolicy(FillPolicy.NOT_A_NUMBER)
                            .setRealFillPolicy(QueryFillPolicy.FillWithRealPolicy.NONE)
                            .setDataType(NumericType.TYPE.toString())
                            .build(),
                    executionGraph);
            serdesOutputbuilder.addFilter(QueryConstants.HEARTBEAT_THRESHOLD_NODE);
            serdesOutputbuilder.addFilter(QueryConstants.HEARTBEAT_NODE);
            return QueryConstants.HEARTBEAT_THRESHOLD_NODE;
        }
    }

    public static void main(String[] args) throws Exception {

        final String s = FileUtils.readFile(new FileInputStream("src/main/resources/query.json"));

        final SemanticQuery query = TsdbV3QueryBuilder.fromString(s);

        final String s1 = JSON.serializeToString(query);

        System.out.println(s);
        System.out.println(s1);
    }

    public static void addNodesForSummary(SummaryType summarizer, String metricSource,
                                          String nodeId, List<QueryNodeConfig> executionGraph) {
        /**
         * NumericSummaryInterpolatorConfig interpolatorConfig = (NumericSummaryInterpolatorConfig) NumericSummaryInterpolatorConfig.newBuilder()
         *                         .setDefaultFillPolicy(FillPolicy.NOT_A_NUMBER)
         *                         .setDefaultRealFillPolicy(QueryFillPolicy.FillWithRealPolicy.NONE)
         *                         .addExpectedSummary(0)
         *                         .setDataType(NumericSummaryType.TYPE.toString())
         *                         .build();
         */
        QueryNodeConfig summaryNode = SummarizerConfig.newBuilder()
                .addSummary(summarizer.name())
                .addSource(metricSource)
                .setId(nodeId)
                .build();
        executionGraph.add(summaryNode);

    }
}
