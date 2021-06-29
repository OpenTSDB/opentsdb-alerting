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

package net.opentsdb.horizon.alerts.state.impl;

import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.state.AlertStateChange;

public class AlertStateChangeImpl implements AlertStateChange {

    private AlertState previousState;

    private AlertState currentState;

    private boolean isNag;

    private boolean raiseAlert;

    public AlertStateChangeImpl(AlertState previousState,
                         AlertState currentState,
                         boolean isNag,
                         boolean raiseAlert) {
        this.previousState = previousState;
        this.currentState = currentState;
        this.isNag = isNag;
        this.raiseAlert = raiseAlert;
    }

    public AlertState getPreviousState() {
        return previousState;
    }

    public AlertState getCurrentState() {
        return currentState;
    }

    public boolean isNag() {
        return isNag;
    }

    public boolean raiseAlert() {
        return raiseAlert;
    }
}
