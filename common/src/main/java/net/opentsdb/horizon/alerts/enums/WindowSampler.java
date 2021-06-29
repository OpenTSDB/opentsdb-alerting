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

public enum WindowSampler {

    AT_LEAST_ONCE((byte)0),ALL_OF_THE_TIMES((byte)1),SUMMARY((byte)2);

    private byte id;

    WindowSampler(byte b) {
        this.id = b;
    }

    public byte getId() {
        return id;
    }

    public static WindowSampler getWindowAggregatorTypeFromId(byte b) {

        switch (b) {

            case 0:
                return AT_LEAST_ONCE;

            case 1:
                return ALL_OF_THE_TIMES;

            case 2:
                return SUMMARY;
        }

        throw new AssertionError("Unsupported id for WindowAggregatorType :"+b);

    }

    public static WindowSampler getWindowAggregatorTypeFromString(String b, ComparatorType comparatorType) {
        
        if (comparatorType.equals(ComparatorType.MISSING)) {
            return SUMMARY;
        }

        switch (b) {
            case "at_least_once":
                return AT_LEAST_ONCE;
            case "all_of_the_times":
                return ALL_OF_THE_TIMES;
            case "in_total":
            case "in_avg":
                return SUMMARY;
        }
        throw new AssertionError("Unsupported id for WindowAggregatorType :" + b);
    }

    public WindowSampler flip() {

        switch (this){

            case SUMMARY:
                return SUMMARY;

            case AT_LEAST_ONCE:
                return ALL_OF_THE_TIMES;

            case ALL_OF_THE_TIMES:
                return ALL_OF_THE_TIMES;

        }

        return null;
    }
}
