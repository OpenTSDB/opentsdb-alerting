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

package net.opentsdb.horizon.alerts.query.eventdb;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.impl.EventAlertConfig;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.model.EventAlertEvent;
import net.opentsdb.horizon.alerts.model.MonitorEvent;
import net.opentsdb.horizon.alerts.query.tsdb.TSDBClient;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.snooze.SnoozeFilter;
import net.opentsdb.horizon.alerts.state.AlertStateStore;

import it.unimi.dsi.fastutil.longs.LongIterator;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class EventProcessorTest {

    private static final long END_TIME_DAYS = 7L;

    @Injectable
    private TSDBClient tsdbClient;
    @Injectable
    private SnoozeFilter snoozeFilter;

    private String resource(final String name) {
        return TestUtil.loadResource("data/EventProcessorTest/" + name + ".json");
    }

    @Test
    void executeWithGroup() throws Exception {
        class MockSnoozeFilter extends MockUp<SnoozeFilter> {
            @Mock
            public void $init() { }

            @Mock
            public boolean snooze(MonitorEvent e, AlertConfig alertConfig) {
                return false;
            }
        }
        new MockSnoozeFilter();

        final EventAlertConfig config =
                (EventAlertConfig) AlertUtils.loadConfig(resource("group-config"));
        final AlertStateStore stateStore = config.createAlertStateStore();
        final EventProcessor processor = new EventProcessor(config, tsdbClient);
        processor.prepAndValidate(config, stateStore);
        new Expectations() {{
            tsdbClient.getResponse(withAny(""), withAny(1L));
            result = resource("group-response-1");
        }};

        stateStore.newRun();
        AlertEventBag alertBag =
                processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);

        // Response 1 State:
        // {"type": "n/a"} - BAD, raised alert
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 1);
            assertEquals(numberOfStates(stateStore), 1);

            final EventAlertEvent alert = (EventAlertEvent) alerts.get(0);
            assertEquals(alert.getCount(), 575);
            Assert.assertEquals(alert.getOriginSignal(), AlertState.GOOD);
            Assert.assertEquals(alert.getSignal(), AlertState.BAD);
            assertEquals(alert.getTags(),
                    new HashMap<String, String>() {{
                        put("type", "n/a");
                    }}
            );
        }

        new Expectations() {{
            tsdbClient.getResponse(withAny(""), withAny(1L));
            result = resource("group-response-2");
        }};

        stateStore.newRun();
        alertBag = processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);

        // Response 2 State:
        // {"type": "n/a"}     - BAD, no alert
        // {"type": "samoyed"} - BAD. alert raised
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 1);
            assertEquals(numberOfStates(stateStore), 2);

            final EventAlertEvent alert = (EventAlertEvent) alerts.get(0);
            assertEquals(alert.getCount(), 10);
            Assert.assertEquals(alert.getOriginSignal(), AlertState.GOOD);
            Assert.assertEquals(alert.getSignal(), AlertState.BAD);
            assertEquals(alert.getTags(),
                    new HashMap<String, String>() {{
                        put("type", "samoyed");
                    }}
            );
        }

        new Expectations() {{
            tsdbClient.getResponse(withAny(""), withAny(1L));
            result = resource("group-response-3");
        }};

        stateStore.newRun();
        alertBag = processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);

        // Response 3 State:
        // {"type": "n/a"}     - GOOD, recovery alert, removed from state.
        // {"type": "samoyed"} - GOOD, recovery alert, removed from state.
        //
        // Both states are cleared.
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 2);
            assertEquals(numberOfStates(stateStore), 0);
        }

        new Expectations() {{
            tsdbClient.getResponse(withAny(""), withAny(1L));
            result = resource("group-response-4");
        }};

        stateStore.newRun();
        alertBag = processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);

        // Response 4 State:
        // No state, no alerts.
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 0);
            assertEquals(numberOfStates(stateStore), 0);
        }

        new Expectations() {{
            tsdbClient.getResponse(withAny(""), withAny(1L));
            result = resource("group-response-5");
        }};

        stateStore.newRun();
        alertBag = processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);

        // Response 5 State:
        // {"type": "chihuahua"} - BAD, alert raised
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 1);
            assertEquals(numberOfStates(stateStore), 1);

            final EventAlertEvent alert = (EventAlertEvent) alerts.get(0);
            assertEquals(alert.getCount(), 20);
            Assert.assertEquals(alert.getOriginSignal(), AlertState.GOOD);
            Assert.assertEquals(alert.getSignal(), AlertState.BAD);
            assertEquals(alert.getTags(),
                    new HashMap<String, String>() {{
                        put("type", "chihuahua");
                    }}
            );
        }

        new Expectations() {{
            tsdbClient.getResponse(withAny(""), withAny(1L));
            result = resource("group-response-6");
        }};

        stateStore.newRun();
        alertBag = processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);

        // Response 6 State:
        // {"type": "chihuahua"} - GOOD, recovery alert, removed from state.
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 1);
            assertEquals(numberOfStates(stateStore), 0);

            final EventAlertEvent alert = (EventAlertEvent) alerts.get(0);
            assertEquals(alert.getCount(), 5);
            Assert.assertEquals(alert.getOriginSignal(), AlertState.BAD);
            Assert.assertEquals(alert.getSignal(), AlertState.GOOD);
            assertEquals(alert.getTags(),
                    new HashMap<String, String>() {{
                        put("type", "chihuahua");
                    }}
            );
        }
    }

    @Test
    void executeNoGroup() throws IOException, AlertException {
        class MockSnoozeFilter extends MockUp<SnoozeFilter> {
            @Mock
            public void $init() { }

            @Mock
            public boolean snooze(MonitorEvent e, AlertConfig alertConfig) {
                return false;
            }
        }
        new MockSnoozeFilter();
        final EventAlertConfig config =
                (EventAlertConfig) AlertUtils.loadConfig(resource("nogroup-config"));

        final AlertStateStore stateStore = config.createAlertStateStore();

        final EventProcessor processor = new EventProcessor(config, tsdbClient);
        processor.prepAndValidate(config, stateStore);

        new Expectations() {{
            tsdbClient.getResponse(withAny(""), withAny(1L));
            result = resource("nogroup-response-1");
        }};

        stateStore.newRun();
        AlertEventBag alertBag =
                processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);

        // Response 1 State:
        // No state, no alert.
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 0);
            assertEquals(numberOfStates(stateStore), 0);
        }

        new Expectations() {{
            tsdbClient.getResponse(withAny(""), withAny(1L));
            result = resource("nogroup-response-2");
        }};

        stateStore.newRun();
        alertBag = processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);

        // Response 2 State:
        // Stored state, alert raised.
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 1);
            assertEquals(numberOfStates(stateStore), 1);

            final EventAlertEvent alert = (EventAlertEvent) alerts.get(0);
            assertEquals(alert.getCount(), 552);
            Assert.assertEquals(alert.getOriginSignal(), AlertState.GOOD);
            Assert.assertEquals(alert.getSignal(), AlertState.BAD);
            assertEquals(alert.getTags(), Collections.emptyMap());
        }

        new Expectations() {{
            tsdbClient.getResponse(withAny(""), withAny(1L));
            result = resource("nogroup-response-3");
        }};

        stateStore.newRun();
        alertBag = processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);

        // Response 3 State:
        // Old state, no alert.
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 0);
            assertEquals(numberOfStates(stateStore), 1);
        }

        new Expectations() {{
            tsdbClient.getResponse(withAny(""), withAny(1L));
            result = resource("nogroup-response-4");
        }};

        stateStore.newRun();
        alertBag = processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);

        // Response 4 State:
        // No state, recovery alert.
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 1);
            assertEquals(numberOfStates(stateStore), 0);

            final EventAlertEvent alert = (EventAlertEvent) alerts.get(0);
            assertEquals(alert.getCount(), 0);
            Assert.assertEquals(alert.getOriginSignal(), AlertState.BAD);
            Assert.assertEquals(alert.getSignal(), AlertState.GOOD);
            assertEquals(alert.getTags(), Collections.emptyMap());
        }

        new Expectations() {{
            tsdbClient.getResponse(withAny(""), withAny(1L));
            result = resource("nogroup-response-5");
        }};

        stateStore.newRun();
        alertBag = processor.execute(END_TIME_DAYS, TimeUnit.DAYS, stateStore);

        // Response 5 State:
        // Stored state, alert raised
        {
            final List<AlertEvent> alerts = alertBag.getAlertEvents();
            assertEquals(alerts.size(), 1);
            assertEquals(numberOfStates(stateStore), 1);

            final EventAlertEvent alert = (EventAlertEvent) alerts.get(0);
            assertEquals(alert.getCount(), 20);
            Assert.assertEquals(alert.getOriginSignal(), AlertState.GOOD);
            Assert.assertEquals(alert.getSignal(), AlertState.BAD);
            assertEquals(alert.getTags(), Collections.emptyMap());
        }
    }

    private int numberOfStates(final AlertStateStore stateStore) {
        int count = 0;
        final LongIterator it = stateStore.getIteratorForStoredData();
        while (it.hasNext()) {
            count++;
            it.next();
        }
        return count;
    }
}
