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

import java.util.Objects;

import lombok.Getter;

public enum Transition {

    GOOD_TO_BAD((byte) 0, "good", "bad"),

    WARN_TO_BAD((byte) 1, "warn", "bad"),

    WARN_TO_GOOD((byte) 2, "warn", "good"),

    BAD_TO_GOOD((byte) 3, "bad", "good"),

    GOOD_TO_WARN((byte) 4, "good", "warn"),

    BAD_TO_WARN((byte) 5, "bad", "warn");

    /* ------------ Static Methods ------------ */

    public static Transition valueFrom(final byte id)
    {
        switch (id) {
            case 0:
                return GOOD_TO_BAD;
            case 1:
                return WARN_TO_BAD;
            case 2:
                return WARN_TO_GOOD;
            case 3:
                return BAD_TO_GOOD;
            case 4:
                return GOOD_TO_WARN;
            case 5:
                return BAD_TO_WARN;
            default:
                throw new AssertionError("Unknown id=" + id);
        }
    }

    /* ------------ Fields ------------ */

    @Getter
    private byte id;

    @Getter
    private String from;

    @Getter
    private String to;

    /* ------------ Constructor ------------ */

    Transition(final byte id, final String from, final String to)
    {
        Objects.requireNonNull(from, "from cannot be null");
        Objects.requireNonNull(to, "to cannot be null");
        this.id = id;
        this.from = from;
        this.to = to;
    }
}
