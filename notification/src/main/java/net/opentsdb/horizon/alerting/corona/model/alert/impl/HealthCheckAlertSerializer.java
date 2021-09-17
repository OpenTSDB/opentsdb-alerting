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
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.HealthCheckAlert.Builder;

public class HealthCheckAlertSerializer
        extends AlertSerializer<HealthCheckAlert, Builder<?>>
{

    /* ------------ Constructors ------------ */

    public HealthCheckAlertSerializer()
    {
        super(HealthCheckAlert::builder);
    }

    /* ------------ Methods ------------ */

    @Override
    protected void writeAddedFields(final Kryo kryo,
                                    final Output output,
                                    final HealthCheckAlert alert)
    {
        output.writeString(alert.getDataNamespace());
        output.writeString(alert.getApplication());

        final State[] states = alert.getStates();
        final long[] timestampsSec = alert.getTimestampsSec();
        output.writeInt(states.length);
        for (int i = 0; i < states.length; i++) {
            output.writeByte(states[i].getId());
            output.writeLong(timestampsSec[i]);
        }

        output.writeInt(alert.getThreshold());
        output.writeBoolean(alert.isMissingRecovery());
        output.writeInt(alert.getMissingIntervalSec());
    }

    @Override
    protected void readAddedFields(final HealthCheckAlert.Builder<?> builder,
                                   final Kryo kryo,
                                   final Input input,
                                   final Class<HealthCheckAlert> type)
    {
        final String dataNamespace = input.readString();
        final String application = input.readString();

        final int size = input.readInt();
        final State[] states = new State[size];
        final long[] timestampsSec = new long[size];
        for (int i = 0; i < size; i++) {
            states[i] = State.valueFrom(input.readByte());
            timestampsSec[i] = input.readLong();
        }
        final int threshold = input.readInt();
        final boolean isAutoRecovery = input.readBoolean();
        final int missingInterval = input.readInt();

        builder.setDataNamespace(dataNamespace)
                .setApplication(application)
                .setStates(states)
                .setTimestampsSec(timestampsSec)
                .setThreshold(threshold)
                .setMissingRecovery(isAutoRecovery)
                .setMissingIntervalSec(missingInterval);
    }

    @Override
    protected Class<HealthCheckAlert> getSerializableClass()
    {
        return HealthCheckAlert.class;
    }
}
