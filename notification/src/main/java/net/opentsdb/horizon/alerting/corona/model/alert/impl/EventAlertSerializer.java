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

package net.opentsdb.horizon.alerting.corona.model.alert.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.opentsdb.horizon.alerting.corona.model.alert.AlertSerializer;
import net.opentsdb.horizon.alerting.corona.model.alert.Event;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.EventAlert.Builder;

public class EventAlertSerializer
        extends AlertSerializer<EventAlert, Builder<?>>
{

    /* ------------ Constructors ------------ */

    public EventAlertSerializer()
    {
        super(EventAlert::builder);
    }

    /* ------------ Methods ------------ */

    @Override
    protected void writeAddedFields(final Kryo kryo,
                                    final Output output,
                                    final EventAlert alert)
    {
        output.writeString(alert.getDataNamespace());
        output.writeString(alert.getFilterQuery());
        output.writeInt(alert.getThreshold());
        output.writeInt(alert.getWindowSizeSec());
        output.writeInt(alert.getCount());

        final Event event = alert.getEvent();
        output.writeBoolean(event != null);
        if (event != null) {
            event.write(kryo, output);
        }
    }

    @Override
    protected void readAddedFields(final EventAlert.Builder<?> builder,
                                   final Kryo kryo,
                                   final Input input,
                                   final Class<EventAlert> type)
    {
        builder.setDataNamespace(input.readString())
                .setFilterQuery((input.readString()))
                .setThreshold(input.readInt())
                .setWindowSizeSec(input.readInt())
                .setCount(input.readInt());

        final boolean hasEvent = input.readBoolean();
        if (hasEvent) {
            final Event event = new Event();
            event.read(kryo, input);
            builder.setEvent(event);
        }
    }

    @Override
    protected Class<EventAlert> getSerializableClass()
    {
        return EventAlert.class;
    }
}
