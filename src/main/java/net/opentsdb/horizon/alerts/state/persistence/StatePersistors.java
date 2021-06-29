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

import java.util.Collections;
import java.util.List;

public class StatePersistors {

    private static volatile int NUMBER_OF_PERSISTORS;

    private static volatile List<StatePersistor> PERSISTORS;

    private static final StatePersistor NOOP = new NoopStatePersistor();

    public static void initialize(final List<StatePersistor> persistors) {
        NUMBER_OF_PERSISTORS = persistors.size();
        PERSISTORS = Collections.unmodifiableList(persistors);
    }

    /*
     * TODO: The sole reason for this to exist is not to create a
     *       persistor per alert config. I do not see how easily set
     *       them up in the current code structure.
     */
    public static StatePersistor getDefault(final long alertId) {
        if (PERSISTORS == null) {
            return NOOP;
        }
        return PERSISTORS.get(
                Long.valueOf(alertId).hashCode() % NUMBER_OF_PERSISTORS
        );
    }
}
