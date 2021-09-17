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
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroupSerializer;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.EmailContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.EmailMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PrePackedMessageKitSerializerTest {

    static final PrePackedMessageKitSerializer PRE_PACKED_MESSAGE_SERIALIZER =
            PrePackedMessageKitSerializer.instance();

    static final MessageKitSerializer MESSAGE_SERIALIZER =
            MessageKitSerializer.instance();

    static final AlertGroupSerializer ALERT_GROUP_SERIALIZER =
            new AlertGroupSerializer();

    @Injectable
    public Kryo kryo;

    @Test
    public void testGetSerializableClass()
    {
        assertEquals(PRE_PACKED_MESSAGE_SERIALIZER.getSerializableClass(),
                PrePackedMessageKit.class);
    }

    @Test
    public void testWriteRead()
    {
        // Shared AlertGroup object and serialized bytes.
        final AlertGroup alertGroup = AlertGroup.builder()
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
                                .setTimestampSec(10)
                                .setNamespace("OpenTSDB")
                                .setComparator(Comparator.EQUALS)
                                .setStateFrom(State.BAD)
                                .setState(State.GOOD)
                                .setMetric("cpu.user")
                                .setSampler(WindowSampler.ALL_OF_THE_TIMES)
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
                .build();
        final byte[] alertGroupBytes =
                ALERT_GROUP_SERIALIZER.toBytes(alertGroup);

        // Expected deserialized MessageKit.
        final MessageKit expected = MessageKit.builder()
                .setType(Contact.Type.EMAIL)
                .setAlertGroup(alertGroup)
                .setContacts(
                        EmailContact.builder()
                                .setName("bob@opentsdb.net")
                                .setEmail("bob@opentsdb.net")
                                .build()
                )
                .setMeta(EmailMeta.builder()
                        .setSubject("Very important alert")
                        .setBody("Nope, not really")
                        .build())
                .build();

        // PrePackedMessageKit to be serialized and then deserialized as
        // MessageKit.
        final PrePackedMessageKit prePackedMessageKit =
                PrePackedMessageKit.builder()
                        .setType(Contact.Type.EMAIL)
                        .setSerializedAlertGroup(alertGroupBytes)
                        .setContacts(
                                EmailContact.builder()
                                        .setName("bob@opentsdb.net")
                                        .setEmail("bob@opentsdb.net")
                                        .build()
                        )
                        .setMeta(EmailMeta.builder()
                                .setSubject("Very important alert")
                                .setBody("Nope, not really")
                                .build())
                        .build();

        // Test
        final Output output = new Output(1024, -1);
        PRE_PACKED_MESSAGE_SERIALIZER
                .write(kryo, output, prePackedMessageKit);

        final MessageKit actual = MESSAGE_SERIALIZER.read(
                kryo, new Input((output.toBytes())), MessageKit.class);

        assertEquals(expected, actual);
    }

    @Test
    public void testRead()
    {
        final Input input = new Input();

        assertThrows(UnsupportedOperationException.class, () -> {
            PRE_PACKED_MESSAGE_SERIALIZER
                    .read(kryo, input, PrePackedMessageKit.class);
        });
    }
}
