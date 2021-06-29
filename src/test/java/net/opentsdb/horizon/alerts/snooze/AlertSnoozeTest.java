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

package net.opentsdb.horizon.alerts.snooze;

import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.SnoozeFetcher;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.model.Snooze;
import mockit.Expectations;
import mockit.Injectable;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AlertSnoozeTest {

    private static final String TEST_DATA_ROOT = "src/test/resources/data/SnoozeFilterTest";

    @Injectable
    SnoozeFetcher snoozeFetcher;

    @Test
    public void snoozeOnAlertId() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-alertid.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        SnoozeAlert snoozeAlert = new SnoozeAlert(snoozeFilter);

        AlertEvent alertEvent = new AlertEvent() {

            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "supervisor-l-1.yms.den.yahoo.com");
                }};
            }
        };


        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long, Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        snoozeAlert.process(new AlertEventBag(Collections.singletonList(alertEvent), alertConfig));


        Assert.assertTrue("Condition for id: "+ alertConfig.getAlertId(),
                alertEvent.isSnoozed());


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        alertEvent.setSnoozed(false);

        snoozeAlert.process(new AlertEventBag(Collections.singletonList(alertEvent), alertConfig1));

        Assert.assertFalse("Condition for id: "+ alertConfig1.getAlertId(),
                alertEvent.isSnoozed());

    }

}
