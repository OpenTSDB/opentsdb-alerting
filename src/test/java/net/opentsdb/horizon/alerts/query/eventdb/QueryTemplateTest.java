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

import java.util.Arrays;
import java.util.Collections;

import net.opentsdb.horizon.alerts.core.TestUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class QueryTemplateTest {

    private static final long START_TIME = 1L;

    private static final long END_TIME = 10L;

    private static final String NAMESPACE = "QueryNamespace";

    private static final String FILTER = "Query AND Filter";

    @Test
    public void evaluateWithGroupByList() {
        final QueryTemplate template =
                new QueryTemplate(
                        NAMESPACE,
                        FILTER,
                        Arrays.asList("host", "app")
                );

        final JsonNode query = TestUtil.parseJson(
                template.evaluate(START_TIME, END_TIME)
        );

        // Verifications
        assertEquals(query.get("start").asLong(), START_TIME);
        assertEquals(query.get("end").asLong(), END_TIME);

        final ArrayNode graphNode = (ArrayNode) query.get("executionGraph");
        assertEquals(graphNode.size(), 2);

        final JsonNode dataNode = graphNode.get(0);
        assertEquals(dataNode.get("namespace").asText(), NAMESPACE);
        assertEquals(
                dataNode.get("filter").get("filters").get(0).get("filter").asText(),
                FILTER
        );

        final JsonNode groupNode = graphNode.get(1);
        assertEquals(groupNode.get("tagKeys").get(0).asText(), "host");
        assertEquals(groupNode.get("tagKeys").get(1).asText(), "app");
    }

    @Test
    public void evaluateNoGroupBy() {
        final QueryTemplate template =
                new QueryTemplate(
                        NAMESPACE,
                        FILTER,
                        Collections.emptyList()
                );

        final JsonNode query = TestUtil.parseJson(
                template.evaluate(START_TIME, END_TIME)
        );

        // Verifications
        assertEquals(query.get("start").asLong(), START_TIME);
        assertEquals(query.get("end").asLong(), END_TIME);

        final ArrayNode graphNode = (ArrayNode) query.get("executionGraph");
        assertEquals(graphNode.size(), 1);

        final JsonNode dataNode = graphNode.get(0);
        assertEquals(dataNode.get("namespace").asText(), NAMESPACE);
        assertEquals(
                dataNode.get("filter").get("filters").get(0).get("filter").asText(),
                FILTER
        );
    }
}
