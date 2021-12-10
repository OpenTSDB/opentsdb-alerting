/*
 *  This file is part of OpenTSDB.
 *  Copyright (C) 2021 Yahoo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.opentsdb.horizon.alerting.corona.processor.emitter.view;

import net.opentsdb.horizon.alerting.corona.model.alert.Comparator;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alert.WindowSampler;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricSimpleAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricAlertView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewsTest {

    @BeforeAll
    public static void setup()
    {
        Views.initialize(Views.config()
                .setHorizonUrl("https://horizon.example.com")
                .setSplunkUrl("https://splunk.example.com/splunk")
                .setSplunkIndex("test-index")
                .setSplunkLocale("ja_JP")
        );
    }

    @Test
    public void testOfSingleMetricSimpleAlert()
    {
        final SingleMetricSimpleAlert given =
                SingleMetricSimpleAlert.builder()
                        .setIsSnoozed(true)
                        .setSampler(WindowSampler.AT_LEAST_ONCE)
                        .setTimestampSec(123123)
                        .setComparator(Comparator.EQUALS)
                        .setDetails("Not used")
                        .setId(1)
                        .setIsNag(false)
                        .setMetric("metric")
                        .setNamespace("namespace")
                        .setState(State.GOOD)
                        .setStateFrom(State.BAD)
                        .setThreshold(10)
                        .setTimestampsSec(1, 2, 3, 4, 5)
                        .setValuesInWindow(5, 4, 3, 2, 1)
                        .setWindowSizeSec(60)
                        .addTag("host", "host")
                        .addTag("colo", "colo")
                        .build();

        final SingleMetricAlertView expectedView =
                SingleMetricAlertView.builder()
                        .setComparator("=")
                        .setIsSnoozed(true)
                        .setNamespace("namespace")
                        .setStateFrom("bad")
                        .setStateTo("good")
                        .setType(ViewType.RECOVERY)
                        .setTagsAndSort(new HashMap<String, String>() {{
                            put("host", "host");
                            put("colo", "colo");
                        }})
                        .setTimestampMs(123123000)
                        .setTimestampsSec(1, 2, 3, 4, 5)
                        .setDisplayValues(5, 4, 3, 2, 1)
                        .setEvaluationWindowMin(1)
                        .setThreshold(10)
                        .setTimeSampler("at least once")
                        .setMetric("metric")
                        .setMetricValue(1)
                        .build();

        final SingleMetricAlertView actual = Views.of(given);

        assertEquals(expectedView, actual);
    }

    @Test
    void alertEditUrl()
    {
        assertEquals("https://horizon.example.com/a/9876543210/edit", Views.get().alertEditUrl(9876543210L));
    }

    @Test
    void alertViewUrl()
    {
        assertEquals("https://horizon.example.com/a/9876543210/view", Views.get().alertViewUrl(9876543210L));
    }

    @Test
    void alertSplunkUrl()
    {
        assertEquals("https://splunk.example.com/splunk/ja_JP/app/search/search?q=search%20index%3Dtest-index%20alert_id%3D9876543210", Views.get().alertSplunkUrl(9876543210L));
    }

    @Test
    void testAlertSplunkUrl()
    {
        assertEquals("https://splunk.example.com/splunk/ja_JP/app/search/search?q=search+index%3Dtest-index+alert_id%3D9876543210+earliest%3D12%2F01%2F2021%3A11%3A55%3A00UTC+latest%3D12%2F01%2F2021%3A12%3A10%3A00UTC+timeformat%3D%25m%2F%25d%2F%25Y%3A%25H%3A%25M%3A%25S%25Z", Views.get().alertSplunkUrl(9876543210L, 1638360000000L));
    }
}