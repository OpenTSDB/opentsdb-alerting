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

public enum ComparatorType {

    GREATER_THAN(">",(byte)0),
    LESS_THAN("<",(byte)1),
    SYMBOL_EQUALS("=",(byte)2),
    GREATER_THAN_OR_EQUALS(">=",(byte)3),
    LESS_THAN_OR_EQUALS("<=",(byte)4),
    MISSING("missing",(byte)5);

    private String operator;
    private byte i;

    ComparatorType(String s, byte i) {
        this.operator = s;
        this.i = i;
    }

    public byte getId() {
        return i;
    }

    public String getOperator() {
        return operator;
    }

    public static ComparatorType getComparatorTypeFromId(byte b) {

        switch (b) {
            case 0:
                return GREATER_THAN;
            case 1:
                return LESS_THAN;
            case 2:
                return SYMBOL_EQUALS;
            case 3:
                return GREATER_THAN_OR_EQUALS;
            case 4:
                 return LESS_THAN_OR_EQUALS;

            default:
                throw new AssertionError("unsupportes Id "+b+" in ComparatorType");
        }
    }

    public static ComparatorType getComparatorTypeFromString(String b) {
        switch (b) {
            case "above":
                return GREATER_THAN;
            case "above_or_equal_to":
                return GREATER_THAN_OR_EQUALS;
            case "below":
                return LESS_THAN;
            case "below_or_equal_to":
                return LESS_THAN_OR_EQUALS;
            case "missing":
                return MISSING;
            default:
                throw new AssertionError("unsupported Id "+b+" in ComparatorType");
        }
    }

    public static ComparatorType getComparatorTypeFromOperator(String operator) {

        switch (operator) {
            case ">":
                return GREATER_THAN;
            case "<":
                return LESS_THAN;
            case "=":
                return SYMBOL_EQUALS;
            case ">=":
                return GREATER_THAN_OR_EQUALS;
            case "<=":
                return LESS_THAN_OR_EQUALS;
            case "missing":
                return MISSING;
            default:
                throw new AssertionError("unsupportes operator " +operator+ " in ComparatorType");
        }
    }
}
