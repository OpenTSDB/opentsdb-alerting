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

package net.opentsdb.horizon.alerting.corona.model.contact.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.opentsdb.horizon.alerting.corona.model.contact.AbstractContactSerializer;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.WebhookContact.Builder;

public class WebhookContactSerializer
        extends AbstractContactSerializer<WebhookContact, Builder<?>>
{

    public WebhookContactSerializer()
    {
        super(WebhookContact::builder);
    }

    @Override
    protected Class<WebhookContact> getSerializableClass()
    {
        return WebhookContact.class;
    }

    @Override
    protected void writeAddedFields(final Kryo kryo,
                                    final Output output,
                                    final WebhookContact contact)
    {
        output.writeString(contact.getEndpoint());
    }

    @Override
    protected void readAddedFields(final WebhookContact.Builder<?> builder,
                                   final Kryo kryo,
                                   final Input input,
                                   final Class<WebhookContact> type)
    {
        builder.setEndpoint(input.readString());
    }
}
