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

package net.opentsdb.horizon.alerts.query;

import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.processor.impl.StatusWriter;
import net.opentsdb.horizon.alerts.query.tsdb.TSDBClient;
import net.opentsdb.horizon.alerts.query.tsdb.TSDBV3SlidingWindowQuery;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class HeartBeatE2ETest {

    @Test
    public void testHeartbeatQuery_NonSummary() throws AlertException {

        MetricAlertConfig alertConfig = TestUtil.getMetricAlertConfig("src/test/resources/data/heartbeat/heartbeat_non_summary.json");
        TSDBClient tsdbClient = mock(TSDBClient.class);
        when(tsdbClient.getResponse(anyString(), anyLong())).thenReturn(TestUtil.loadResource("data/heartbeat/heartbeat_response_non_summaries"));
        AlertStateStore alertStateStore = TestUtil.createStateStore(alertConfig);
        StatusWriter statusWriter = mock(StatusWriter.class);
        TSDBV3SlidingWindowQuery tsdbv3SlidingWindowQuery = new TSDBV3SlidingWindowQuery(alertConfig, tsdbClient, statusWriter);

        tsdbv3SlidingWindowQuery.prepAndValidate(alertConfig, alertStateStore);

        AlertEventBag alertEventBag = tsdbv3SlidingWindowQuery.execute(System.currentTimeMillis(), TimeUnit.MILLISECONDS, alertStateStore);
        List<AlertEvent> alertEvents = alertEventBag.getAlertEvents();

        assertEquals(alertEvents.size(), 2);

        assertFalse(alertEvents.get(0).getTags().get("host").equalsIgnoreCase("alertengine-2.yms.bf2.yahoo.com"));
        assertFalse(alertEvents.get(1).getTags().get("host").equalsIgnoreCase("alertengine-2.yms.bf2.yahoo.com"));
    }

    @Test
    public void testHeartbeatQuery_Summary() throws AlertException {

        MetricAlertConfig alertConfig = TestUtil.getMetricAlertConfig("src/test/resources/data/heartbeat/heartbeat_3.json");
        TSDBClient tsdbClient = mock(TSDBClient.class);
        when(tsdbClient.getResponse(anyString(), anyLong())).thenReturn(TestUtil.loadResource("data/heartbeat/heartbeat_response_2.json"));
        StatusWriter statusWriter = mock(StatusWriter.class);

        AlertStateStore alertStateStore = TestUtil.createStateStore(alertConfig);

        TSDBV3SlidingWindowQuery tsdbv3SlidingWindowQuery = new TSDBV3SlidingWindowQuery(alertConfig, tsdbClient, statusWriter);

        tsdbv3SlidingWindowQuery.prepAndValidate(alertConfig, alertStateStore);

        AlertEventBag alertEventBag = tsdbv3SlidingWindowQuery.execute(System.currentTimeMillis(), TimeUnit.MILLISECONDS, alertStateStore);
        List<AlertEvent> alertEvents = alertEventBag.getAlertEvents();

        assertEquals(alertEvents.size(), 2);

        assertFalse(alertEvents.get(0).getTags().get("host").equalsIgnoreCase("alertengine-2.yms.bf2.yahoo.com"));
        assertFalse(alertEvents.get(1).getTags().get("host").equalsIgnoreCase("alertengine-2.yms.bf2.yahoo.com"));
    }

    @Test
    public void testHeartbeatQuery_Summary_Missing() throws AlertException {

        MetricAlertConfig alertConfig = TestUtil.getMetricAlertConfig("src/test/resources/data/heartbeat/heartbeat.json");
        TSDBClient tsdbClient = mock(TSDBClient.class);
        when(tsdbClient.getResponse(anyString(), anyLong())).thenReturn(TestUtil.loadResource("data/heartbeat/heartbeat_response_missing.json"));
        StatusWriter statusWriter = mock(StatusWriter.class);

        AlertStateStore alertStateStore = TestUtil.createStateStore(alertConfig);

        TSDBV3SlidingWindowQuery tsdbv3SlidingWindowQuery = new TSDBV3SlidingWindowQuery(alertConfig, tsdbClient, statusWriter);

        tsdbv3SlidingWindowQuery.prepAndValidate(alertConfig, alertStateStore);

        AlertEventBag alertEventBag = tsdbv3SlidingWindowQuery.execute(System.currentTimeMillis(), TimeUnit.MILLISECONDS, alertStateStore);
        List<AlertEvent> alertEvents = alertEventBag.getAlertEvents();

        assertEquals(alertEvents.size(), 2);

        assertFalse(alertEvents.get(0).getTags().get("host").equalsIgnoreCase("alertengine-2.yms.bf2.yahoo.com"));
        assertFalse(alertEvents.get(1).getTags().get("host").equalsIgnoreCase("alertengine-2.yms.bf2.yahoo.com"));
    }
}
