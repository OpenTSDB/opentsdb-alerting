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

package net.opentsdb.horizon.alerts.config;

public class AlertConfigFields {

    public static final String ALERT_ID = "id";

    public static final String NAME = "name";

    public static final String NAMESPACE = "namespace";

    public static final String UPDATED_TIME = "updatedTime";

    public static final String TYPE = "type";

    public static final String QUERIES = "queries";

    public static final String QUERY_INDEX = "queryIndex";

    public static final String QUERY_TYPE = "queryType";

    public static final String THRESHOLD = "threshold";

    public static final String SUB_TYPE = "subType";

    public static final String ENABLED = "enabled";

    public static final String LABELS = "labels";

    public static final String SUBJECT = "subject";

    public static final String BODY = "body";

    public static final String RECIPIENTS = "recipients";

    public static final String RECIPIENTS_NAME = "name";

    public static final String ALERT_GROUPING_RULES = "alertGroupingRules";

    public static final String NOTIFICATION = "notification";

    public static final String TRANSITIONS_TO_NOTIFY = "transitionsToNotify";

    public static final String DELAY_EVALUATION = "delayEvaluation";

    public static final String BAD_THRESHOLD = "badThreshold";

    public static final String WARN_THRESHOLD = "warnThreshold";

    public static final String RECOVERY_THRESHOLD = "recoveryThreshold";

    public static final String IS_NAG_ENABLED = "isNagEnabled";

    public static final String NAG_INTERVAL = "nagInterval";

    public static final String NOTIFY_ON_MISSING = "notifyOnMissing";

    public static final String SUPPRESS_METRIC = "suppress";
}
