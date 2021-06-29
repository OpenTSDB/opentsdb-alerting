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

import net.opentsdb.horizon.alerts.state.AlertStateEntry;

public class NoopStatePersistor implements StatePersistor {

    private static final Logger LOG =
            LoggerFactory.getLogger(NoopStatePersistor.class);

    @Override
    public void persist(final AlertConfig config,
                        final Iterable<AlertStateEntry> stateStore,
                        final long runStampSec) {
        if (LOG.isDebugEnabled()) {
            for (AlertStateEntry entry : stateStore) {
                LOG.debug("persist: alert_id={}, run_stamp_sec={}, alert_state_entry={}",
                        config.getAlertId(), runStampSec, entry);
            }
        }
    }
}
