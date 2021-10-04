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

import java.util.Objects;

import javax.annotation.concurrent.ThreadSafe;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroupSerializer;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.ContactSerializerFactory;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.MetaSerializers;

@ThreadSafe
public class MessageKitSerializer
        extends AbstractMessageKitSerializer<MessageKit>
{

    /* ------------ Constants ------------ */

    private static final MessageKitSerializer DEFAULT_INSTANCE =
            new MessageKitSerializer(
                    new ContactSerializerFactory(),
                    new MetaSerializers(),
                    new AlertGroupSerializer()
            );

    /* ------------ Static Methods ------------ */

    public static MessageKitSerializer instance()
    {
        return DEFAULT_INSTANCE;
    }

    /* ------------ Fields ------------ */

    private final AbstractSerializer<AlertGroup> alertGroupSerializer;

    /* ------------ Constructor ------------ */

    protected MessageKitSerializer(
            final ContactSerializerFactory contactSerializerFactory,
            final MetaSerializers metaSerializers,
            final AbstractSerializer<AlertGroup> alertGroupSerializer)
    {
        super(contactSerializerFactory, metaSerializers);
        Objects.requireNonNull(alertGroupSerializer,
                "alertGroupSerializer cannot be null");
        this.alertGroupSerializer = alertGroupSerializer;
    }

    /* ------------ Methods ------------ */

    @Override
    protected Class<MessageKit> getSerializableClass()
    {
        return MessageKit.class;
    }

    @Override
    public void write(final Kryo kryo,
                      final Output output,
                      final MessageKit messageKit)
    {
        super.write(kryo, output, messageKit);
        alertGroupSerializer.write(kryo, output, messageKit.getAlertGroup());
    }

    @Override
    public MessageKit read(final Kryo kryo,
                           final Input input,
                           final Class<MessageKit> aClass)
    {
        final MessageKit.Builder<?> builder = MessageKit.builder();
        super.read(builder, kryo, input);

        final AlertGroup alertGroup =
                alertGroupSerializer.read(kryo, input, AlertGroup.class);

        return builder
                .setAlertGroup(alertGroup)
                .build();
    }
}
