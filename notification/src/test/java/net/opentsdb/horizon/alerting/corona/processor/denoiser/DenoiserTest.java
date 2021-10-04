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

package net.opentsdb.horizon.alerting.corona.processor.denoiser;

import mockit.Injectable;
import mockit.Verifications;
import net.opentsdb.horizon.alerting.corona.TestData;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.HealthCheckAlert;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OpsGenieContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OpsGenieMeta;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DenoiserTest {

    @Injectable
    Processor<MessageKit> dummy;

    @BeforeAll
    static void stubMonitoring() {
        AppMonitor.initialize(
                AppMonitor.config()
                        .setApplication("corona.test")
                        .setNamespace("Skhegay")
                        .setHost("localhost")
        );
    }

    Denoiser getDenoiser() {
        return Denoiser.builder()
                .setEmitterType("opsgenie")
                .setNext(dummy)
                .build();
    }

    @Test
    void process_discardOldAlertsWithSameTags() {
        final MessageKit messageKit = MessageKit.builder()
                .setType(Contact.Type.OPSGENIE)
                .setMeta(OpsGenieMeta.builder()
                        .setSubject("Jenkins Job Result")
                        .setBody("Health Check Alert body.")
                        .setOpsGeniePriority("P5")
                        .setOpsGenieAutoClose(true)
                        .build()
                )
                .setContacts(OpsGenieContact.builder()
                        .setName("Test OpsGenie Contact")
                        .setApiKey("secret.opsgenie.api.key")
                        .build()
                )
                .setAlertGroup(AlertGroup.builder()
                        .setGroupKey(GroupKey.builder()
                                .setAlertId(42)
                                .setAlertType(AlertType.HEALTH_CHECK)
                                .setNamespace("OpenTSDB")
                                .setKeys("env")
                                .setValues("prod")
                                .build()
                        )
                        .setAlerts(Arrays.asList(
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-1")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec())
                                        .setStateFrom(State.GOOD)
                                        .setState(State.BAD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 3))
                                        .setStates(State.BAD, State.WARN, State.GOOD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build(),
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-1")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec() + 1000)
                                        .setStateFrom(State.BAD)
                                        .setState(State.GOOD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 3))
                                        .setStates(State.BAD, State.WARN, State.GOOD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build(),
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-1")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec() + 2000)
                                        .setStateFrom(State.BAD)
                                        .setState(State.GOOD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 3))
                                        .setStates(State.BAD, State.WARN, State.GOOD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build()
                        ))
                        .build()
                )
                .build();

        final Denoiser denoiser = getDenoiser();
        denoiser.process(messageKit);

        new Verifications() {{
            final List<MessageKit> messageKits = new ArrayList<>();
            dummy.process(withCapture(messageKits));
            assertEquals(1, messageKits.size());
            final MessageKit resultingMessageKit = messageKits.get(0);
            Assertions.assertEquals(1, resultingMessageKit.getAlertGroup().getAlerts().size());
        }};
    }

    @Test
    void process_separateTagSetsAreTreatedSeparately() {
        final MessageKit messageKit = MessageKit.builder()
                .setType(Contact.Type.OPSGENIE)
                .setMeta(OpsGenieMeta.builder()
                        .setSubject("Jenkins Job Result")
                        .setBody("Health Check Alert body.")
                        .setOpsGeniePriority("P5")
                        .setOpsGenieAutoClose(true)
                        .build()
                )
                .setContacts(OpsGenieContact.builder()
                        .setName("Test OpsGenie Contact")
                        .setApiKey("secret.opsgenie.api.key")
                        .build()
                )
                .setAlertGroup(AlertGroup.builder()
                        .setGroupKey(GroupKey.builder()
                                .setAlertId(42)
                                .setAlertType(AlertType.HEALTH_CHECK)
                                .setNamespace("OpenTSDB")
                                .setKeys("env")
                                .setValues("prod")
                                .build()
                        )
                        .setAlerts(Arrays.asList(
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-1")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec())
                                        .setStateFrom(State.GOOD)
                                        .setState(State.BAD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 3))
                                        .setStates(State.BAD, State.WARN, State.GOOD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build(),
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-1")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec() + 1000)
                                        .setStateFrom(State.BAD)
                                        .setState(State.GOOD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 3))
                                        .setStates(State.BAD, State.WARN, State.GOOD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build(),
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "another-host-2")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec() + 2000)
                                        .setStateFrom(State.BAD)
                                        .setState(State.GOOD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 3))
                                        .setStates(State.BAD, State.WARN, State.GOOD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build()
                        ))
                        .build()
                )
                .build();

        final Denoiser denoiser = getDenoiser();
        denoiser.process(messageKit);

        new Verifications() {{
            final List<MessageKit> messageKits = new ArrayList<>();
            dummy.process(withCapture(messageKits));
            assertEquals(1, messageKits.size());
            final MessageKit resultingMessageKit = messageKits.get(0);
            Assertions.assertEquals(2, resultingMessageKit.getAlertGroup().getAlerts().size());
        }};
    }
}