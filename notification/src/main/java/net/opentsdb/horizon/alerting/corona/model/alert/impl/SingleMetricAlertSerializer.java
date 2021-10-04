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

import net.opentsdb.horizon.alerting.corona.model.Factory;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertSerializer;
import net.opentsdb.horizon.alerting.corona.model.alert.Comparator;
import net.opentsdb.horizon.alerting.corona.model.alert.WindowSampler;

public abstract class SingleMetricAlertSerializer<
        A extends SingleMetricAlert,
        B extends SingleMetricAlert.Builder<A, ?>>
        extends AlertSerializer<A, B>
{

    /* ------------ Constructor ------------ */

    protected SingleMetricAlertSerializer(final Factory<B> builderFactory)
    {
        super(builderFactory);
    }

    /* ------------ Methods ------------ */

    @Override
    protected void writeAddedFields(final Kryo kryo,
                                    final Output output,
                                    final A alert)
    {
        final double[] values = alert.getValuesInWindow();
        final long[] timestamps = alert.getTimestampsSec();
        final int size = values.length;

        output.writeInt(size);
        for (int i = 0; i < size; i++) {
            output.writeDouble(values[i]);
            output.writeLong(timestamps[i], true);
        }
        output.writeString(alert.getMetric());
        output.writeDouble(alert.getThreshold());
        output.writeInt(alert.getWindowSizeSec());
        output.writeByte(alert.getComparator().getId());
        output.writeByte(alert.getSampler().getId());
    }

    @Override
    protected void readAddedFields(final B builder,
                                   final Kryo kryo,
                                   final Input input,
                                   final Class<A> type)
    {
        final int size = input.readInt();
        final double[] values = new double[size];
        final long[] timestamps = new long[size];
        for (int i = 0; i < size; i++) {
            values[i] = input.readDouble();
            timestamps[i] = input.readLong(true);
        }

        builder.setValuesInWindow(values)
                .setTimestampsSec(timestamps)
                .setMetric(input.readString())
                .setThreshold(input.readDouble())
                .setWindowSizeSec(input.readInt())
                .setComparator(Comparator.valueFrom(input.readByte()))
                .setSampler(WindowSampler.valueFrom(input.readByte()));
    }
}
