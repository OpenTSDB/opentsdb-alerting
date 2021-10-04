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

package net.opentsdb.horizon.alerting.corona.model.metadata;

public enum OcSeverity {

    NOT_SET((byte) 0),

    SEV_1((byte) 1),
    SEV_2((byte) 2),
    SEV_3((byte) 3),
    SEV_4((byte) 4),
    SEV_5((byte) 5);

    private final byte id;

    OcSeverity(final byte id)
    {
        this.id = id;
    }

    public byte getId()
    {
        return id;
    }

    public static OcSeverity fromId(final byte id)
    {
        switch (id) {
            case 0:
                return NOT_SET;
            case 1:
                return SEV_1;
            case 2:
                return SEV_2;
            case 3:
                return SEV_3;
            case 4:
                return SEV_4;
            case 5:
                return SEV_5;
        }
        throw new IllegalArgumentException("Unknown severity: " + id);
    }
}
