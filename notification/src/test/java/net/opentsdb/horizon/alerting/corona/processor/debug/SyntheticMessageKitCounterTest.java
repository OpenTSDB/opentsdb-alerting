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

package net.opentsdb.horizon.alerting.corona.processor.debug;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.SlackMeta;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class SyntheticMessageKitCounterTest {

    @Mocked
    Processor<MessageKit> next;

    @Mocked
    AppMonitor appMonitor;

    @Test
    void process() {
        final SyntheticMessageKitCounter tested =
                new SyntheticMessageKitCounter(
                        next,
                        Collections.singletonList("1234"),
                        appMonitor
                );

        final MessageKit mk1 = MessageKit.builder()
                .setType(Contact.Type.SLACK)
                .setMeta(SlackMeta.builder().build())
                .setContacts(null, null)
                .setAlertGroup(AlertGroup.builder()
                        .setAlerts(null, null, null)
                        .setGroupKey(GroupKey.builder()
                                .setNamespace("OpenTSDB")
                                .setAlertType(AlertType.SINGLE_METRIC)
                                .setAlertId(1234)
                                .build()
                        ).build()
                ).build();
        final MessageKit mk2 = MessageKit.builder()
                .setType(Contact.Type.SLACK)
                .setMeta(SlackMeta.builder().build())
                .setContacts(null, null)
                .setAlertGroup(AlertGroup.builder()
                        .setAlerts(null, null)
                        .setGroupKey(GroupKey.builder()
                                .setNamespace("OpenTSDB")
                                .setAlertType(AlertType.SINGLE_METRIC)
                                .setAlertId(9999)
                                .build()
                        ).build()
                ).build();

        new Expectations() {{
            next.process(mk1);
            next.process(mk2);
            next.process(mk1);
            next.process(mk2);
            times = 1;
        }};

        tested.process(mk1);
        tested.process(mk2);
        tested.process(mk1);
        tested.process(mk1);

        new Verifications() {{
            appMonitor.countSyntheticAlertReceived(3, 1234);
            appMonitor.countSyntheticAlertReceived(3, 1234);
        }};
    }
}