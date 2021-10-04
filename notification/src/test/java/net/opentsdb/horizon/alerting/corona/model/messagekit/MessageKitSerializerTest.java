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

package net.opentsdb.horizon.alerting.corona.model.messagekit;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import mockit.Injectable;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alert.Comparator;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alert.WindowSampler;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricSimpleAlert;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.EmailContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.EmailMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageKitSerializerTest {

    private static final MessageKitSerializer SERIALIZER_UNDER_TEST =
            MessageKitSerializer.instance();

    @Injectable
    public Kryo kryo;

    @Test
    public void testGetSerializableClass()
    {
        assertEquals(SERIALIZER_UNDER_TEST.getSerializableClass(),
                MessageKit.class);
    }

    @Test
    public void testWriteRead()
    {
        final MessageKit messageKit = MessageKit.builder()
                .setType(Contact.Type.EMAIL)
                .setContacts(
                        EmailContact.builder()
                                .setName("bob@opentsdb.net")
                                .setEmail("bob@opentsdb.net")
                                .build()
                )
                .setAlertGroup(AlertGroup.builder()
                        .setGroupKey(GroupKey.builder()
                                .setAlertId(0)
                                .setAlertType(AlertType.SINGLE_METRIC)
                                .setNamespace("OpenTSDB")
                                .setKeys("host")
                                .setValues("localhost")
                                .build())
                        .setAlerts(
                                SingleMetricSimpleAlert.builder()
                                        .setId(0)
                                        .setTimestampSec(1000)
                                        .setNamespace("OpenTSDB")
                                        .setComparator(Comparator.EQUALS)
                                        .setStateFrom(State.BAD)
                                        .setState(State.GOOD)
                                        .setMetric("cpu.user")
                                        .setSampler(WindowSampler.AT_LEAST_ONCE)
                                        .build(),
                                SingleMetricSimpleAlert.builder()
                                        .setId(1)
                                        .setTimestampSec(1000)
                                        .setNamespace("OpenTSDB")
                                        .setComparator(Comparator.GREATER_THAN)
                                        .setStateFrom(State.BAD)
                                        .setState(State.GOOD)
                                        .setMetric("cpu.user")
                                        .setSampler(WindowSampler.ALL_OF_THE_TIMES)
                                        .setIsSnoozed(true)
                                        .build()
                        )
                        .build())
                .setMeta(EmailMeta.builder()
                        .setSubject("Very important alert")
                        .setBody("Nope, not really")
                        .build())
                .build();

        // Serialize
        final Output output = new Output(1024, -1);
        SERIALIZER_UNDER_TEST.write(kryo, output, messageKit);

        // Deserialize
        final Input input = new Input(output.toBytes());
        final MessageKit deserialized =
                SERIALIZER_UNDER_TEST.read(kryo, input, MessageKit.class);

        // Compare
        assertEquals(deserialized, messageKit);
    }
}
