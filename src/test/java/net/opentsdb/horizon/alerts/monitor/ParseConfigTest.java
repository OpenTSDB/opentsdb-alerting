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

package net.opentsdb.horizon.alerts.monitor;

import com.fasterxml.jackson.databind.JsonNode;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.AlertConfigFetcher;
import net.opentsdb.horizon.alerts.config.impl.FileConfigFetcher;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.config.impl.PartitionedConfigFetcher;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.enums.AlertType;
import net.opentsdb.horizon.alerts.enums.WindowSampler;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.processor.ControlledAlertExecutor;
import mockit.Expectations;
import mockit.Injectable;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

public class ParseConfigTest {

    @BeforeMethod
    public void setUp() {
        try {
            EnvironmentConfig.IS_LOCAL = true;
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    @Test
    public void runParseAlertConfig() {

        FileConfigFetcher fileConfigFetcher = new FileConfigFetcher(0);

        final Map<Long, AlertConfig> longAlertConfigMap = fileConfigFetcher.getAlertConfig();

        Assert.assertEquals(longAlertConfigMap.size(),6);

    }

    @Test
    public void runParseSnoozeConfig() throws IOException {



        Assert.assertNotNull(TestUtil.getSnoozeCoonfigFromFile("src/main/resources/snoozes/snooze_config.json"));
    }

    @Test
    public void runParseSingleMetricAlertConfig() throws IOException {

        final MetricAlertConfig metricAlertConfig = TestUtil.getMetricAlertConfig("src/main/resources/alerts/singleMetricConfig.json");

        Assert.assertEquals(metricAlertConfig.getAlertId(),7);
        Assert.assertEquals(metricAlertConfig.getNamespace(),"NS");
        Assert.assertEquals(metricAlertConfig.getLabels(),new ArrayList<String>() {{
            add("cpu");
        }});
        Assert.assertEquals(metricAlertConfig.isEnabled(),true);
        Assert.assertEquals(metricAlertConfig.getNagIntervalInSecs(),0);
        Assert.assertEquals(metricAlertConfig.isMissingEnabled(),true);
        Assert.assertEquals(metricAlertConfig.isHasBadThreshold(),true);
        Assert.assertEquals(metricAlertConfig.isHasWarnThreshold(),true);
        Assert.assertEquals(metricAlertConfig.isHasRecoveryThreshold(),true);

        Assert.assertEquals(metricAlertConfig.getBadThreshold(),60d);
        Assert.assertEquals(metricAlertConfig.getWarnThreshold(),55d);
        Assert.assertNotEquals(metricAlertConfig.getRecoveryThreshold(),0d);

        Assert.assertEquals(metricAlertConfig.getWindowSampler(), WindowSampler.AT_LEAST_ONCE);
        Assert.assertEquals(metricAlertConfig.getComparisonOperator(),">");
        Assert.assertEquals(metricAlertConfig.getComparisonString(),"above");
        Assert.assertEquals(metricAlertConfig.getFlippedComparisionOperator(),"<=");

    }

    @Injectable
    private AlertConfigFetcher alertConfigFetcher;

    @Test
    public void partitionedConfigFetcherTest() {

        //Pump in alert ids 0 - 9
        final Map<Long, AlertConfig> alertConfigMap = new HashMap<>();
        final long epochSecond = Instant.now().getEpochSecond();
        for (long i = 0; i < 10; i++) {

            alertConfigMap.put(i, new AlertConfig("ytest", AlertType.SIMPLE,
                    i, epochSecond) {
                @Override
                public void parseAlertSpecific(JsonNode jsonNode) {

                }

                @Override
                public <K extends AlertConfig> ControlledAlertExecutor<AlertEventBag, K> createAlertExecutor() {
                    return null;
                }

                @Override
                protected String getDefaultQueryType() {
                    return null;
                }

                @Override
                protected boolean validateConfig() throws AlertException {
                    return false;
                }

                @Override
                public AlertEvent createAlertEvent(long hash, String tsField, SortedMap<String, String> tags, AlertState alertType) {
                    return null;
                }
            });


        }


        final PartitionedConfigFetcher fetcher = PartitionedConfigFetcher.builder()
                .daemonid(0)
                .totalNumberOfDaemons(1)
                .mirrorid(1)
                .totalNumberOfMirrors(3)
                .mirrorSetId(0)
                .totalNumberMirrorSets(1)
                .alertConfigFetcher(alertConfigFetcher)
                .build();

        new Expectations() {{

            alertConfigFetcher.getAlertConfig();
            result = alertConfigMap;
            times = 1;

        }};
        final Map<Long, AlertConfig> finalConfig = fetcher.getAlertConfig();
        Assert.assertTrue(finalConfig.containsKey(1l));
        Assert.assertTrue(finalConfig.containsKey(4l));
        Assert.assertTrue(finalConfig.containsKey(7l));


    }

}
