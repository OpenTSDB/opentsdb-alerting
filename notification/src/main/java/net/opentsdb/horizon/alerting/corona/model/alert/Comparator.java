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

public enum Comparator {

    GREATER_THAN((byte) 0, ">"),

    LESS_THAN((byte) 1, "<"),

    EQUALS((byte) 2, "="),

    GREATER_THAN_OR_EQUALS((byte) 3, ">="),

    LESS_THAN_OR_EQUALS((byte) 4, "<=");

    public static Comparator valueFrom(byte id)
    {

        switch (id) {
            case 0:
                return GREATER_THAN;
            case 1:
                return LESS_THAN;
            case 2:
                return EQUALS;
            case 3:
                return GREATER_THAN_OR_EQUALS;
            case 4:
                return LESS_THAN_OR_EQUALS;
            default:
                throw new IllegalArgumentException("Unknown id=" + id);
        }
    }

    public static Comparator valueFrom(String operator)
    {
        switch (operator) {
            case ">":
                return GREATER_THAN;
            case "<":
                return LESS_THAN;
            case "=":
                return EQUALS;
            case ">=":
                return GREATER_THAN_OR_EQUALS;
            case "<=":
                return LESS_THAN_OR_EQUALS;
            default:
                throw new IllegalArgumentException(
                        "Unsupported operator=" + operator);
        }
    }

    @Getter
    private byte id;

    @Getter
    private String operator;

    Comparator(byte id, String operator)
    {
        this.id = id;
        this.operator = operator;
    }

    public Comparator reverse()
    {
        switch (this) {
            case GREATER_THAN:
                return LESS_THAN_OR_EQUALS;
            case LESS_THAN:
                return GREATER_THAN_OR_EQUALS;
            case EQUALS:
                return EQUALS;
            case GREATER_THAN_OR_EQUALS:
                return LESS_THAN;
            case LESS_THAN_OR_EQUALS:
                return GREATER_THAN;
            default:
                throw new IllegalStateException(
                        "Unknown state=" + this.name());
        }
    }
}
