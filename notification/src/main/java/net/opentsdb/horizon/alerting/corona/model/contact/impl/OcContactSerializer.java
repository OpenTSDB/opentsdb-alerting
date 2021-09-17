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

public class OcContactSerializer
        extends AbstractContactSerializer<OcContact, OcContact.Builder<?>>
{

    public OcContactSerializer()
    {
        super(OcContact::builder);
    }

    @Override
    protected Class<OcContact> getSerializableClass()
    {
        return OcContact.class;
    }

    @Override
    protected void writeAddedFields(final Kryo kryo,
                                    final Output output,
                                    final OcContact contact)
    {
        output.writeString(contact.getDisplayCount());
        output.writeString(contact.getCustomer());
        output.writeString(contact.getContext());
        output.writeString(contact.getOpsdbProperty());
    }

    @Override
    protected void readAddedFields(final OcContact.Builder<?> builder,
                                   final Kryo kryo,
                                   final Input input,
                                   final Class<OcContact> type)
    {
        builder.setDisplayCount(input.readString())
                .setCustomer(input.readString())
                .setContext(input.readString())
                .setOpsdbProperty(input.readString());
    }
}
