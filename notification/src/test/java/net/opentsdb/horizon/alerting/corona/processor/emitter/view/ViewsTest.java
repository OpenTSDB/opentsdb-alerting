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
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewsTest {

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
}