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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class MetricAliasParser {

    private static final Logger LOG = LoggerFactory.getLogger(MetricAliasParser.class);

    private static final String KEY_EXPRESSION = "expression";
    private static final String KEY_LABEL = "label";
    private static final String KEY_METRICS = "metrics";
    private static final String KEY_NAME = "name";
    private static final String KEY_QUERIES = "queries";
    private static final String KEY_RAW = "raw";
    private static final String KEY_SETTINGS = "settings";
    private static final String KEY_VISUAL = "visual";

    /**
     * Parse metric/expression aliases from the root query configuration.
     * If aliases cannot be parsed, then an empty map is returned without any
     * error thrown.
     */
    public static Map<String, String> parseAliases(JsonNode configRoot) {
        try {
            return unsafeParseAliases(configRoot);
        } catch (Exception e) {
            LOG.debug("Failed to parse configuration aliases: config={}", configRoot, e);
            return Collections.emptyMap();
        }
    }

    private static Map<String, String> unsafeParseAliases(JsonNode root) {
        final Map<String, String> aliases = new HashMap<>();
        final JsonNode queriesNode = root.get(KEY_QUERIES);
        final JsonNode rawQueriesNode = queriesNode.get(KEY_RAW);

        int queryId = 0;
        for (JsonNode queryNode : rawQueriesNode) {
            queryId++;

            int metricId = 0;
            int expressionId = 0;

            for (JsonNode entry : queryNode.get(KEY_METRICS)) {
                // Metrics have "name" field, expressions have "expression" field.
                final boolean isMetric = entry.hasNonNull(KEY_NAME) || !entry.hasNonNull(KEY_EXPRESSION);
                if (isMetric) {
                    metricId++;
                } else {
                    expressionId++;
                }

                final JsonNode visualNode = entry.get(KEY_SETTINGS).get(KEY_VISUAL);
                if (!visualNode.hasNonNull(KEY_LABEL)) {
                    continue;
                }
                final String aliasLabel = visualNode.get(KEY_LABEL).asText();
                if (aliasLabel == null || aliasLabel.isEmpty()) {
                    continue;
                }

                final char entryChar = isMetric ? 'm' : 'e';
                final int entryId = isMetric ? metricId : expressionId;
                final String id = String.format("q%d_%c%d", queryId, entryChar, entryId);
                aliases.put(id, aliasLabel);
            }
        }

        return aliases;
    }
}
