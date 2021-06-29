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

package net.opentsdb.horizon.alerts.config.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.AlertConfigFields;

import net.opentsdb.horizon.alerts.config.MetricAliasParser;
import net.opentsdb.horizon.alerts.config.SuppressMetricConfig;
import net.opentsdb.horizon.alerts.enums.*;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.processor.impl.UpdatableExecutorWrapper;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import net.opentsdb.horizon.alerts.state.AlertStateStores;
import net.opentsdb.horizon.alerts.state.purge.Purge;
import net.opentsdb.horizon.alerts.state.purge.impl.MetricPurgePolicy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import static net.opentsdb.horizon.alerts.config.AlertConfigFields.SUPPRESS_METRIC;
import static net.opentsdb.horizon.alerts.config.impl.MetricAlertConfigFields.*;

@Slf4j
@Getter
public class MetricAlertConfig extends AlertConfig {



    private long slidingWindowInSecs = 300;

    private String comparisonOperator;

    private String flippedComparisionOperator;

    private String comparisonString;

    private Iterator<JsonNode> elements = null;

    /**
     * at_least_once/all_of_the_times/summary(on_avg/in_total)
     */
    private WindowSampler windowSampler = WindowSampler.ALL_OF_THE_TIMES;

    private String metricId = null;

    private String metricAlias;

    private long temporalThreshold = 1;

    private SummaryType summarizer = null;

    private String samplerStringForMessage = null;

    private String flippedSamplerStringForMessage = null;

    private boolean deriveTemporalThresholdForRecovery = false;

    private boolean deriveTemporalThresholdForAlert = false;

    private boolean requireFullWindow = false;

    private boolean autoRecover = false;

    private int autoRecoveryInterval = 3600;
    
    private SuppressMetricConfig suppressMetricConfig = null;

    //default
    private int reportingInterval = 60;

    public MetricAlertConfig(String namespace, AlertType alertType,
                             MetricAlertType metricAlertType, long alertId, long last_mofied) {
        super(namespace,alertType,alertId, last_mofied);
        //System.out.println(this.toString());
    }

