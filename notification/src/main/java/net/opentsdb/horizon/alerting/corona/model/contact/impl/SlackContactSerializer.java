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

public class SlackContactSerializer extends
        AbstractContactSerializer<SlackContact, SlackContact.Builder<?>>
{

    public SlackContactSerializer()
    {
        super(SlackContact::builder);
    }

    @Override
    protected Class<SlackContact> getSerializableClass()
    {
        return SlackContact.class;
    }

    @Override
    protected void writeAddedFields(final Kryo kryo,
                                    final Output output,
                                    final SlackContact contact)
    {
        output.writeString(contact.getEndpoint());
    }

    @Override
    protected void readAddedFields(final SlackContact.Builder<?> builder,
                                   final Kryo kryo,
                                   final Input input,
                                   final Class<SlackContact> type)
    {
        builder.setEndpoint(input.readString());
    }
}
