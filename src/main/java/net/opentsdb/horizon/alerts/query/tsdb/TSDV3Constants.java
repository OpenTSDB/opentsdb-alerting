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

package net.opentsdb.horizon.alerts.query.tsdb;

public class TSDV3Constants {

    public static final String NumericSummaryType = "NumericSummaryType";
    public static final String NumericType = "NumericType";
    public static final String DATA = "data";
    public static final String TIME_SPECIFICATION = "timeSpecification";
    public static final String TIME_START = "start";
    public static final String TIME_END = "end";
    public static final String INTERVAL = "interval";
    public static final int DEFAULT_INTERVAL = 60;
    public static final int SECS_IN_MIN = 60;
    public static final int SECS_IN_HOUR = 3600;
    public static final int SECS_IN_DAY = 86400;
    public static final String TAGS = "tags";
    public static final String SOURCE = "source";
    public static final String RESULTS = "results";
    public static final String BINARY_EXPRESSION_FORMAT = "%s %s %s";
    public static final String TSDB_SUM = "sum";
    public static final String GROUPBY = "groupby";
    public static final String JSONV3_QUERY_SERDES = "JsonV3QuerySerdes";
}
