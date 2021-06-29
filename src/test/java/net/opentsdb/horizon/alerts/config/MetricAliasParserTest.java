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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.opentsdb.horizon.alerts.core.TestUtil;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.Test;

import static net.opentsdb.horizon.alerts.AlertUtils.parseJsonTree;
import static org.junit.Assert.assertEquals;

public class MetricAliasParserTest {

    private static String resource(final String name) {
        return TestUtil.loadResource("data/MetricAliasParserTest/" + name + ".json");
    }

    @Test
    void Parse() throws IOException {
        final JsonNode configRoot = parseJsonTree(resource("single-metric-alert"));
        final Map<String, String> expected = new HashMap<>();
        expected.put("q1_m1", "den-alerts-consumed");
        expected.put("q2_e1", "q2-expr");
        expected.put("q1_e1", "den-alert-consumed-2");
        expected.put("q1_e2", "m1_plus_m2");

        assertEquals(expected, MetricAliasParser.parseAliases(configRoot));

        final JsonNode configRoot2 = parseJsonTree(resource("single-metric-alert-2"));
        final Map<String, String> expected2 = new HashMap<>();
        expected2.put("q1_e1", "Avg msg size in rate");

        assertEquals(expected2, MetricAliasParser.parseAliases(configRoot2));
    }
}
