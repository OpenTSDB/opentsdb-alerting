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

package net.opentsdb.horizon.alerts.config.impl;

public class MetricAlertConfigFields {

    public static final String SINGLE_METRIC = "singleMetric";

    public static final String SLIDING_WINDOW = "slidingWindow";

    public static final String TIME_SAMPLER = "timeSampler";

    public static final String ATLEAST_ONCE = "at_least_once";

    public static final String ATLEAST_ONCE_STRING = "at least once";

    public static final String ALL_OF_THE_TIMES = "all_of_the_times";

    public static final String ALL_OF_THE_TIMES_STRING = "all of the times";

    public static final String ON_AVG = "on_avg";

    public static final String ON_AVG_STRING = "on average";

    public static final String IN_TOTAL = "in_total";

    public static final String IN_TOTAL_STRING = "in total";

    public static final String MISSING = "missing";

    public static final String COMPARISON_OPERATOR = "comparisonOperator";

    public static final String METRIC_ID = "metricId";

    public static final String QUERY_TYPE_TSDB = "tsdb";
    public static final String QUERY = "queries";
    public static final String EXECUTION_GRAPH = "executionGraph";

    public static final String REQUIRE_FULL_WINDOW = "requiresFullWindow";

    public static final String REPORTING_INTERVAL = "reportingInterval";

    public static final String AUTO_RECOVERY_INTERVAL = "autoRecoveryInterval";


}
