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

import javax.annotation.concurrent.ThreadSafe;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.opentsdb.horizon.alerting.corona.model.contact.impl.ContactSerializerFactory;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.MetaSerializers;

@ThreadSafe
public class PrePackedMessageKitSerializer
        extends AbstractMessageKitSerializer<PrePackedMessageKit>
{

    /* ------------ Constants ------------ */

    private static final PrePackedMessageKitSerializer DEFAULT_INSTANCE =
            new PrePackedMessageKitSerializer(
                    new ContactSerializerFactory(),
                    new MetaSerializers()
            );

    /* ------------ Static Methods ------------ */

    public static PrePackedMessageKitSerializer instance()
    {
        return DEFAULT_INSTANCE;
    }

    /* ------------ Constructor ------------ */

    protected PrePackedMessageKitSerializer(
            final ContactSerializerFactory contactSerializerFactory,
            final MetaSerializers metaSerializers)
    {
        super(contactSerializerFactory, metaSerializers);
    }

    /* ------------ Methods ------------ */

    @Override
    protected Class<PrePackedMessageKit> getSerializableClass()
    {
        return PrePackedMessageKit.class;
    }

    @Override
    public void write(final Kryo kryo,
                      final Output output,
                      final PrePackedMessageKit messageKit)
    {
        super.write(kryo, output, messageKit);
        output.writeBytes(messageKit.getAlertGroupBytes());
    }

    /**
     * This class cannot be used for deserialization.
     * Use {@link MessageKitSerializer}. The byte array produced by this
     * serializer, by design, is decodable by the {@link MessageKitSerializer}.
     *
     * @throws UnsupportedOperationException this class must not be deserialized
     */
    @Override
    public PrePackedMessageKit read(
            final Kryo kryo,
            final Input input,
            final Class<PrePackedMessageKit> aClass)
    {
        throw new UnsupportedOperationException();
    }
}
