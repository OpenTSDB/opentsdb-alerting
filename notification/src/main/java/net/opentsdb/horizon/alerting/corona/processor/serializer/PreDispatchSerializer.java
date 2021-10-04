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

package net.opentsdb.horizon.alerting.corona.processor.serializer;

import java.util.List;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact.Type;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.messagekit.PrePackedMessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.Meta;
import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.component.Triple;
import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;

public class PreDispatchSerializer
        extends ChainableProcessor<
        Triple<AlertGroup, Metadata, Contacts>,
        PrePackedMessageKit
        >
{

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(PreDispatchSerializer.class);

    /* ------------ Fields ------------ */

    private final AbstractSerializer<AlertGroup> alertGroupSerializer;

    /* ------------ Constructor ------------ */

    private PreDispatchSerializer(final Builder builder)
    {
        super(builder.next);
        this.alertGroupSerializer = builder.alertGroupSerializer;
    }

    /* ------------ Methods ------------ */

    /**
     * Submits pre-packed message kit of the given type to the next
     * processor.
     *
     * @param alertGroupBytes pre-serialized alert group
     * @param metadata        message metadata
     * @param type            contact type
     * @param contacts        list of contacts
     */
    private void submitOne(final byte[] alertGroupBytes,
                           final Metadata metadata,
                           final Type type,
                           final List<? extends Contact> contacts)
    {
        final Meta meta = Meta.from(type, metadata);
        final PrePackedMessageKit message =
                PrePackedMessageKit.builder()
                        .setType(type)
                        .setMeta(meta)
                        .setContacts(contacts)
                        .setSerializedAlertGroup(alertGroupBytes)
                        .build();
        submit(message);
    }

    private void submit(final AlertGroup alertGroup,
                        final Metadata metadata,
                        final Contacts contacts)
    {
        final byte[] bytes = alertGroupSerializer.toBytes(alertGroup);
        contacts.forEach((type, contactList) -> {
            if (contactList == null || contactList.isEmpty()) {
                return;
            }

            try {
                submitOne(bytes, metadata, type, contactList);
            } catch (Exception e) {
                LOG.error("Submit one: metadata={}, type={}, contacts={}",
                        metadata, type, contactList, e);
            }
        });
    }

    @Override
    public void process(final Triple<AlertGroup, Metadata, Contacts> triple)
    {
        final AlertGroup alertGroup = triple.getFirst();
        final Metadata metadata = triple.getSecond();
        final Contacts contacts = triple.getLast();

        submit(alertGroup, metadata, contacts);
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private Processor<PrePackedMessageKit> next;

        private AbstractSerializer<AlertGroup> alertGroupSerializer;

        private Builder() {}

        public Builder setNext(final Processor<PrePackedMessageKit> next)
        {
            this.next = next;
            return this;
        }

        public Builder setAlertGroupSerializer(
                final AbstractSerializer<AlertGroup> alertGroupSerializer)
        {
            this.alertGroupSerializer = alertGroupSerializer;
            return this;
        }

        public PreDispatchSerializer build()
        {
            return new PreDispatchSerializer(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
