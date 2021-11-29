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

package net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl;

import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.HealthCheckAlert;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OcMeta;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthCheckMessageKitViewTest {

    @Test
    void interpolateSubjectBody() {
        MessageKit mk = MessageKit.builder()
                .setType(Contact.Type.OC)
                .setContacts(Contacts.of(OcContact.builder()
                        .setId(12)
                        .setContext("live")
                        .setName("test oc")
                        .setOpsdbProperty("NS")
                        .build())
                )
                .setMeta(OcMeta.builder()
                        .setSubject("job_id: {{job_id}}, status: {{status_message}}")
                        .setBody("status: {{status_message}}")
                        .setOcSeverity(OcSeverity.SEV_2)
                        .setOcTier(OcTier.TIER_2)
                        .setRunbookId("KB343434")
                        .build())
                .setAlertGroup(AlertGroup.builder()
                        .setGroupKey(GroupKey.builder()
                                .setAlertId(42)
                                .setAlertType(AlertType.HEALTH_CHECK)
                                .setKeys("job_id")
                                .setValues("jenkins-5")
                                .setNamespace("NS")
                                .build()
                        )
                        .setAlerts(
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setApplication("jenkins")
                                        .setNamespace("NS")
                                        .setDataNamespace("NS")
                                        .setState(State.GOOD)
                                        .setStateFrom(State.BAD)
                                        .setDetails("Status OK")
                                        .setTimestampSec(1614040207)
                                        .setTags(Collections.singletonMap("job_id", "jenkins-5"))
                                        .build()
                        )
                        .build())
                .build();

        HealthCheckMessageKitView mkView = (HealthCheckMessageKitView) Views.of(mk);
        assertEquals("job_id: jenkins-5, status: Status OK", mkView.interpolateSubject(mkView.getAllViews().get(0)));
        assertEquals("status: Status OK", mkView.interpolateBody(mkView.getAllViews().get(0)));
    }
}