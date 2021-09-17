/*
 *  This file is part of OpenTSDB.
 *  Copyright (C) 2021 Yahoo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.opentsdb.horizon.alerting.corona.model.namespace;

import com.fasterxml.jackson.databind.JsonNode;
import net.opentsdb.horizon.alerting.corona.testutils.Utils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NamespaceListParserTest {

    @Test
    void doParse()
    {
        final Set<String> expected = new HashSet<>(Arrays.asList(
                "ns2", "ns1"
        ));

        final JsonNode json = Utils.parseJsonTree("payloads/namespaces.json");

        // Test
        List<String> namespaces = new NamespaceListParser().doParse(json);

        assertEquals(expected, new HashSet<>(namespaces));
    }
}