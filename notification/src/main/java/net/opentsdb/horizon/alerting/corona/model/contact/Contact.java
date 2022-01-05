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

package net.opentsdb.horizon.alerting.corona.model.contact;

import lombok.Getter;

public interface Contact {

    // Used to mark an unknown contact id. Part of a fix for Config API
    // changes.
    int UNKNOWN_ID = 0;

    enum Type {
        EMAIL((byte) 0),
        WEBHOOK((byte) 1),
        OC((byte) 2),
        OPSGENIE((byte) 3),
        SLACK((byte) 4),
        PAGERDUTY((byte) 5);

        /* ------------ Static Methods ------------ */

        public static Type valueFrom(byte id)
        {
            switch (id) {
                case 0:
                    return EMAIL;
                case 1:
                    return WEBHOOK;
                case 2:
                    return OC;
                case 3:
                    return OPSGENIE;
                case 4:
                    return SLACK;
                case 5:
                    return PAGERDUTY;
            }
            throw new IllegalArgumentException("Unknown id=" + id);
        }

        /* ------------ Fields ------------ */

        @Getter
        private final byte id;

        Type(byte id)
        {
            this.id = id;
        }
    }

    Type getType();

    int getId();

    String getName();

}
