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

package net.opentsdb.horizon.alerts.state.purge;

import net.opentsdb.horizon.alerts.Monitoring;
import net.opentsdb.horizon.alerts.state.AlertStateEntry;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

@Slf4j
public class PurgeUtil {


    public static int doPurge(final AlertStateStore alertStateStore,
                              final boolean firstPurge,
                              final long purgeDate,
                              final String[] tags,
                              final long alertId) {
        int purged = 0;
        final Iterator<AlertStateEntry> iterator = alertStateStore.iterator();

        while (iterator.hasNext()) {

            final AlertStateEntry entry = iterator.next();
            if(entry.getLastSeenTimestamp() > 0
                    || firstPurge) {
                if (entry.getLastSeenTimestamp() <= purgeDate) {
                    //Time to remove - yay!
                    log.debug(
                             "id: {} " +
                             "purging: {} " +
                             "lastseen: {} " +
                             "purgeDate {}",
                              alertId,
                              entry.getStateId(),
                              entry.getLastSeenTimestamp(),
                              purgeDate);
                    purged++;
                    iterator.remove();
                }
            } else {

                //Should not happen!
                log.error(
                        "id: {} bad purge for {} {}",
                        alertId,
                        entry.getStateId(),
                        entry.getLastSeenTimestamp()
                );

                Monitoring.get().countErrorPurge(purged, tags);

            }
        }

        log.info("id: {} Purged: {}", alertId,
                purged);
        Monitoring.get()
                .countPurged(purged, tags);

        return purged;
    }
}
