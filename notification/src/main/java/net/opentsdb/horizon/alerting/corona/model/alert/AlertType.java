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

public enum AlertType {

    SINGLE_METRIC((byte) 0),
    HEALTH_CHECK((byte) 1),
    EVENT((byte) 2),
    PERIOD_OVER_PERIOD((byte) 3);

    /* ------------ Static Methods ------------ */

    public static AlertType valueFrom(byte id)
    {
        switch (id) {
            case 0:
                return SINGLE_METRIC;
            case 1:
                return HEALTH_CHECK;
            case 2:
                return EVENT;
            case 3:
                return PERIOD_OVER_PERIOD;
        }
        throw new IllegalArgumentException("Unknown id=" + id);
    }

    /* ------------ Fields ------------ */

    @Getter
    private final byte id;

    /* ------------ Constructor ------------ */

    AlertType(final byte id)
    {
        this.id = id;
    }
}
