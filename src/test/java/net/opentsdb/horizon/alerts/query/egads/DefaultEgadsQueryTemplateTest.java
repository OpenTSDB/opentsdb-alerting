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
import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.opentsdb.horizon.alerts.core.TestUtil;

public class DefaultEgadsQueryTemplateTest {

    private static final String EGADS_NODE_ID = "EGADS_ID";

    private static String resource(final String name) {
        return TestUtil.loadResource("data/DefaultEgadsQueryTemplateTest/" + name + ".json");
    }

    @Test
    public void format() {
        final EgadsQueryTemplate egadsQueryTemplate =
                DefaultEgadsQueryTemplate.create(
                        TestUtil.parseJson(resource("egads-config"))
                                .get("queries")
                                .get("tsdb")
                                .get(0),
                        "q1_m1",
                        EGADS_NODE_ID
                );

        final long startSec = 1L;
        final long endSec = 1L;
        final boolean isPriming = false;

        // Test mode set correctly.
        {
            final String query = egadsQueryTemplate.format(startSec, endSec, isPriming);
            assertTrue(query.contains("EVALUATE"));
            assertTrue(query.contains(EGADS_NODE_ID));
            final JsonNode root = TestUtil.parseJson(query);
            assertEquals(startSec, root.get("start").asLong());
            assertEquals(endSec, root.get("end").asLong());
        }

        {
            final String query = egadsQueryTemplate.format(startSec, endSec, !isPriming);
            assertTrue(query.contains("PREDICT"));
            assertTrue(query.contains(EGADS_NODE_ID));
            final JsonNode root = TestUtil.parseJson(query);
            assertEquals(startSec, root.get("start").asLong());
            assertEquals(endSec, root.get("end").asLong());
        }
    }
}
