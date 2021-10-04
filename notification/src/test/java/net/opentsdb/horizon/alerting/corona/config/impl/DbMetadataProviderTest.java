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

package net.opentsdb.horizon.alerting.corona.config.impl;

import mockit.Expectations;
import mockit.Injectable;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.AbstractAlertConfig;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.Notification;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.Recipient;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.impl.NAlertConfig;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.EmailContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OpsGenieContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.SlackContact;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DbMetadataProviderTest {

    @Injectable
    DbConfigFetcher<NAlertConfig> configFetcher;

    @BeforeAll
    static void stubMonitoring() {
        AppMonitor.initialize(
                AppMonitor.config()
                        .setApplication("corona.test")
                        .setNamespace("Skhegay")
                        .setHost("localhost")
        );
    }

    // Test a fix for the Config API contact id changes:
    @Test
    void testRecipientToContactMapping() {
        final String OpenTSDB = "OpenTSDB";
        final DbMetadataProvider testedMetaProvider =
                DbMetadataProvider.create(configFetcher);

        new Expectations() {{
            configFetcher.getNamespaces();
            result = Optional.of(Collections.singletonList(OpenTSDB));

            configFetcher.getContacts(OpenTSDB);
            result = Optional.of(CONTACTS);

            configFetcher.getAlertConfigs(OpenTSDB);
            result = Optional.of(ALERT_CONFIGS);
        }};
        testedMetaProvider.updateAllMetadata();

        final Contacts actual = testedMetaProvider.getContacts(1L)
                .orElseThrow(() -> new RuntimeException("must return contacts"));

        final Contacts expected = Contacts.builder()
                .setEmailContacts(
                        EmailContact.builder()
                                .setId(21)
                                .setName("foo@opentsdb.net")
                                .setEmail("foo@opentsdb.net")
                                .build(),
                        EmailContact.builder()
                                .setId(Contact.UNKNOWN_ID)
                                .setName("bar@opentsdb.net")
                                .setEmail("bar@opentsdb.net")
                                .build(),
                        EmailContact.builder()
                                .setId(Contact.UNKNOWN_ID)
                                .setName("bar@opentsdb.net")
                                .setEmail("bar@opentsdb.net")
                                .build()
                )
                .setOpsGenieContacts(
                        OpsGenieContact.builder()
                                .setId(27)
                                .setName("test-4")
                                .setApiKey("112wqeqweqweqdsfsdfdsdfsdfsdfsdfsdfs")
                                .build()
                )
                .build();

        assertEquals(expected, actual);
    }

    private static final Contacts CONTACTS =
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
                                    .setId(Contact.UNKNOWN_ID)
                                    .setName("bar@opentsdb.net")
                                    .setEmail("bar@opentsdb.net")
                                    .build(),
                            EmailContact.builder()
                                    .setId(Contact.UNKNOWN_ID)
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
                    .build();

    private static final List<NAlertConfig> ALERT_CONFIGS =
            Collections.singletonList(NAlertConfig.builder()
                    .setId(1L)
                    .setName("test 1")
                    .setNamespace("OpenTSDB")
                    .setType(AbstractAlertConfig.Type.SIMPLE)
                    .setEnabled(true)
                    .setLabels(Arrays.asList("prod", "us-west-1"))
                    .setGroupingRules(Arrays.asList("colo", "host"))
                    .setNotification(Notification.builder()
                            .setSubject("test subject")
                            .setBody("test body")
                            .setTransitionsToNotify(Collections.singletonList("goodToBad"))
                            .setRecipients(new HashMap<Contact.Type, List<Recipient>>() {{
                                put(Contact.Type.EMAIL, Arrays.asList(
                                        // This recipient will be resolved by id.
                                        Recipient.builder()
                                                .setId(21)
                                                .setName("foo@opentsdb.net")
                                                .build(),
                                        // This recipient will be resolved by name
                                        // because the corresponding contact has
                                        // invalid id.
                                        Recipient.builder()
                                                .setId(1024)
                                                .setName("bar@opentsdb.net")
                                                .build(),
                                        // This recipient will be resolved by name
                                        // because ids are invalid for both, the
                                        // recipient and the corresponding contact.
                                        Recipient.builder()
                                                .setId(Contact.UNKNOWN_ID)
                                                .setName("bar@opentsdb.net")
                                                .build()
                                        )
                                );
                                put(Contact.Type.OPSGENIE, Collections.singletonList(
                                        // This recipient will be resolved by id.
                                        // the corresponding contact has `test-4`
                                        // for some weird sync reason.
                                        Recipient.builder()
                                                .setId(27)
                                                .setName("not test-4")
                                                .build()
                                ));
                            }})
                            .setRunbookId("RB007")
                            .setOpsGeniePriority("P5")
                            .setOcSeverity(OcSeverity.SEV_5)
                            .setOcTier(OcTier.TIER_2)
                            .build())
                    .build()
            );
}
