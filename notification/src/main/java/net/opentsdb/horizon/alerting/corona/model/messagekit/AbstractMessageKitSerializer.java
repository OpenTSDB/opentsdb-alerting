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

import java.util.List;
import java.util.Objects;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.ContactSerializerFactory;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.EmailContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.WebhookContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OpsGenieContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.SlackContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.Meta;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.MetaSerializers;

public abstract class AbstractMessageKitSerializer<M extends AbstractMessageKit>
        extends AbstractSerializer<M>
{

    /* ------------ Fields ------------ */

    private final ContactSerializerFactory contactSerializerFactory;

    private final MetaSerializers metaSerializers;


    /* ------------ Constructors ------------ */

    public AbstractMessageKitSerializer(
            final ContactSerializerFactory contactSerializerFactory,
            final MetaSerializers metaSerializers)
    {
        Objects.requireNonNull(contactSerializerFactory,
                "contactSerializerFactory cannot be null");
        Objects.requireNonNull(metaSerializers,
                "metaSerializers cannot be null");
        this.contactSerializerFactory = contactSerializerFactory;
        this.metaSerializers = metaSerializers;
    }

    /* ------------ Methods ------------ */

    private AbstractSerializer<Contact> getContactSerializer(final Contact.Type type)
    {
        @SuppressWarnings("unchecked")
        final AbstractSerializer<Contact> serializer =
                (AbstractSerializer<Contact>) contactSerializerFactory.get(type);

        return serializer;
    }

    private AbstractSerializer<Meta> getMetaSerializer(final Contact.Type type)
    {
        @SuppressWarnings("unchecked")
        final AbstractSerializer<Meta> serializer =
                (AbstractSerializer<Meta>) metaSerializers.get(type);

        return serializer;
    }

    private Class<? extends Contact> getContactClass(final Contact.Type type)
    {
        switch (type) {
            case EMAIL:
                return EmailContact.class;
            case WEBHOOK:
                return WebhookContact.class;
            case OC:
                return OcContact.class;
            case OPSGENIE:
                return OpsGenieContact.class;
            case SLACK:
                return SlackContact.class;
        }
        throw new IllegalArgumentException("Unknown type: " + type.name());
    }

    /**
     * Subclasses have to override and call this method in their
     * implementation.
     *
     * @param kryo Kryo instance, not used
     * @param output output to write to
     * @param messageKit messageKit to serialize
     */
    @Override
    public void write(final Kryo kryo,
                      final Output output,
                      final M messageKit)
    {
        final Contact.Type type = messageKit.getType();
        final AbstractSerializer<Contact> contactSerializer =
                getContactSerializer(type);
        final AbstractSerializer<Meta> metaSerializer = getMetaSerializer(type);

        output.writeString(type.name());
        writeCollection(
                kryo,
                output,
                messageKit.getContacts(),
                contactSerializer::write
        );
        metaSerializer.write(kryo, output, messageKit.getMeta());
    }

    /**
     * Subclasses have to use this method in their implementation of
     * {@link #read(Kryo, Input, Class)}.
     *
     * @param builder builder to set parameters to
     * @param kryo Kryo instance (not used)
     * @param input input to readAddedFields from
     */
    protected void read(final AbstractMessageKit.Builder<?> builder,
                        final Kryo kryo,
                        final Input input)
    {
        final Contact.Type type = Contact.Type.valueOf(input.readString());

        final Class clazz = getContactClass(type);
        final AbstractSerializer<Contact> contactSerializer =
                getContactSerializer(type);
        final AbstractSerializer<Meta> metaSerializer = getMetaSerializer(type);

        @SuppressWarnings("unchecked")
        final List<? extends Contact> contacts =
                readList(kryo, input, clazz, contactSerializer::read);
        final Meta meta = metaSerializer.read(kryo, input, Meta.class);

        builder.setType(type)
                .setContacts(contacts)
                .setMeta(meta);
    }
}
