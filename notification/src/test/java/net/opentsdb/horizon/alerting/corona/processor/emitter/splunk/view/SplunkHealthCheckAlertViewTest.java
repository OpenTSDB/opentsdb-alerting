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
import net.opentsdb.horizon.alerting.corona.model.alert.impl.HealthCheckAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.LogTime;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthCheckAlertView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplunkHealthCheckAlertViewTest {

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
                        AlertType.HEALTH_CHECK,
                        Arrays.asList("alerts@opentsdb.net", "opsgenie-test"),
                        "Alert Subject {{status_message}}",
                        "Alert Body {{env}} {{status_message}}",
                        "opentsdb.net/a/42/view"
                );

        final HealthCheckAlertView innerView = Views.of(
                (HealthCheckAlert) TestData.getHealthCheckAlerts().get(0)
        );
        final SplunkHealthCheckAlertView view =
                new SplunkHealthCheckAlertView(sharedData, innerView);

        final String expected = "{" +
                "\"_logged_at\":\"Thu Jan 1, 1970 12:00:00 AM UTC\"," +
                "\"ts\":\"Tue Jul 23, 2019 06:57:42 PM UTC\"," +
                "\"namespace\":\"OpenTSDB\"," +
                "\"alert_id\":42," +
                "\"state_from\":\"good\"," +
                "\"state_to\":\"bad\"," +
                "\"description\":\"test.namespace: test.application was in the bad state for at least 9 times.\"," +
                "\"url\":\"opentsdb.net/a/42/view\"," +
                "\"snoozed\":false," +
                "\"nag\":false," +
                "\"tags\":{\"env\":\"prod\",\"host\":\"localhost\"}," +
                "\"contacts\":[\"alerts@opentsdb.net\",\"opsgenie-test\"]," +
                "\"subject\":\"Alert Subject Very detailed info\"," +
                "\"body\":\"Alert Body prod Very detailed info\"," +
                "\"alert_type\":\"HEALTH_CHECK\"," +
                "\"check_namespace\":\"test.namespace\"," +
                "\"check_application\":\"test.application\"," +
                "\"check_message\":\"Very detailed info\"" +
                "}";

        assertEquals(expected, OBJECT_MAPPER.writeValueAsString(view));
    }
}