    @Override
    public void parseAlertSpecific(JsonNode root) {

        final JsonNode thresholdNode = root.get(AlertConfigFields.THRESHOLD);

        final JsonNode singleMetric = thresholdNode.get(MetricAlertConfigFields.SINGLE_METRIC);

        slidingWindowInSecs = singleMetric.get(MetricAlertConfigFields.SLIDING_WINDOW).asLong();

        comparisonString = singleMetric.get(COMPARISON_OPERATOR).asText();

        String sampler = singleMetric.get(TIME_SAMPLER).
                asText(MetricAlertConfigFields.ALL_OF_THE_TIMES);

        if (thresholdNode.hasNonNull(AUTO_RECOVERY_INTERVAL)) {

            autoRecover = true;

            autoRecoveryInterval = thresholdNode.get(AUTO_RECOVERY_INTERVAL).asInt();
        }

        if (thresholdNode.hasNonNull(SUPPRESS_METRIC)) {
            final JsonNode node = thresholdNode.get(SUPPRESS_METRIC);
            this.suppressMetricConfig = extractSuppressMetricConfig(node, root, slidingWindowInSecs);
        }

        switch (sampler) {
            case MetricAlertConfigFields.ATLEAST_ONCE:
                temporalThreshold = 1;
                deriveTemporalThresholdForRecovery = true;
                windowSampler = WindowSampler.AT_LEAST_ONCE;
                samplerStringForMessage = MetricAlertConfigFields.ATLEAST_ONCE_STRING;
                flippedSamplerStringForMessage = MetricAlertConfigFields.ALL_OF_THE_TIMES_STRING;
                break;
            case MetricAlertConfigFields.ALL_OF_THE_TIMES:
                temporalThreshold = slidingWindowInSecs/ AlertUtils.dataFrequencyInSecs;
                deriveTemporalThresholdForAlert = true;
                deriveTemporalThresholdForRecovery = true;
                windowSampler = WindowSampler.ALL_OF_THE_TIMES;

                samplerStringForMessage = MetricAlertConfigFields.ALL_OF_THE_TIMES_STRING;
                flippedSamplerStringForMessage = samplerStringForMessage;

                if(singleMetric.hasNonNull(REQUIRE_FULL_WINDOW)) {
                    requireFullWindow = singleMetric.get(REQUIRE_FULL_WINDOW).asBoolean();
                    if(requireFullWindow) {

                        if(singleMetric.hasNonNull(MetricAlertConfigFields.REPORTING_INTERVAL)) {
                            reportingInterval = singleMetric.get(MetricAlertConfigFields.REPORTING_INTERVAL).asInt();
                        }
                    }
                }
                break;
            case ON_AVG:
                temporalThreshold = 1;
                summarizer = SummaryType.AVG;
                windowSampler = WindowSampler.SUMMARY;
                samplerStringForMessage = MetricAlertConfigFields.ON_AVG_STRING;
                flippedSamplerStringForMessage = samplerStringForMessage;
                break;
            case IN_TOTAL:
                temporalThreshold = 1;
                summarizer = SummaryType.SUM;
                windowSampler = WindowSampler.SUMMARY;
                samplerStringForMessage = MetricAlertConfigFields.IN_TOTAL_STRING;
                flippedSamplerStringForMessage = samplerStringForMessage;
                break;

        }

        comparisonOperator = AlertUtils.getComparatorFromWord(comparisonString);

        flippedComparisionOperator = AlertUtils.flipOperator(comparisonOperator);

        if(!super.isHasRecoveryThreshold()) {
            //recoveryThreshold =
            if(super.isHasWarnThreshold()) {
                super.recoveryThreshold = AlertUtils.calculateRecoveryThreshold(
                        super.getWarnThreshold(),comparisonString);
            } else if(super.isHasBadThreshold()) {
                super.recoveryThreshold = AlertUtils.calculateRecoveryThreshold(
                        super.getBadThreshold(),comparisonString);
            }
            super.hasRecoveryThreshold = true;
        }

        //System.out.println("ID: "+ getAlertId() + " tt: "+ temporalThreshold + " summ "+ summarizer);

        metricId = singleMetric.get(MetricAlertConfigFields.METRIC_ID).asText();
        metricAlias = parseMetricAlias(root, metricId);
        //LOG.debug("Recovery threshold: {} Bad threshold: {} Warn threshold: {} Comparator: {} S: {} s: {}",
          //      recoveryThreshold,badThreshold,warnThreshold,comparisonString,(s != null && !s.isEmpty()),s);

    }

    @VisibleForTesting
    public static SuppressMetricConfig extractSuppressMetricConfig(final JsonNode node,
                                                                   final JsonNode rootNode,
                                                                   final long slidingWindowInSecs) {
            /*
            "suppress": {
              "comparisonOperator": "missing",
              "threshold": null,
              "reportingInterval": 60,
              "metricId": "q2_m1_groupby"
             }
             */

        String heartBeatSampler = node.hasNonNull(TIME_SAMPLER) ?
                node.get(TIME_SAMPLER).asText(MetricAlertConfigFields.ALL_OF_THE_TIMES) :
                MetricAlertConfigFields.ALL_OF_THE_TIMES;
        String metricId = node.get("metricId").asText();

        boolean requiresFullWindow = node.hasNonNull(REQUIRE_FULL_WINDOW) && node.get(REQUIRE_FULL_WINDOW).asBoolean();

        final Double threshold = node.get("threshold").asText().equals("null") ? null : Double.valueOf(node.get("threshold").asText());
        int reportingInterval = node.get("reportingInterval").asInt();

        ComparatorType comparatorType = ComparatorType
                .getComparatorTypeFromString(node.get(COMPARISON_OPERATOR).asText());

        long suppressMetricTemporalThreshold = SuppressMetricConfig.getTemporalThreshold(heartBeatSampler, slidingWindowInSecs);

        return new SuppressMetricConfig(metricId,
                reportingInterval,
                comparatorType,
                threshold,
                WindowSampler.getWindowAggregatorTypeFromString(heartBeatSampler, comparatorType),
                SummaryType.getSummaryTypeFromString(heartBeatSampler),
                requiresFullWindow,
                suppressMetricTemporalThreshold,
                getTagKeysForHeartbeat(rootNode, metricId));
    }

