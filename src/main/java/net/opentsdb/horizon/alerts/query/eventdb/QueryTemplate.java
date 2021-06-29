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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import net.opentsdb.horizon.alerts.AlertUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;

class QueryTemplate {

    /* ------------ Constants ------------ */

    private static final JsonNode GROUP_BY_QUERY_TEMPLATE =
            loadQueryTemplate("event_query_group_count.json");

    private static final JsonNode LIST_QUERY_TEMPLATE =
            loadQueryTemplate("event_query_list_one.json");

    /* ------------ Static Methods ------------ */

    private static JsonNode loadQueryTemplate(final String resource) {
        final ClassLoader cl = EventProcessor.class.getClassLoader();
        try (InputStream is = cl.getResourceAsStream(resource)) {
            final String template = IOUtils.toString(is, StandardCharsets.UTF_8);
            return AlertUtils.parseJsonTree(template);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* ------------ Fields ------------ */

    private final ObjectNode queryTemplate;

    /* ------------ Constructors ------------ */

    QueryTemplate(final String queryNamespace,
                  final String queryFilter,
                  final List<String> groupBy) {
        Objects.requireNonNull(queryNamespace, "queryNamespace cannot be null");
        Objects.requireNonNull(queryFilter, "queryFilter cannot be null");
        Objects.requireNonNull(groupBy, "groupBy cannot be null");
        this.queryTemplate =
                formatQueryTemplate(queryNamespace, queryFilter, groupBy);
    }

    /* ------------ Methods ------------ */

    private ObjectNode formatQueryTemplate(final String namespace,
                                           final String filter,
                                           final List<String> groupBy) {
        final JsonNode template;
        if (groupBy == null || groupBy.isEmpty()) {
            template = LIST_QUERY_TEMPLATE.deepCopy();
            formatDataNode(template, namespace, filter);
            // There is no group by node in the list query.
        } else {
            template = GROUP_BY_QUERY_TEMPLATE.deepCopy();
            formatDataNode(template, namespace, filter);
            formatGroupByNode(template, groupBy);
        }
        return (ObjectNode) template;
    }

    private void formatDataNode(final JsonNode template,
                                final String namespace,
                                final String filter) {
        // TODO: How to remove the node selection logic from here?
        final JsonNode executionGraphNode = template.get("executionGraph");
        final ObjectNode dataNode = (ObjectNode) executionGraphNode.get(0);

        dataNode.put("namespace", namespace);

        final ObjectNode filterNode =
                (ObjectNode) dataNode
                        .get("filter")
                        .get("filters")
                        .get(0);
        filterNode.put("filter", filter);
    }

    private void formatGroupByNode(final JsonNode template,
                                   final List<String> groupBy) {
        // TODO: How to remove the node selection logic from here?
        final JsonNode executionGraphNode = template.get("executionGraph");
        final JsonNode groupByNode = executionGraphNode.get(1);

        final ArrayNode tagKeysNode = (ArrayNode) groupByNode.get("tagKeys");
        tagKeysNode.removeAll();

        groupBy.forEach(tagKeysNode::add);
    }

    /**
     * Evaluates the template with given start and end times.
     *
     * @param startTimeSec query range start time
     * @param endTimeSec   query range end time
     * @return JSON query string.
     */
    String evaluate(final long startTimeSec, final long endTimeSec) {
        queryTemplate.set("start", new LongNode(startTimeSec));
        queryTemplate.set("end", new LongNode(endTimeSec));
        return queryTemplate.toString();
    }
}
