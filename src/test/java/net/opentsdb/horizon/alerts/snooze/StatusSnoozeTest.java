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

import net.opentsdb.horizon.alerts.OutputWriter;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.SnoozeFetcher;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.model.Snooze;
import net.opentsdb.horizon.alerts.model.tsdb.Datum;
import net.opentsdb.horizon.alerts.model.tsdb.Tags;
import net.opentsdb.horizon.alerts.model.tsdb.YmsStatusEvent;
import net.opentsdb.horizon.alerts.processor.impl.StatusWriter;
import mockit.Expectations;
import mockit.Injectable;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import static net.opentsdb.horizon.alerts.processor.impl.StatusWriter.STATUS_SNOOZED_TAG;

public class StatusSnoozeTest {


    private static final String TEST_DATA_ROOT = "src/test/resources/data/SnoozeFilterTest";

    @Injectable
    SnoozeFetcher snoozeFetcher;

    @Test
    public void snoozeWithAlertIdLabelTagFilter() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-alertid-label-tag-filter.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        final String alertConfigPath_181 = TEST_DATA_ROOT + "/alertConfigs/alertid-181-label2.json";
        final String alertConfigPath_182 = TEST_DATA_ROOT + "/alertConfigs/alertid-182-label2.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        YmsStatusEvent statusEvent = new YmsStatusEvent();

        statusEvent.setData(
                Datum.newBuilder()
                        .withCluster("NS")
                .withTags(new Tags(
                        new HashMap<String, String>(){{
                            put("host", "proc.den.opentsdb.net");
                            put("Region", "ap-northeast-2");
                            put("AwsId", "01");
                        }}
                ))
                .build()
        );



        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long, Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        Accumulator accumulator = new Accumulator();

        StatusWriter statusWriter = new StatusWriter(alertConfig, snoozeFilter, accumulator);

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};



        statusWriter.process(statusEvent);
        System.out.println("Status: "+accumulator.statusEvents.get(0).getTags());
        Assert.assertTrue("Condition for id: "+ alertConfig.getAlertId(),
                Boolean.valueOf(accumulator.statusEvents.get(0).getTags().get(STATUS_SNOOZED_TAG)));


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        Accumulator accumulator1 = new Accumulator();
        StatusWriter statusWriter1 = new StatusWriter(alertConfig1, snoozeFilter, accumulator1);
        statusWriter1.process(statusEvent);
        Assert.assertTrue("Condition for id: "+ alertConfig1.getAlertId(),
                Boolean.valueOf(accumulator1.statusEvents.get(0).getTags().get(STATUS_SNOOZED_TAG)));

        final AlertConfig alertConfig2 = TestUtil.getMetricAlertConfig(alertConfigPath_181);
        Accumulator accumulator2 = new Accumulator();
        StatusWriter statusWriter2 = new StatusWriter(alertConfig2, snoozeFilter, accumulator2);
        statusWriter2.process(statusEvent);
        Assert.assertTrue("Condition for id: "+ alertConfig2.getAlertId(),
                Boolean.valueOf(accumulator2.statusEvents.get(0).getTags().get(STATUS_SNOOZED_TAG)));

        final AlertConfig alertConfig3 = TestUtil.getMetricAlertConfig(alertConfigPath_182);
        Accumulator accumulator3 = new Accumulator();
        StatusWriter statusWriter3 = new StatusWriter(alertConfig3, snoozeFilter, accumulator3);
        statusWriter3.process(statusEvent);
        Assert.assertFalse("Condition for id: "+ alertConfig3.getAlertId(),
                Boolean.valueOf(accumulator3.statusEvents.get(0).getTags().get(STATUS_SNOOZED_TAG)));

    }


    private class Accumulator implements OutputWriter {

        private final List<YmsStatusEvent> statusEvents = new ArrayList<>();

        @Override
        public void sendAlertEvent(AlertEventBag alertEventBag) {
            //Do nothing
        }

        @Override
        public void sendStatusEvent(YmsStatusEvent ymsStatusEvent) {
            statusEvents.add(ymsStatusEvent);
        }
    }
}
