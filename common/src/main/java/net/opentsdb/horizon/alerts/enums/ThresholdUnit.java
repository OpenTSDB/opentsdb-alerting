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

public enum ThresholdUnit {

    UNKNOWN((byte) 0),
    PERCENT((byte) 1),
    VALUE((byte) 2);

    final byte id;

    ThresholdUnit(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static ThresholdUnit valueOf(byte id) {
        switch (id) {
            case 1:
                return PERCENT;
            case 2:
                return VALUE;
            default:
                return UNKNOWN;
        }
    }
}
