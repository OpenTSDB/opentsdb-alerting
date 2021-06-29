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

public enum  SummaryType {

    AVG((byte)0),SUM((byte)1);

    private byte b;

    SummaryType(byte b) {
        this.b = b;
    }

    public byte getId() {
        return b;
    }

    public static SummaryType getSummaryFromId(byte b) {
        switch (b) {

            case 0:
                return AVG;
            case 1:
                return SUM;
            default:
                throw new AssertionError("Unknown summary type : "+b);

        }
    }

    public static SummaryType getSummaryTypeFromString(String b) {
        switch (b) {
            case "at_least_once":
            case "all_of_the_times":
                return null;
            case "in_total":
                return AVG;
            case "in_avg":
                return SUM;
        }
        throw new AssertionError("Unsupported string for SummaryType :" + b);
    }
}