    private static Set<String> getTagKeysForHeartbeat(JsonNode rootNode, String metricId) {

        final Set<String> keySet = new HashSet<>();
        final JsonNode queries = rootNode.get(MetricAlertConfigFields.QUERY);
        final JsonNode tsdbNode = queries.get(MetricAlertConfigFields.QUERY_TYPE_TSDB);

        for (JsonNode executionGraphs: tsdbNode) {
            final JsonNode executionGraph = executionGraphs.get(MetricAlertConfigFields.EXECUTION_GRAPH);
            final Iterator<JsonNode> nodeIterator = executionGraph.elements();

            while (nodeIterator.hasNext()) {
                JsonNode node = nodeIterator.next();
                String metric = node.get("id").asText();
                if (metric.equalsIgnoreCase(metricId)) {
                    if (node.has("tagKeys")) {
                        for (final JsonNode objNode : node.get("tagKeys")) {
                            keySet.add(objNode.textValue());
                        }
                    }
                }
            }
        }
        return keySet;
    }
    /**
     * @param root JsonNode of the alert configuration.
     * @param metricId metric id parse alias for, e.g. "q1_m1", "q2_e2", "q1_m1_groupby".
     * @return parsed alias, null if parsing failed or corresponding alias
     * couldn't be found.
     */
    private String parseMetricAlias(JsonNode root, String metricId) {
        if (metricId == null) {
            return null;
        }
        final String[] parts = metricId.split("_");
        if (parts.length < 2) {
            return null;
        }
        final Map<String, String> aliases = MetricAliasParser.parseAliases(root);
        return aliases.get(parts[0] + "_" + parts[1]);
    }

    public String getDefaultQueryType() {
        return MetricAlertConfigFields.QUERY_TYPE_TSDB;
    }

    @Override
    public UpdatableExecutorWrapper<MetricAlertConfig> createAlertExecutor() {

        return new UpdatableExecutorWrapper<>(this);
    }

    @Override
    public String toString() {
        return (super.toString() + String.format(" metricId : %s ," +
                        " sliding_window : %s, " +
                        "comparison: %s, autoRecover: %s autoRecoveryInterval: %s, requiresFullWindow: %s " +
                        "reportingInterval: %s",
                metricId,slidingWindowInSecs, comparisonOperator,
                autoRecover,
                autoRecoveryInterval, requireFullWindow, reportingInterval));
    }

    @Override
    protected boolean validateConfig() throws AlertException {

        if(!AlertUtils.isEmpty(metricId) &&
                !AlertUtils.isEmpty(comparisonOperator)) {
            return true;
        }
        return false;
    }

    @Override
    public AlertEvent createAlertEvent(final long hash,
                                       final String tsField,
                                       final SortedMap<String, String> tags,
                                       final AlertState alertType) {
        return AlertUtils.createSingleMetricAlertEvent(tsField,tags,alertType,this);
    }

    @Override
    public AlertStateStore createAlertStateStore() {

        if(autoRecover) {
            return AlertStateStores.withTransitionsAndMissing(String.valueOf(getAlertId()),
                    super.getNagIntervalInSecs(), super.getTransitionConfig());
        } else {
            return super.createAlertStateStore();
        }

    }

    @Override
    public boolean storeIdentity() {

        return (autoRecover || super.storeIdentity());

    }

    @Override
    public Purge createPurge() {

        return Purge.PurgeBuilder.create()
                .addPolicy(new MetricPurgePolicy(this))
                .build();

    }
}
