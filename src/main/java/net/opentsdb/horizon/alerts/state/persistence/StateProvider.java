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

import java.util.Optional;

import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.state.AlertStateStore;

/**
 * An implementation of this interface should have a corresponding
 * implementation of {@link StatePersistor}.
 */
public interface StateProvider {

    /**
     * Get alert state store for the given configuration.
     *
     * @param config alert configuration.
     */
    Optional<AlertStateStore> get(AlertConfig config);

}
