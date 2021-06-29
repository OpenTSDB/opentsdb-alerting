package net.opentsdb.horizon.alerts.config.impl;

import com.fasterxml.jackson.databind.JsonNode;
import net.opentsdb.horizon.alerts.config.AlertConfigFields;
import net.opentsdb.horizon.alerts.config.SuppressMetricConfig;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.enums.ComparatorType;
import net.opentsdb.horizon.alerts.enums.WindowSampler;
import org.testng.annotations.Test;

import java.io.IOException;

import static net.opentsdb.horizon.alerts.AlertUtils.parseJsonTree;
import static net.opentsdb.horizon.alerts.enums.ComparatorType.MISSING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MetricAlertConfigTest {
    
    @Test
    void testParse_MissingSuppressMetric() throws IOException {

        final JsonNode root = parseJsonTree(TestUtil.loadResource("data/heartbeat/heartbeat.json"));

        final JsonNode thresholdNode = root.get(AlertConfigFields.THRESHOLD);
        
        final JsonNode suppressNode = thresholdNode.get(AlertConfigFields.SUPPRESS_METRIC);

        SuppressMetricConfig suppressMetricConfig = 
                MetricAlertConfig.extractSuppressMetricConfig(suppressNode, root, 300L);
        assertEquals("q2_m1_groupby", suppressMetricConfig.getMetricId());
        assertEquals(60, suppressMetricConfig.getReportingInterval());
        assertEquals(MISSING, suppressMetricConfig.getComparatorType());
        assertEquals(WindowSampler.SUMMARY, suppressMetricConfig.getSampler());
        assertNull(suppressMetricConfig.getThreshold()); // because it is type missing 
    }

    @Test
    void testParse_SuppressMetric() throws IOException {

        final JsonNode root = parseJsonTree(TestUtil.loadResource("data/heartbeat/heartbeat_2.json"));

        final JsonNode thresholdNode = root.get(AlertConfigFields.THRESHOLD);

        final JsonNode suppressNode = thresholdNode.get(AlertConfigFields.SUPPRESS_METRIC);

        SuppressMetricConfig suppressMetricConfig = 
                MetricAlertConfig.extractSuppressMetricConfig(suppressNode, root,  300L);
        assertEquals("q2_m1_groupby", suppressMetricConfig.getMetricId());
        assertEquals(120, suppressMetricConfig.getReportingInterval());
        assertEquals(ComparatorType.LESS_THAN_OR_EQUALS, suppressMetricConfig.getComparatorType());
        assertEquals(WindowSampler.ALL_OF_THE_TIMES, suppressMetricConfig.getSampler());
        assertEquals(Double.valueOf(5000), suppressMetricConfig.getThreshold());
    }

}
