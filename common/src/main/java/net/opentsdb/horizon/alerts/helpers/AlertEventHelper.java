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

package net.opentsdb.horizon.alerts.helpers;

import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.model.SingleMetricAlertEvent;

import java.util.Map;
import java.util.Optional;

public class AlertEventHelper {

    public static final String BREACHED_VALUE_INDEX = "BREACHED_VALUE_INDEX";


    public static void addBreachingIndex(final SingleMetricAlertEvent singleMetricAlertEvent,
                                        final int index) {

        if(index >= 0 &&
                !singleMetricAlertEvent.getSignal().equals(AlertState.MISSING)) {

            singleMetricAlertEvent.getAdditionalProperties().put(BREACHED_VALUE_INDEX,
                    String.valueOf(index));
        }

    }

    /**
     * Will throw IllegalStateException if it has to
     * return a negative value
     *
     * @throws IllegalStateException
     *
     * @param singleMetricAlertEvent
     * @return
     */
    public static int getBreachingIndex(final SingleMetricAlertEvent singleMetricAlertEvent)
            throws IllegalStateException {
        final Map<String, String> additionalProperties =
                singleMetricAlertEvent.getAdditionalProperties();

        if(singleMetricAlertEvent.getSignal().equals(AlertState.MISSING)) {
            throw new IllegalStateException("Breach index not valid for missing alert");
        }

        final int i = Integer.parseInt(Optional.ofNullable(additionalProperties
                .get(BREACHED_VALUE_INDEX))
                .orElse(
                        String.valueOf(
                                (singleMetricAlertEvent.getTimestamps().length - 1))));
        if( i < 0) {
            throw new IllegalStateException("The event does not have a valid array of values");
        }

        return i;
    }


}
