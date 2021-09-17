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

import net.opentsdb.horizon.alerting.corona.model.alert.Summary;

public class SingleMetricSummaryAlertSerializer
        extends SingleMetricAlertSerializer<
        SingleMetricSummaryAlert,
        SingleMetricSummaryAlert.Builder<?>
        >
{

    /* ------------ Constructors ------------ */

    public SingleMetricSummaryAlertSerializer()
    {
        super(SingleMetricSummaryAlert::builder);
    }

    /* ------------ Methods ------------ */

    @Override
    protected void writeAddedFields(final Kryo kryo,
                                    final Output output,
                                    final SingleMetricSummaryAlert alert)
    {
        super.writeAddedFields(kryo, output, alert);
        output.writeByte(alert.getSummary().getId());
        writeDoubleArray(output, alert.getSummaryValues());

    }

    @Override
    protected void readAddedFields(
            final SingleMetricSummaryAlert.Builder<?> builder,
            final Kryo kryo,
            final Input input,
            final Class<SingleMetricSummaryAlert> type)
    {
        super.readAddedFields(builder, kryo, input, type);
        builder.setSummary(Summary.valueFrom(input.readByte()))
                .setSummaryValues(readDoubleArray(input));
    }

    @Override
    protected Class<SingleMetricSummaryAlert> getSerializableClass()
    {
        return SingleMetricSummaryAlert.class;
    }
}
