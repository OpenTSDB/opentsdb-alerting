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

package net.opentsdb.horizon.alerts.enums;

/**
 * Note that this class does _NOT_ directly map to the `type` field
 * in the alert configuration. This class is flattened version of `type`
 * and `threshold.subType` combination.
 *
 * <pre>{@code
 * {
 *   ...,
 *   "type": "simple",                  <---- here
 *   ...,
 *   "queries": {
 *     "tsdb": [ ... ]
 *     "raw":  [ ... ],
 *   },
 *   "threshold": {
 *     "periodOverPeriod": { ... },
 *     "subType": "periodOverPeriod",   <---- and here
 *     "delayEvaluation": "0",
 *     "isNagEnabled": true
 *   },
 *   "notification": {
 *     ...
 *   },
 *   "alertGroupingRules": [],
 *   "namespaceId": 6539
 * }
 * }
 * </pre>
 *
 * Having a proper two level separation using the {@link MetricAlertType} is
 * troublesome. The latter is not really used, hence deprecated.
 */
public enum AlertType {

    SIMPLE((byte) 0), // Single metric alert.
    HEALTH_CHECK((byte) 1),
    EVENT((byte) 2),
    PERIOD_OVER_PERIOD((byte) 3);

    private byte i;

    AlertType(byte i) {
        this.i = i;
    }

    public byte getId() {
        return i;
    }

    public AlertType getAlertTypeById(byte i) {
        switch (i) {
            case 0:
                return SIMPLE;
            case 1:
                return HEALTH_CHECK;
            case 2:
                return EVENT;
            case 3:
                return PERIOD_OVER_PERIOD;
            default:
                throw new AssertionError("Alert type not supported for: "+i);
        }
    }

    public String getString() {
        switch (i) {
            case 0:
                return "metric";
            case 1:
                return "healthCheck";
            case 3:
                return "event";
            case 4:
                return "periodOverPeriod";
            default:
                return null;
        }
    }
}
