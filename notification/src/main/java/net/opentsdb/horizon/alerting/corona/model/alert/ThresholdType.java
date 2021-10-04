/*
 *  This file is part of OpenTSDB.
 *  Copyright (C) 2021 Yahoo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.opentsdb.horizon.alerting.corona.model.alert;

public enum ThresholdType {

    UNKNOWN((byte) 0),
    LOWER_WARN((byte) 1),
    LOWER_BAD((byte) 2),
    UPPER_WARN((byte) 3),
    UPPER_BAD((byte) 4);

    final byte id;

    ThresholdType(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static ThresholdType valueOf(byte id) {
        switch (id) {
            case 1:
                return LOWER_WARN;
            case 2:
                return LOWER_BAD;
            case 3:
                return UPPER_WARN;
            case 4:
                return UPPER_BAD;
            default:
                return UNKNOWN;
        }
    }
}
