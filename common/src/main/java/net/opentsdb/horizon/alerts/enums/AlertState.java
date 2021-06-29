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

public enum AlertState {

    GOOD((byte) 0),
    BAD((byte) 1),
    WARN((byte) 2),
    UNKNOWN((byte) 3),
    MISSING((byte) 4);

    private byte b;

    AlertState(byte b) {
        this.b = b;
    }

    public byte getId() {
        return b;
    }

    public static AlertState fromId(byte b) {
        switch (b) {
            case 0:
                return GOOD;
            case 1:
                return BAD;
            case 2:
                return WARN;
            case 3:
                return UNKNOWN;
            case 4:
                return MISSING;
        }

        throw new AssertionError("Bad AlertState id: " + b);
    }

    public static AlertState fromId(long l) {
        return fromId((byte) l);
    }
}
