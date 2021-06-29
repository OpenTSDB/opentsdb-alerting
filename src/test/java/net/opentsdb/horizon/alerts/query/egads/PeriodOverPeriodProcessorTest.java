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

package net.opentsdb.horizon.alerts.query.egads;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.opentsdb.horizon.alerts.config.SnoozeFetcher;
import net.opentsdb.horizon.alerts.model.Snooze;
import net.opentsdb.horizon.alerts.snooze.SnoozeAlert;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.model.MonitorEvent;
import net.opentsdb.horizon.alerts.snooze.SnoozeFilter;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.config.impl.PeriodOverPeriodAlertConfig;
import net.opentsdb.horizon.alerts.http.CollectorWriter;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.processor.impl.StatusWriter;
import net.opentsdb.horizon.alerts.query.tsdb.TSDBClient;
import net.opentsdb.horizon.alerts.state.AlertStateStore;

public class PeriodOverPeriodProcessorTest {
    private static final long END_TIME_DAYS = 7L;

    @Injectable
    private TSDBClient tsdbClient;
    @Injectable
    private StatusWriter statusWriter;
    @Injectable
    private CollectorWriter collectorWriter;

    private static String resource(final String name) {
        return TestUtil.loadResource("data/PeriodOverPeriodProcessorTest/" + name + ".json");
    }

    @Test
    void execute_NormalFlow() throws IOException, AlertException {
        class MockSnoozeFilter extends MockUp<SnoozeFilter> {
            @Mock
            public void $init() { }

            @Mock
            public boolean snooze(MonitorEvent e, AlertConfig alertConfig) {
                return false;
            }
        }
        new MockSnoozeFilter();

        final PeriodOverPeriodAlertConfig config = (PeriodOverPeriodAlertConfig)
                AlertUtils.loadConfig(resource("egads-config"));
        final AlertStateStore stateStore = config.createAlertStateStore();

        final PeriodOverPeriodProcessor processor =
                new PeriodOverPeriodProcessor(
                        config,
                        tsdbClient,
                        statusWriter,
                        collectorWriter
                );
        processor.prepAndValidate(config, stateStore);

        new Expectations() {{
            tsdbClient.getResponse(withSubstring("EVALUATE"), withAny(1L));
            result = resource("egads-response");

            // Priming call
            tsdbClient.getResponse(withSubstring("PREDICT"), withAny(1L));
            result = "";
        }};

        stateStore.newRun();
        AlertEventBag alertBag =
                processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 5);
        }
    }

    @Test
    void execute_EmptyData() throws IOException, AlertException {
        class MockSnoozeFilter extends MockUp<SnoozeFilter> {
            @Mock
            public void $init() { }

            @Mock
            public boolean snooze(MonitorEvent e, AlertConfig alertConfig) {
                return false;
            }
        }
        new MockSnoozeFilter();
        
        final PeriodOverPeriodAlertConfig config = (PeriodOverPeriodAlertConfig)
                AlertUtils.loadConfig(resource("egads-config"));
        final AlertStateStore stateStore = config.createAlertStateStore();

        final PeriodOverPeriodProcessor processor =
                new PeriodOverPeriodProcessor(
                        config,
                        tsdbClient,
                        statusWriter,
                        collectorWriter
                );
        processor.prepAndValidate(config, stateStore);

        new Expectations() {{
            tsdbClient.getResponse(withSubstring("EVALUATE"), withAny(1L));
            result = resource("egads-response-empty-data");

            // Priming call
            tsdbClient.getResponse(withSubstring("PREDICT"), withAny(1L));
            result = "";
        }};

        stateStore.newRun();
        AlertEventBag alertBag =
                processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 0);
        }
    }

    @Injectable
    SnoozeFetcher snoozeFetcher;

    @Test
    void execute_NormalFlow_WithSnooze() throws IOException, AlertException {
        final PeriodOverPeriodAlertConfig config = (PeriodOverPeriodAlertConfig)
                AlertUtils.loadConfig(resource("egads-config"));
        final AlertStateStore stateStore = config.createAlertStateStore();

        final PeriodOverPeriodProcessor processor =
                new PeriodOverPeriodProcessor(
                        config,
                        tsdbClient,
                        statusWriter,
                        collectorWriter
                );
        processor.prepAndValidate(config, stateStore);
        final long alertId = config.getAlertId();
        new Expectations() {{
            tsdbClient.getResponse(withSubstring("EVALUATE"), alertId);
            result = resource("egads-response");

            // Priming call
            tsdbClient.getResponse(withSubstring("PREDICT"), alertId);
            result = "";
        }};

        stateStore.newRun();
        final int EXPECTED_ALERT_COUNT = 5;
        AlertEventBag alertBag =
                processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), EXPECTED_ALERT_COUNT);
        }

        //Snooze
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);
        SnoozeAlert snoozeAlert = new SnoozeAlert(snoozeFilter);
        final Map<Long, Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        AlertUtils.jsonMapper.readValue(
                                resource("snooze-config"),
                                Snooze.class)
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        snoozeAlert.process(alertBag);
        {
            final int input_events_size = alertBag.getAlertEvents().size();
            assertEquals(input_events_size, EXPECTED_ALERT_COUNT,
                    "Snoozing altered the number of events from: "
                            + EXPECTED_ALERT_COUNT + " to "+ input_events_size);
            final List<AlertEvent> events = alertBag.getAlertEvents()
                    .stream()
                    .filter(e -> !e.isSnoozed())
                    .collect(Collectors.toList());
            
            assertTrue(events.isEmpty(), "Some events not snoozed: "+ events.toString());
        }

    }
}
