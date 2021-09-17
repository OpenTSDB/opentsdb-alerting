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

package net.opentsdb.horizon.alerting.corona.model.contact.impl;

import com.fasterxml.jackson.databind.JsonNode;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.testutils.Utils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContactsParserTest {

    private static final ContactsParser parser = new ContactsParser();

    @ParameterizedTest
    @MethodSource("doParseArgs")
    public void doParse(String content, Contacts expected) {
        final JsonNode jsonTree = Utils.parseJsonTreeFromString(content);
        final Contacts actual = parser.doParse(jsonTree);
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> doParseArgs() {
        return Stream.of(
                Arguments.of(
                        "{" +
                                // email
                                "\"email\": [" +
                                "{" +
                                " \"id\": 21," +
                                " \"name\": \"foo@opentsdb.net\"," +
                                " \"email\": \"foo@opentsdb.net\"," +
                                " \"admin\": true" +
                                "}," +
                                "{" +
                                " \"id\": 22," +
                                " \"name\": \"foo@opentsdb.net\"," +
                                " \"email\": \"foo@opentsdb.net\"," +
                                " \"admin\": true" +
                                "}," +
                                "{" +
                                " \"id\": 0," +
                                " \"name\": \"bar@opentsdb.net\"," +
                                " \"email\": \"bar@opentsdb.net\"," +
                                " \"admin\": true" +
                                "}," +
                                "{" +
                                " \"id\": 0," +
                                " \"name\": \"bar@opentsdb.net\"," +
                                " \"email\": \"bar@opentsdb.net\"," +
                                " \"admin\": true" +
                                "}" +
                                "]," +

                                // slack
                                "\"slack\": [" +
                                "{" +
                                " \"id\": 34," +
                                " \"name\": \"#test1\"," +
                                " \"webhook\": \"test1\"" +
                                "}" +
                                "]," +

                                // opsgenie
                                "\"opsgenie\": [" +
                                "{" +
                                " \"id\": 27," +
                                " \"name\": \"test-4\"," +
                                " \"apikey\": \"112wqeqweqweqdsfsdfdsdfsdfsdfsdfsdfs\"" +
                                "}" +
                                "]," +

                                // oc
                                "\"oc\": [" +
                                "{" +
                                " \"id\": 28," +
                                " \"name\": \"test\"," +
                                " \"displaycount\": \"2\"," +
                                " \"context\": \"test\"," +
                                " \"opsdbproperty\": \"OpenTSDB\"" +
                                "}," +
                                "{" +
                                " \"id\": 32," +
                                " \"name\": \"new-oc\"," +
                                " \"displaycount\": \"1\"," +
                                " \"context\": \"analysis\"," +
                                " \"opsdbproperty\": \"Zack\"" +
                                "}" +
                                "]," +

                                // webhook
                                "\"webhook\": [" +
                                "{" +
                                " \"id\": 36," +
                                " \"name\": \"test webhook contact\"," +
                                " \"endpoint\": \"https://test.endpoint.url\"" +
                                "}," +
                                "{" +
                                " \"id\": 38," +
                                " \"name\": \"test second webhook contact\"," +
                                " \"endpoint\": \"https://test.second.endpoint.url\"" +
                                "}" +
                                "]" +
                                "}",
                        Contacts.builder()
                                .setEmailContacts(
                                        EmailContact.builder()
                                                .setId(21)
                                                .setName("foo@opentsdb.net")
                                                .setEmail("foo@opentsdb.net")
                                                .build(),
                                        EmailContact.builder()
                                                .setId(22)
                                                .setName("foo@opentsdb.net")
                                                .setEmail("foo@opentsdb.net")
                                                .build(),
                                        EmailContact.builder()
                                                .setId(0)
                                                .setName("bar@opentsdb.net")
                                                .setEmail("bar@opentsdb.net")
                                                .build(),
                                        EmailContact.builder()
                                                .setId(0)
                                                .setName("bar@opentsdb.net")
                                                .setEmail("bar@opentsdb.net")
                                                .build()
                                )
                                .setSlackContacts(
                                        SlackContact.builder()
                                                .setId(34)
                                                .setName("#test1")
                                                .setEndpoint("test1")
                                                .build()
                                )
                                .setOpsGenieContacts(
                                        OpsGenieContact.builder()
                                                .setId(27)
                                                .setName("test-4")
                                                .setApiKey("112wqeqweqweqdsfsdfdsdfsdfsdfsdfsdfs")
                                                .build()
                                )
                                .setOcContacts(
                                        OcContact.builder()
                                                .setId(28)
                                                .setName("test")
                                                .setContext("test")
                                                .setDisplayCount("2")
                                                .setOpsdbProperty("OpenTSDB")
                                                .build(),
                                        OcContact.builder()
                                                .setId(32)
                                                .setName("new-oc")
                                                .setContext("analysis")
                                                .setDisplayCount("1")
                                                .setOpsdbProperty("Zack")
                                                .build()
                                )
                                .setWebhookContacts(
                                        WebhookContact.builder()
                                            .setId(36)
                                            .setName("test webhook contact")
                                            .setEndpoint("https://test.endpoint.url")
                                            .build(),
                                        WebhookContact.builder()
                                            .setId(38)
                                            .setName("test second webhook contact")
                                            .setEndpoint("https://test.second.endpoint.url")
                                            .build()
                                )
                                .build()
                )
        );
    }
}
