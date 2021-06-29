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

import net.opentsdb.horizon.alerts.config.AlertConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerts.state.impl.AlertStateStoreImpl;
import net.opentsdb.horizon.alerts.state.AlertStateEntry;
import net.opentsdb.horizon.alerts.state.ModifiableAlertStateStore;

import com.esotericsoftware.kryo.io.Input;

public class BatchDeserializer implements Deserializer {

    private static final Logger LOG =
            LoggerFactory.getLogger(BatchDeserializer.class);

    @Override
    public ModifiableAlertStateStore initialize(final Input input,
                                                final AlertConfig config) {
        return new AlertStateStoreImpl(
                String.valueOf(config.getAlertId()),
                config.getNagIntervalInSecs(),
                config.getTransitionConfig(),
                config.storeIdentity()
        );
    }

    @Override
    public void update(final Input input,
                       final ModifiableAlertStateStore stateStore) {
        while (input.readBoolean()) {
            final AlertStateEntry entry = AlertStateEntrySerDe.read(input);
            stateStore.put(entry);
        }
    }

    @Override
    public void finalize(final Input input,
                         final ModifiableAlertStateStore stateStore) {
        final int expectedCount = input.readInt();
        final int actual = stateStore.size(); // Expensive operation.
        if (expectedCount != actual) {
            LOG.error("State count mismatch: expected={}, actual={}.",
                    expectedCount, actual);
        }
    }
}
