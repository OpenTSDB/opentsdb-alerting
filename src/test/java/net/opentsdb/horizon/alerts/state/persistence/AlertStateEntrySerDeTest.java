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

package net.opentsdb.horizon.alerts.state.persistence;

import java.util.SortedMap;
import java.util.TreeMap;

import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.state.AlertStateEntry;
import net.opentsdb.horizon.alerts.state.impl.AlertStateEntryImpl;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class AlertStateEntrySerDeTest {

    @Test
    public void writeRead() {
        final SortedMap<String, String> tags =
                new TreeMap<String, String>() {{
                    put("hello", "world");
                    put("so", "cliche");
                }};

        final AlertStateEntry original =
                new AlertStateEntryImpl(
                        1L,
                        tags,
                        AlertState.WARN,
                        AlertState.GOOD,
                        1945L,
                        15L
                );

        final Output output = new Output(1024);
        AlertStateEntrySerDe.write(output, original);

        final Input input = new Input(output.toBytes());
        final AlertStateEntry actual = AlertStateEntrySerDe.read(input);

        assertEquals(actual, original);
        assertEquals(actual.getStateId(), 1L);
        assertEquals(actual.getTags(), tags);
        assertEquals(actual.getCurrentState(), AlertState.WARN);
        assertEquals(actual.getPreviousState(), AlertState.GOOD);
        assertEquals(actual.getLastSeenTimestamp(), 1945L);
        assertEquals(actual.getNagInterval(), 15L);
    }
}
