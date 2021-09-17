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

package net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockit.Mock;
import mockit.MockUp;
import net.opentsdb.horizon.alerting.corona.TestData;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.LogTime;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricAlertView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplunkSingleMetricAlertViewTest {

    static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeEach
    void stubStaticMethods() {
        new MockUp<LogTime>() {
            @Mock
            public String get() {
                return LogTime.DATE_FORMAT.format(new Date(0));
            }
        };
    }

    @Test
    public void testJson() throws JsonProcessingException {
        final SplunkViewSharedData sharedData =
                new SplunkViewSharedData(
                        42L,
                        "OpenTSDB",
                        AlertType.SINGLE_METRIC,
                        Arrays.asList("alerts@opentsdb.net", "opsgenie-test"),
                        "Alert Subject",
                        "Alert Body",
                        "opentsdb.net/a/42/view"
                );

        final SingleMetricAlertView innerView = Views.of(
                (SingleMetricAlert) TestData.getSingleMetricAlerts().get(0)
        );
        final SplunkSingleMetricAlertView view =
                new SplunkSingleMetricAlertView(sharedData, innerView);

        final String expected = "{" +
                "\"_logged_at\":\"Thu Jan 1, 1970 12:00:00 AM UTC\"," +
                "\"ts\":\"Tue Jul 23, 2019 06:57:42 PM UTC\"," +
                "\"namespace\":\"OpenTSDB\"," +
                "\"alert_id\":42," +
                "\"state_from\":\"good\"," +
                "\"state_to\":\"bad\"," +
                "\"description\":\"cpu.utilization.pct > 9.000000 at least once in the last 0 minutes\"," +
                "\"url\":\"opentsdb.net/a/42/view\"," +
                "\"snoozed\":true," +
                "\"nag\":false," +
                "\"tags\":{\"env\":\"prod\",\"host\":\"localhost\"}," +
                "\"contacts\":[\"alerts@opentsdb.net\",\"opsgenie-test\"]," +
                "\"subject\":\"Alert Subject\"," +
                "\"body\":\"Alert Body\"," +
                "\"alert_type\":\"SINGLE_METRIC\"," +
                "\"metric\":\"cpu.utilization.pct\"," +
                "\"value\":89.0" +
                "}";

        assertEquals(expected, OBJECT_MAPPER.writeValueAsString(view));
    }
}
