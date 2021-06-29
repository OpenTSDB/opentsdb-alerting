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

public class HealthCheckConfigFields {

    public static final String MISSING_DATA_INTERVAL = "missingDataInterval";

    public static final String MISSING_DATA_PURGE_INTERVAL = "missingDataPurgeInterval";

    public static final String QUERY_TYPE_AURA = "aura";

    public static final String APPLICATION_FOR_DATA = "alerts_data_application";

    public static final String NAMESPACE_FOR_DATA = "alerts_data_namespace";

    public static final String HEALTHCHECK = "healthCheck";

    public static final String UNKNOWN_THRESHOLD = "unknownThreshold";

    public static final String MISSING_SINCE = "missingSince";

    public static final String RECOVERED_SINCE = "recoveredSince";

}
