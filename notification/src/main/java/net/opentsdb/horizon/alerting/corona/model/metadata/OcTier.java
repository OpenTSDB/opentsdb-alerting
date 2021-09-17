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

public enum OcTier {

    NOT_SET((byte) 0),

    TIER_1((byte) 1),
    TIER_2((byte) 2),
    TIER_3((byte) 3),
    TIER_4((byte) 4);

    private final byte id;

    OcTier(final byte id)
    {
        this.id = id;
    }

    public byte getId()
    {
        return id;
    }

    public static OcTier fromId(final byte id)
    {
        switch (id) {
            case 0:
                return NOT_SET;
            case 1:
                return TIER_1;
            case 2:
                return TIER_2;
            case 3:
                return TIER_3;
            case 4:
                return TIER_4;
        }
        throw new IllegalArgumentException("Unknown tier: " + id);
    }
}
