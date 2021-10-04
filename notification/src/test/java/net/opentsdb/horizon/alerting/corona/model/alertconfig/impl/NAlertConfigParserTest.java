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

package net.opentsdb.horizon.alerting.corona.model.alertconfig.impl;

import com.fasterxml.jackson.databind.JsonNode;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.AbstractAlertConfig;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.Notification;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.Recipient;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact.Type;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;
import net.opentsdb.horizon.alerting.corona.testutils.Utils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NAlertConfigParserTest {

    @Test
    void test() {
        final JsonNode configJson = Utils.parseJsonTree("payloads/alertconfig.json");

        final NAlertConfigParser parser = new NAlertConfigParser();
        final NAlertConfig actual = parser.doParse(configJson);

        final Map<Type, List<Recipient>> recipients = new HashMap<>();
        recipients.put(
                Type.EMAIL,
                Collections.singletonList(
                        Recipient.builder()
                                .setName("bob@opentsdb.net")
                                .build()
                )
        );
        final NAlertConfig expected = NAlertConfig.builder()
                .setId(1L)
                .setName("test 1")
                .setNamespace("NS")
                .setType(AbstractAlertConfig.Type.SIMPLE)
                .setEnabled(true)
                .setLabels(Arrays.asList("prod", "us-west-1"))
                .setGroupingRules(Arrays.asList("colo", "host"))
                .setNotification(Notification.builder()
                        .setSubject("test subject")
                        .setBody("test body")
                        .setTransitionsToNotify(Collections.singletonList("goodToBad"))
                        .setRecipients(recipients)
                        .setRunbookId("RB007")
                        .setOpsGeniePriority("P5")
                        .setOpsGenieAutoClose(true)
                        .setOcSeverity(OcSeverity.SEV_5)
                        .setOcTier(OcTier.TIER_2)
                        .build())
                .build();

        assertEquals(expected, actual);
    }

    @Test
    void testAlert3214() {
        final JsonNode configJson = Utils.parseJsonTree("payloads/alert-3214.json");

        final NAlertConfigParser parser = new NAlertConfigParser();
        final NAlertConfig actual = parser.doParse(configJson);

        final Map<Type, List<Recipient>> recipients = new HashMap<>();
        recipients.put(
                Type.EMAIL,
                Collections.singletonList(
                        Recipient.builder()
                                .setId(183)
                                .setName("no-reply@opentsdb.net")
                                .build()
                )
        );
        recipients.put(
                Type.WEBHOOK,
                Collections.singletonList(
                        Recipient.builder()
                                .setId(2902)
                                .setName("test-webook")
                                .build())
        );
        recipients.put(
                Type.OC,
                Collections.singletonList(
                        Recipient.builder()
                                .setId(35)
                                .setName("oc")
                                .build())
        );
        recipients.put(
                Type.SLACK,
                Collections.singletonList(
                        Recipient.builder()
                                .setId(151)
                                .setName("NS-test")
                                .build())
        );
        final NAlertConfig expected = NAlertConfig.builder()
                .setId(3214L)
                .setName("[3214] OpenTSDB Notification Synthetic Alert")
                .setNamespace("NS")
                .setType(AbstractAlertConfig.Type.SIMPLE)
                .setEnabled(true)
                .setLabels(Arrays.asList("synthetic", "opentsdb-notification"))
                .setNotification(Notification.builder()
                        .setSubject("OpenTSDB Notification Synthetic Alert")
                        .setBody("You should see the alert coming every 5 minutes (as nag)\n")
                        .setTransitionsToNotify(Arrays.asList("goodToBad", "badToGood"))
                        .setRecipients(recipients)
                        .setRunbookId("")
                        .setOpsGeniePriority("P5")
                        .setOpsGenieAutoClose(false)
                        .setOpsGenieTags(Arrays.asList("hello", "world"))
                        .setOcSeverity(OcSeverity.SEV_1)
                        .setOcTier(OcTier.TIER_4)
                        .build())
                .build();

        assertEquals(expected, actual);
    }
}