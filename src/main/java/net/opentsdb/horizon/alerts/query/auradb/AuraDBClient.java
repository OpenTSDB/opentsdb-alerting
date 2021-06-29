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

package net.opentsdb.horizon.alerts.query.auradb;

import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.query.tsdb.TSDBClient;

public class AuraDBClient {

    private final String auradbHost = "TODO";

    private final TSDBClient tsdbClient;

    public AuraDBClient() {
        this.tsdbClient = new TSDBClient(auradbHost, "no_auth");

    }

    public String getResponse(String query, long alertId) throws AlertException {
        return tsdbClient.getResponse(query,"/v1/status", alertId);
    }

}
