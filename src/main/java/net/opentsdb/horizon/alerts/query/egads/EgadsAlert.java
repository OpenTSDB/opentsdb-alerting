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

package net.opentsdb.horizon.alerts.query.egads;

import net.opentsdb.horizon.core.builder.CoreBuilder;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.enums.ThresholdType;

/**
 * Interface for the OpenTSDB <em>AlertType</em>.
 */
public interface EgadsAlert {

    long getTimestampSec();

    AlertState getAlertState();

    String getMessage();

    double getObservedValue();

    double getThresholdValue();

    ThresholdType getThresholdType();

    /**
     * Builder interface for {@link EgadsAlert}
     *
     * @param <B>
     */
    interface Builder<B extends Builder<B>>
            extends CoreBuilder<B, EgadsAlert> {

        B setTimestampSec(long timestampSec);

        B setAlertState(AlertState alertState);

        B setMessage(String message);

        B setObservedValue(double value);

        B setThresholdValue(double value);

        B setThresholdType(ThresholdType type);

    }

    EgadsAlert[] EMPTY = new EgadsAlert[0];

    static EgadsAlert[] emptyAlerts() {
        return EMPTY;
    }

}
