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

package net.opentsdb.horizon.alerts.model;

import java.util.HashMap;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.opentsdb.horizon.alerts.enums.AlertState;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class EventAlertEventTest {

    @Test
    public void testReadWrite()
    {
        final EventAlertEvent event = new EventAlertEvent();
        event.setWindowSizeSec(300);
        event.setThreshold(17);
        event.setFilterQuery("*:*");
        event.setEvent(null); // Event doesn't have equals and hashCode =(
        event.setDataNamespace("Jupyter");
        event.setCount(9);
        event.setTags(new HashMap<String, String>() {{
            put("host", "localhost");
            put("app", "the-beast");
        }});
        event.setOriginSignal(AlertState.BAD);
        event.setCurrentSignal(AlertState.GOOD);
        event.setNag(false);
        event.setAlertRaisedTimestamp(1796);
        event.setAlertId(171);
        event.setAlertHash(9090);

        final Output output = new Output(1024, -1);
        event.write(null, output);

        final Input input = new Input(output.toBytes());
        final EventAlertEvent newEvent = new EventAlertEvent();
        newEvent.read(null, input);

        assertEquals(event, newEvent);
    }
}
