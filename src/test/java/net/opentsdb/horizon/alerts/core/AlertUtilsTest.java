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

package net.opentsdb.horizon.alerts.core;

import net.opentsdb.horizon.alerts.AlertException;
import org.testng.annotations.Test;

import static net.opentsdb.horizon.alerts.AlertUtils.getIntervalAsInt;
import static org.testng.AssertJUnit.assertEquals;

public class AlertUtilsTest {

    private long start = 1596819856;
    private long end = 1596841456;

    @Test
    public void parseDurationMinTest() throws AlertException {
        assertEquals(360 * 60, getIntervalAsInt("360min", start, end));
        assertEquals(60 * 60, getIntervalAsInt("60min", start, end));
        assertEquals(10 * 60, getIntervalAsInt("10m", start, end));
        assertEquals(60 * 60, getIntervalAsInt("60min", start, end));
    }

    @Test
    public void parseDurationDayTest() throws AlertException {
        assertEquals(360 * 60 * 60 * 24, getIntervalAsInt("360day", start, end));
        assertEquals(60 * 60 * 60 * 24, getIntervalAsInt("60day", start, end));
        assertEquals(10 * 60 * 60 * 24, getIntervalAsInt("10d", start, end));
        assertEquals(60 * 60 * 60 * 24, getIntervalAsInt("60day", start, end));
    }

    @Test
    public void parseDurationHourTest() throws AlertException {
        assertEquals(360 * 60 * 60, getIntervalAsInt("360h", start, end));
        assertEquals(60 * 60 * 60, getIntervalAsInt("60h", start, end));
        assertEquals(10 * 60 * 60, getIntervalAsInt("10hr", start, end));
        assertEquals(60 * 60 * 60, getIntervalAsInt("60hr", start, end));
    }

    @Test
    public void parseDurationSecTest() throws AlertException {
        assertEquals(360, getIntervalAsInt("360s", start, end));
        assertEquals(60, getIntervalAsInt("60s", start, end));
        assertEquals(10, getIntervalAsInt("10secs", start, end));
        assertEquals(60, getIntervalAsInt("60secs", start, end));
    }

    @Test
    public void parseDuration0AllTest() throws AlertException {
        assertEquals(end - start, getIntervalAsInt("0all", start, end));
    }
}
