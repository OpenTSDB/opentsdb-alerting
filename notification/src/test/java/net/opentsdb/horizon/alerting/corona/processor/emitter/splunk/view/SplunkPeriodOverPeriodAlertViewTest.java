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
import net.opentsdb.horizon.alerting.corona.model.alert.impl.PeriodOverPeriodAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.LogTime;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodAlertView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplunkPeriodOverPeriodAlertViewTest {

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
                        AlertType.PERIOD_OVER_PERIOD,
                        Arrays.asList("alerts@opentsdb.net", "opsgenie-test"),
                        "Alert Subject",
                        "Alert Body",
                        "opentsdb.net/a/42/view"
                );

        final PeriodOverPeriodAlertView innerView = Views.of(
                (PeriodOverPeriodAlert) TestData.getPeriodOverPeriodAlerts().get(0)
        );
        final SplunkPeriodOverPeriodAlertView view =
                new SplunkPeriodOverPeriodAlertView(sharedData, innerView);

        final String expected = "{" +
                "\"_logged_at\":\"Thu Jan 1, 1970 12:00:00 AM UTC\"," +
                "\"ts\":\"Tue Nov 26, 2019 09:25:00 AM UTC\"," +
                "\"namespace\":\"OpenTSDB\"," +
                "\"alert_id\":42," +
                "\"state_from\":\"bad\"," +
                "\"state_to\":\"good\"," +
                "\"description\":\"Observed value 5.000000 within 3.000000 and NaN (2.000000 units below and 4.000000% above predicted 10.000000)\"," +
                "\"url\":\"opentsdb.net/a/42/view\"," +
                "\"snoozed\":false," +
                "\"nag\":false," +
                "\"tags\":{\"emitter\":\"oc\",\"host\":\"localhost\"}," +
                "\"contacts\":[\"alerts@opentsdb.net\",\"opsgenie-test\"]," +
                "\"subject\":\"Alert Subject\"," +
                "\"body\":\"Alert Body\"," +
                "\"alert_type\":\"PERIOD_OVER_PERIOD\"," +
                "\"metric\":\"<unknown>\"," +
                "\"observed_value\":5.0," +
                "\"predicted_value\":10.0," +
                "\"lower_bad_threshold\":\"3.0 units\"," +
                "\"lower_bad_threshold_value\":3.0," +
                "\"lower_warn_threshold\":\"2.0 units\"," +
                "\"lower_warn_threshold_value\":3.0," +
                "\"upper_warn_threshold\":\"4.0%\"," +
                "\"upper_warn_threshold_value\":\"NaN\"," +
                "\"upper_bad_threshold\":\"3.0%\"," +
                "\"upper_bad_threshold_value\":\"NaN\"" +
                "}";

        assertEquals(expected, OBJECT_MAPPER.writeValueAsString(view));
    }

}
