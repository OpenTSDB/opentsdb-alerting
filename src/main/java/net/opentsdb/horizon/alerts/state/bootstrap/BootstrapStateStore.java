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

import java.util.List;
import java.util.Optional;

import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.state.persistence.Deserializer;

import com.esotericsoftware.kryo.io.Input;

public interface BootstrapStateStore {

    void update(Input input, long alertId, String namespace, long runStampSec);

    Optional<BootstrapStateEntry> get(long alertId);

    void remove(long alertId);

    static BootstrapStateStore create(final Deserializer deserializer,
                                      final List<AlertConfig> configs) {
        return new BootstrapStateStoreImpl(deserializer, configs);
    }
}
