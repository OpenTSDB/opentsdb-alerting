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

package net.opentsdb.horizon.alerts.query;

public class QueryConstants {
    public static final String GROUP_BY_ALL = "corona_horizon_all";
    public static final String ALERT_OUTPUT_STRING = "%s is %s (observed) %s %s (threshold) %s";
    public static final String BAD_ALERT_NODE = "badalert";
    public static final String WARN_ALERT_NODE = "warnalert";
    public static final String RECOVERY_ALERT_NODE = "recoveryalert";
    public static final String BAD_THRESHOLD_NODE = "badtld";
    public static final String WARN_THRESHOLD_NODE = "warntld";
    public static final String RECOVERY_THRESHOLD_NODE = "recoverytld";
    public static final String HEARTBEAT_NODE = "heartbeat";
    public static final String HEARTBEAT_THRESHOLD_NODE = "heartbeattld";

    public static final String SUMMARIZED = "summarized%s";
}
