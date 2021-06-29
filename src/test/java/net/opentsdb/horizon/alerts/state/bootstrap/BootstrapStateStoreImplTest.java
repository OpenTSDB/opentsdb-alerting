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

package net.opentsdb.horizon.alerts.state.bootstrap;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.state.AlertStateEntry;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import net.opentsdb.horizon.alerts.state.persistence.StatePersistor;
import net.opentsdb.horizon.alerts.state.persistence.AbstractStatePersistor;
import net.opentsdb.horizon.alerts.state.persistence.BatchDeserializer;

import com.esotericsoftware.kryo.io.Input;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class BootstrapStateStoreImplTest {

    private final Queue<byte[]> states = new ConcurrentLinkedQueue<>();

    private final StatePersistor persistor =
            new AbstractStatePersistor(
            ) {
                @Override
                protected void persist(byte[] payload) {
                    states.add(payload);
                }
            };


    @Test
    public void update() throws IOException {
        final AlertConfig config =
                AlertUtils.loadConfig(
                        TestUtil.loadResource("config/eventAlertWithGroup.json")
                );

        final AlertStateStore originalStateStore = config.createAlertStateStore();

        {
            originalStateStore.newRun();
            originalStateStore.raiseAlert(
                    "NS",
                    7L,
                    new TreeMap<String, String>() {{
                        put("hola", "zola");
                    }},
                    AlertState.BAD
            );
        }

        {
            originalStateStore.newRun();
            originalStateStore.raiseAlert(
                    "NS",
                    7L,
                    new TreeMap<String, String>() {{
                        put("hola", "zola");
                    }},
                    AlertState.WARN
            );

            originalStateStore.raiseAlert(
                    "NS",
                    7L,
                    new TreeMap<String, String>() {{
                        put("one", "one");
                        put("two", "two");
                    }},
                    AlertState.WARN
            );

            originalStateStore.updateDataPoint(
                    "NS",
                    7L,
                    new TreeMap<String, String>() {{
                        put("2", "0");
                        put("1", "9");
                    }},
                    2019L
            );
        }

        persistor.persist(config, originalStateStore, 200L);

        final BootstrapStateStore bootstrapper =
                BootstrapStateStore.create(
                        new BatchDeserializer(),
                        Collections.singletonList(config)
                );

        for (byte[] state : states) {
            final Input input = new Input(state);
            final byte version = input.readByte();
            assertEquals(version, (byte) 1);
            final long alertId = input.readLong();
            final long runStampSec = input.readLong();
            final String namespace = input.readString();

            bootstrapper.update(input, alertId, namespace, runStampSec);
        }

        final Optional<BootstrapStateEntry> maybeBootstrapEntry =
                bootstrapper.get(7L);
        assertTrue(maybeBootstrapEntry.isPresent());


        final BootstrapStateEntry bootstrapEntry = maybeBootstrapEntry.get();
        assertTrue(bootstrapEntry.hasComplete());
        assertFalse(bootstrapEntry.hasIncomplete());
        assertEquals(bootstrapEntry.getCompleteRunStamp(), 200L);

        final Set<AlertStateEntry> actual =
                getStateEntries(bootstrapEntry.getComplete());

        final Set<AlertStateEntry> expected =
                getStateEntries(originalStateStore);

        assertEquals(actual, expected);
    }

    private Set<AlertStateEntry> getStateEntries(
            final AlertStateStore stateStore) {
        final Set<AlertStateEntry> states = new HashSet<>();
        for (AlertStateEntry entry : stateStore) {
            states.add(entry);
        }
        return states;
    }
}
