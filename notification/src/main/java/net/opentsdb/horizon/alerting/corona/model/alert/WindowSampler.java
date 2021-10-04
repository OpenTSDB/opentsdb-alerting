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

import lombok.Getter;

public enum WindowSampler {

    AT_LEAST_ONCE((byte) 0, "at least once"),

    ALL_OF_THE_TIMES((byte) 1, "at all times"),

    SUMMARY((byte) 2, "summary"),

    ;

    /* ------------ Static Methods ------------ */

    public static WindowSampler valueFrom(byte id)
    {
        switch (id) {
            case 0:
                return AT_LEAST_ONCE;
            case 1:
                return ALL_OF_THE_TIMES;
            case 2:
                return SUMMARY;
            default:
                throw new IllegalArgumentException("Unknown id=" + id);
        }
    }

    /* ------------ Fields ------------ */

    @Getter
    private byte id;

    @Getter
    private String type;

    /* ------------ Constructor ------------ */

    WindowSampler(byte id, String type)
    {
        this.id = id;
        this.type = type;
    }
}
