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

package net.opentsdb.horizon.alerts.query;

import com.fasterxml.jackson.databind.JsonNode;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.config.AlertConfigFields;
import net.opentsdb.horizon.alerts.config.SuppressMetricConfig;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.query.tsdb.TsdbV3ResultProcessor;
import org.testng.annotations.Test;

import java.io.IOException;

import static net.opentsdb.horizon.alerts.query.QueryConstants.HEARTBEAT_THRESHOLD_NODE;
import static net.opentsdb.horizon.alerts.AlertUtils.parseJsonTree;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class TsdbV3ResultProcessorTest {

    @Test
    public void testProcessHeartBeatForSummaries() throws IOException {

        final JsonNode root = parseJsonTree(TestUtil.loadResource("data/heartbeat/heartbeat.json"));

        final JsonNode thresholdNode = root.get(AlertConfigFields.THRESHOLD);

        final JsonNode suppressNode = thresholdNode.get(AlertConfigFields.SUPPRESS_METRIC);

        SuppressMetricConfig suppressMetricConfig =
                MetricAlertConfig.extractSuppressMetricConfig(suppressNode, root,60L);
        String response = TestUtil.loadResource("data/heartbeat/heartbeat_response_2.json");

        String nodeId = String.format(QueryConstants.SUMMARIZED, suppressMetricConfig.getMetricId());
        MetricAlertConfig alertConfig = TestUtil.getMetricAlertConfig("src/test/resources/data/heartbeat/heartbeat.json");
        Long2BooleanMap heartbeatMap = TsdbV3ResultProcessor.processHeartBeatForSummaries(response, suppressMetricConfig,nodeId, alertConfig);

        long trueCount = heartbeatMap.values().stream().filter(Boolean::booleanValue).count();
        assertEquals(trueCount, 1);
        long falseCount = heartbeatMap.values().stream().filter(b -> !b).count();
        assertEquals(falseCount, 1354);

    }

    @Test
    public void testProcessHeartBeatForSummaries_withMissing() throws IOException {

        final JsonNode root = parseJsonTree(TestUtil.loadResource("data/heartbeat/heartbeat.json"));

        final JsonNode thresholdNode = root.get(AlertConfigFields.THRESHOLD);

        final JsonNode suppressNode = thresholdNode.get(AlertConfigFields.SUPPRESS_METRIC);

        SuppressMetricConfig suppressMetricConfig =
                MetricAlertConfig.extractSuppressMetricConfig(suppressNode, root, 60L);
        String response = TestUtil.loadResource("data/heartbeat/heartbeat_response.json");

        String nodeId = String.format(QueryConstants.SUMMARIZED, suppressMetricConfig.getMetricId());
        MetricAlertConfig alertConfig = TestUtil.getMetricAlertConfig("src/test/resources/data/heartbeat/heartbeat.json");
        Long2BooleanMap heartbeatMap = TsdbV3ResultProcessor.processHeartBeatForSummaries(response, suppressMetricConfig,nodeId, alertConfig);

        heartbeatMap.forEach((key, val) -> assertFalse(val));
        assertEquals(heartbeatMap.size(), 1355);

    }

    @Test
    public void testProcessHeartBeatForNonSummaries() throws IOException, AlertException {

        final JsonNode root = parseJsonTree(TestUtil.loadResource("data/heartbeat/heartbeat_3.json"));

        final JsonNode thresholdNode = root.get(AlertConfigFields.THRESHOLD);

        final JsonNode suppressNode = thresholdNode.get(AlertConfigFields.SUPPRESS_METRIC);

        SuppressMetricConfig suppressMetricConfig =
                MetricAlertConfig.extractSuppressMetricConfig(suppressNode, root, 60L);
        String response = TestUtil.loadResource("data/heartbeat/heartbeat_response_non_summaries");

        MetricAlertConfig alertConfig = TestUtil.getMetricAlertConfig("src/test/resources/data/heartbeat/heartbeat.json");
        Long2BooleanMap heartbeatMap = TsdbV3ResultProcessor.processHeartbeatForNonSummaries(response, suppressMetricConfig, HEARTBEAT_THRESHOLD_NODE, alertConfig);
        assertEquals(heartbeatMap.size(), 1354);

        int count = 0;

        for (long key : heartbeatMap.keySet()) {
            if (heartbeatMap.get(key)) {
                count++;
            }
        }
        assertEquals(count, 3);
    }

}
