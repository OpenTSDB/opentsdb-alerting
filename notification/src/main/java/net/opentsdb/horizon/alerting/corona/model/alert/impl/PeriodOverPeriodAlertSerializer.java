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
import net.opentsdb.horizon.alerting.corona.model.alert.ThresholdType;
import net.opentsdb.horizon.alerting.corona.model.alert.ThresholdUnit;

public class PeriodOverPeriodAlertSerializer
        extends AlertSerializer<
        PeriodOverPeriodAlert,
        PeriodOverPeriodAlert.Builder<?>
        > {

    public PeriodOverPeriodAlertSerializer() {
        super(PeriodOverPeriodAlert::builder);
    }

    @Override
    protected void writeAddedFields(final Kryo kryo,
                                    final Output output,
                                    final PeriodOverPeriodAlert alert) {
        // Fields for information display.

        output.writeString(alert.getMetric());
        output.writeDouble(alert.getObservedValue());
        output.writeDouble(alert.getPredictedValue());
        output.writeDouble(alert.getUpperWarnValue());
        output.writeDouble(alert.getUpperBadValue());
        output.writeDouble(alert.getLowerWarnValue());
        output.writeDouble(alert.getLowerBadValue());
        output.writeByte(alert.getBreachedThresholdType().getId());

        // Config settings.

        output.writeDouble(alert.getUpperWarnThreshold());
        output.writeDouble(alert.getUpperBadThreshold());
        output.writeByte(alert.getUpperThresholdUnit().getId());
        output.writeDouble(alert.getLowerWarnThreshold());
        output.writeDouble(alert.getLowerBadThreshold());
        output.writeByte(alert.getLowerThresholdUnit().getId());

        // Data for visualization.

        writeLongs(output, alert.getTimestampsSec());
        writeDoubles(output, alert.getObservedValues());
        writeDoubles(output, alert.getPredictedValues());

        writeDoubles(output, alert.getUpperWarnValues().orElse(null));
        writeDoubles(output, alert.getUpperBadValues().orElse(null));
        writeDoubles(output, alert.getLowerWarnValues().orElse(null));
        writeDoubles(output, alert.getLowerBadValues().orElse(null));
    }

    @Override
    protected void readAddedFields(final PeriodOverPeriodAlert.Builder<?> builder,
                                   final Kryo kryo,
                                   final Input input,
                                   final Class<PeriodOverPeriodAlert> type) {
        // Fields for information display.
        builder.setMetric(input.readString())
                .setObservedValue(input.readDouble())
                .setPredictedValue(input.readDouble())
                .setUpperWarnValue(input.readDouble())
                .setUpperBadValue(input.readDouble())
                .setLowerWarnValue(input.readDouble())
                .setLowerBadValue(input.readDouble())
                .setBreachedThresholdType(
                        ThresholdType.valueOf(input.readByte())
                );

        // Config settings.
        builder.setUpperWarnThreshold(input.readDouble())
                .setUpperBadThreshold(input.readDouble())
                .setUpperThresholdUnit(ThresholdUnit.valueOf(input.readByte()))
                .setLowerWarnThreshold(input.readDouble())
                .setLowerBadThreshold(input.readDouble())
                .setLowerThresholdUnit(ThresholdUnit.valueOf(input.readByte()));

        // Data for visualization.
        builder.setTimestampsSec(readLongs(input))
                .setObservedValues(readDoubles(input))
                .setPredictedValues(readDoubles(input))
                .setUpperWarnValues(readDoubles(input))
                .setUpperBadValues(readDoubles(input))
                .setLowerWarnValues(readDoubles(input))
                .setLowerBadValues(readDoubles(input));
    }

    @Override
    protected Class<PeriodOverPeriodAlert> getSerializableClass() {
        return PeriodOverPeriodAlert.class;
    }

    /*
     To be consistent with Corona's code.
     */

    private static void writeLongs(Output output, long[] vals) {
        if (vals == null) {
            output.writeInt(-1);
            return;
        }
        output.writeInt(vals.length);
        for (long val : vals) {
            output.writeLong(val);
        }
    }

    private static long[] readLongs(Input input) {
        final int n = input.readInt();
        if (n == -1) {
            return null;
        }
        long[] res = new long[n];
        for (int i = 0; i < n; i++) {
            res[i] = input.readLong();
        }
        return res;
    }

    private static void writeDoubles(Output output, double[] vals) {
        if (vals == null) {
            output.writeInt(-1);
            return;
        }
        output.writeInt(vals.length);
        for (double val : vals) {
            output.writeDouble(val);
        }
    }

    private static double[] readDoubles(Input input) {
        final int n = input.readInt();
        if (n == -1) {
            return null;
        }
        double[] res = new double[n];
        for (int i = 0; i < n; i++) {
            res[i] = input.readDouble();
        }
        return res;
    }
}
