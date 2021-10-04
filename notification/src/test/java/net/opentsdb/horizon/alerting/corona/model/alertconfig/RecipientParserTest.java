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

package net.opentsdb.horizon.alerting.corona.model.alertconfig;

import com.fasterxml.jackson.databind.JsonNode;
import net.opentsdb.horizon.alerting.corona.testutils.Utils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecipientParserTest {

    private static final RecipientParser parser = new RecipientParser(Recipient::builder);

    @ParameterizedTest
    @MethodSource("doParseArgs")
    public void doParse(String content, Recipient expected) {
        final JsonNode jsonTree = Utils.parseJsonTreeFromString(content);
        final Recipient actual = parser.doParse(jsonTree);
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> doParseArgs() {
        return Stream.of(
                Arguments.of(
                        "{\"id\": 29,\"name\": \"bob@opentsdb.net\",\"email\": \"bob@opentsdb.net\",\"admin\": false }",
                        new Recipient(29, "bob@opentsdb.net")
                ),
                Arguments.of(
                        "{\"id\": 29,\"name\": \"bob@opentsdb.net\" }",
                        new Recipient(29, "bob@opentsdb.net")
                ),
                Arguments.of(
                        "{\"name\": \"bob@opentsdb.net\" }",
                        new Recipient(0, "bob@opentsdb.net")
                )
        );
    }
}
