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

package net.opentsdb.horizon.alerts.query.eventdb;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Consumer;

import net.opentsdb.horizon.alerts.AlertUtils;

import com.fasterxml.jackson.databind.JsonNode;

class QueryResponse {

    private final JsonNode root;

    QueryResponse(final String payload) {
        try {
            this.root = AlertUtils.parseJsonTree(payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Applies the given handler to every data point in the response.
     *
     * @param handler function to apply to a data point.
     */
    void forEach(final Consumer<QueryDataPoint> handler) {
        final Iterator<JsonNode> dataPoints;
        try {
            dataPoints = root
                    .get("results")
                    .get(0)
                    .get("data")
                    .elements();
        } catch (NullPointerException e) {
            // TODO: maybe no data is ok?
            throw new RuntimeException("failed ot get 'data' node", e);
        }

        // Reuse the same instance for all actual data points.
        final QueryDataPoint dataPoint = new QueryDataPoint();
        while (dataPoints.hasNext()) {
            final JsonNode dataNode = dataPoints.next();
            dataPoint.setDataNode(dataNode);
            handler.accept(dataPoint);
        }
    }
}
