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

package net.opentsdb.horizon.alerts.processor.impl;

import com.fasterxml.jackson.databind.JsonNode;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.config.AlertConfigFields;
import net.opentsdb.horizon.alerts.config.SuppressMetricConfig;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.processor.Conditional;
import net.opentsdb.horizon.alerts.query.QueryConstants;
import net.opentsdb.horizon.alerts.query.tsdb.TsdbV3ResultProcessor;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import static net.opentsdb.horizon.alerts.AlertUtils.parseJsonTree;
import static net.opentsdb.horizon.alerts.query.QueryConstants.HEARTBEAT_THRESHOLD_NODE;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class HeartbeatSuppressConditionalTest {

    @Test
    public void testSuppress_ForSummaries() throws IOException {
        final JsonNode root = parseJsonTree(TestUtil.loadResource("data/heartbeat/heartbeat.json"));

        final JsonNode thresholdNode = root.get(AlertConfigFields.THRESHOLD);

        final JsonNode suppressNode = thresholdNode.get(AlertConfigFields.SUPPRESS_METRIC);

        SuppressMetricConfig suppressMetricConfig =
                MetricAlertConfig.extractSuppressMetricConfig(suppressNode, root,60L);
        String response = TestUtil.loadResource("data/heartbeat/heartbeat_response_2.json");

        String nodeId = String.format(QueryConstants.SUMMARIZED, suppressMetricConfig.getMetricId());
        
        suppressMetricConfig.getSampler();
        MetricAlertConfig alertConfig = TestUtil.getMetricAlertConfig("src/test/resources/data/heartbeat/heartbeat.json");
        Long2BooleanMap heartbeatMap = TsdbV3ResultProcessor.processHeartBeatForSummaries(response, suppressMetricConfig,nodeId, alertConfig);

        Conditional heartbeatConditional = new HeartbeatSuppressConditional(heartbeatMap, suppressMetricConfig.getKeySet(), "NS1", 109);

        TreeMap<String,String> tagMap = new TreeMap<>();
        tagMap.put("host", "host-1.hostname.com");
        tagMap.put("container_name", "alerts_ns2");

        assertFalse(heartbeatConditional.checkCondition(tagMap));

        tagMap = new TreeMap<>();
        tagMap.put("host", "host-2.hostname.com");
        tagMap.put("container_name", "alerts_ns2");
        
        assertTrue(heartbeatConditional.checkCondition(tagMap));
    }

    @Test
    public void testSuppress_ForSummaries_NotPresentInMap() throws IOException {
        final JsonNode root = parseJsonTree(TestUtil.loadResource("data/heartbeat/heartbeat.json"));

        final JsonNode thresholdNode = root.get(AlertConfigFields.THRESHOLD);

        final JsonNode suppressNode = thresholdNode.get(AlertConfigFields.SUPPRESS_METRIC);

        SuppressMetricConfig suppressMetricConfig =
                MetricAlertConfig.extractSuppressMetricConfig(suppressNode, root, 60L);
        String response = TestUtil.loadResource("data/heartbeat/heartbeat_response_2.json");

        String nodeId = String.format(QueryConstants.SUMMARIZED, suppressMetricConfig.getMetricId());
        MetricAlertConfig alertConfig = TestUtil.getMetricAlertConfig("src/test/resources/data/heartbeat/heartbeat.json");
        Long2BooleanMap heartbeatMap = TsdbV3ResultProcessor.processHeartBeatForSummaries(response, suppressMetricConfig,nodeId, alertConfig);

        Conditional heartbeatConditional = new HeartbeatSuppressConditional(heartbeatMap, suppressMetricConfig.getKeySet(), "NS1", 109);

        TreeMap<String,String> tagMap = new TreeMap<>();
        tagMap.put("host", "deadhost-1.hostname.com");
        tagMap.put("container_name", "alerts_ns2");

        assertFalse(heartbeatConditional.checkCondition(tagMap));
    }

    @Test
    public void testSuppress_ForNonSummaries() throws IOException, AlertException {
        Set<String> keySet = new HashSet<>();
        keySet.add("host");

        final JsonNode root = parseJsonTree(TestUtil.loadResource("data/heartbeat/heartbeat_2.json"));

        final JsonNode thresholdNode = root.get(AlertConfigFields.THRESHOLD);

        final JsonNode suppressNode = thresholdNode.get(AlertConfigFields.SUPPRESS_METRIC);

        SuppressMetricConfig suppressMetricConfig =
                MetricAlertConfig.extractSuppressMetricConfig(suppressNode, root, 60L);
        String response = TestUtil.loadResource("data/heartbeat/heartbeat_response_non_summaries");

        MetricAlertConfig alertConfig = TestUtil.getMetricAlertConfig("src/test/resources/data/heartbeat/heartbeat.json");
        Long2BooleanMap heartbeatMap = TsdbV3ResultProcessor.processHeartbeatForNonSummaries(response, suppressMetricConfig, HEARTBEAT_THRESHOLD_NODE, alertConfig);

        Conditional heartbeatConditional = new HeartbeatSuppressConditional(heartbeatMap, keySet, "NS1", 109);
        
        TreeMap<String,String> tagMap = new TreeMap<>();
        tagMap.put("host", "host-3.hostname.com");
        tagMap.put("container_name", "alerts_ns2");

        assertTrue(heartbeatConditional.checkCondition(tagMap));

        tagMap = new TreeMap<>();
        tagMap.put("host", "host-4.hostname.com");
        tagMap.put("container_name", "alerts_ns2");

        assertFalse(heartbeatConditional.checkCondition(tagMap));
    }

}
