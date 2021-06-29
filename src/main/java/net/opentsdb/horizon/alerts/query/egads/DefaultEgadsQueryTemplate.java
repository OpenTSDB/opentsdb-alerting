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

package net.opentsdb.horizon.alerts.query.egads;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import net.opentsdb.utils.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.core.validate.Validate;

public final class DefaultEgadsQueryTemplate implements EgadsQueryTemplate {

    private static final Logger LOG =
            LoggerFactory.getLogger(DefaultEgadsQueryTemplate.class);

    private static final String KEY_BASELINE_PERIOD = "baselinePeriod";
    private static final String KEY_END = "end";
    private static final String KEY_EXECUTION_GRAPH = "executionGraph";
    private static final String KEY_FILTER = "filter";
    private static final String KEY_ID = "id";
    private static final String KEY_MODE = "mode";
    private static final String KEY_SERDES_CONFIGS = "serdesConfigs";
    private static final String KEY_SERIALIZE_OBSERVED = "serializeObserved";
    private static final String KEY_SERIALIZE_THRESHOLDS = "serializeThresholds";
    private static final String KEY_START = "start";
    private static final String KEY_TYPE = "type";
    private static final String MODE_EVALUATE = "EVALUATE";
    private static final String MODE_PREDICT = "PREDICT";
    private static final String OLYMPIC_SCORING_TYPE = "OlympicScoring";

    /**
     * Create a new query template.
     *
     * @param query          parsed configuration query
     * @param egadsNodeId    original EGADs node id
     * @param newEgadsNodeId node id to use for EGADs node name override
     * @return query template.
     */
    public static EgadsQueryTemplate create(final JsonNode query,
                                            final String egadsNodeId,
                                            final String newEgadsNodeId) {
        if (query instanceof ObjectNode) {
            return new DefaultEgadsQueryTemplate(
                    (ObjectNode) query,
                    egadsNodeId,
                    newEgadsNodeId
            );
        }
        throw new IllegalArgumentException("query is not an ObjectNode");
    }

    private final ObjectNode query;
    private final ObjectNode egadsNode;
    private final long baselinePeriodSec;

    private DefaultEgadsQueryTemplate(final ObjectNode query,
                                      final String egadsNodeId,
                                      final String newEgadsNodeId) {
        Validate.paramNotNull(query, "query");
        Validate.paramNotNull(egadsNodeId, "egadsNodeId");
        Validate.paramNotNull(newEgadsNodeId, "newEgadsNodeId");
        this.query = query;
        this.egadsNode = findEgadsNode(query, egadsNodeId);
        this.baselinePeriodSec = parseBaselinePeriodSec();
        overrideEgadsNodeId(newEgadsNodeId);
        overrideSerializationPolicy();
    }

    private ObjectNode findEgadsNode(final ObjectNode query,
                                     final String egadsNodeId) {
        final JsonNode execGraphNode = query.get(KEY_EXECUTION_GRAPH);

        // For backward compatibility if the node with given id is not found,
        // then use the first EGADs node out there.
        JsonNode firstEgadsNode = null;

        for (JsonNode node : execGraphNode) {
            if (node.hasNonNull(KEY_TYPE)
                    && OLYMPIC_SCORING_TYPE.equals(node.get(KEY_TYPE).asText())) {
                if (firstEgadsNode == null) {
                    firstEgadsNode = node;
                }

                if (egadsNodeId.equals(node.get(KEY_ID).asText())) {
                    return (ObjectNode) node;
                }
            }
        }

        if (firstEgadsNode != null) {
            LOG.warn("Failed to find OlympicScoring node with {} id, using {}",
                    egadsNodeId, firstEgadsNode);
            return (ObjectNode) firstEgadsNode;
        }

        throw new IllegalArgumentException(
                "OlympicScoring node not found: " + egadsNodeId);
    }

    private long parseBaselinePeriodSec() {
        return DateTime.parseDuration(
                egadsNode.get(KEY_BASELINE_PERIOD).asText()
        ) / 1_000L;
    }

    private void overrideEgadsNodeId(String egadsNodeId) {
        egadsNode.set(KEY_ID, new TextNode(egadsNodeId));

        final JsonNode serdesConfigsNode = query.get(KEY_SERDES_CONFIGS);
        if (serdesConfigsNode.size() != 1) {
            throw new IllegalStateException(
                    "Expected 1 node in `serdesCofigs`, got " +
                            serdesConfigsNode.size()
            );
        }

        // Serde has to contain only the EGADs node.
        final ObjectNode serde = (ObjectNode) serdesConfigsNode.get(0);
        serde.putArray(KEY_FILTER).add(egadsNodeId);
    }

    private void overrideSerializationPolicy() {
        egadsNode.set(KEY_SERIALIZE_OBSERVED, BooleanNode.getTrue());
        egadsNode.set(KEY_SERIALIZE_THRESHOLDS, BooleanNode.getTrue());
    }

    public String format(final long startTimeSec,
                         final long endTimeSec,
                         final boolean isPriming) {
        query.set(KEY_START, new LongNode(startTimeSec));
        query.set(KEY_END, new LongNode(endTimeSec));
        if (isPriming) {
            egadsNode.set(KEY_MODE, new TextNode(MODE_PREDICT));
        } else {
            egadsNode.set(KEY_MODE, new TextNode(MODE_EVALUATE));
        }
        return query.toString();
    }

    @Override
    public long getBaselinePeriodSec() {
        return baselinePeriodSec;
    }
}
