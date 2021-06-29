/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.model;

import java.util.Arrays;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.AccessLevel;
import lombok.Setter;

import net.opentsdb.horizon.core.validate.Validate;
import net.opentsdb.horizon.alerts.enums.ThresholdType;
import net.opentsdb.horizon.alerts.enums.ThresholdUnit;

/**
 * Note: fields of this class should be fully populated if possible.
 *
 * TODO: A similar class will appear in Corona Notification, but it will expose
 *       a different API. Keep this in mind if we ever have time to merge two
 *       projects together. At this point, we should just start Corona 2.0. =)
 */
@Setter
public class PeriodOverPeriodAlertEvent extends AlertEvent {

    // Fields for information display.

    private String metric;
    private double observedValue = Double.NaN;
    private double predictedValue = Double.NaN;
    private double upperWarnValue = Double.NaN;
    private double upperBadValue = Double.NaN;
    private double lowerWarnValue = Double.NaN;
    private double lowerBadValue = Double.NaN;
    @Setter(AccessLevel.NONE)
    private ThresholdType breachedThresholdType = ThresholdType.UNKNOWN;

    // Config settings.

    private double upperWarnThreshold = Double.NaN;
    private double upperBadThreshold = Double.NaN;
    private ThresholdUnit upperThresholdUnit = ThresholdUnit.UNKNOWN;

    private double lowerWarnThreshold = Double.NaN;
    private double lowerBadThreshold = Double.NaN;
    private ThresholdUnit lowerThresholdUnit = ThresholdUnit.UNKNOWN;

    // Data for visualization.

    private long[] timestampsSec;
    private double[] observedValues;
    private double[] predictedValues;
    private double[] upperWarnValues;
    private double[] upperBadValues;
    private double[] lowerWarnValues;
    private double[] lowerBadValues;

    /**
     * @param value         breached threshold value
     * @param thresholdType breached threshold type.
     */
    public void setBreachedThresholdValue(final double value,
                                          final ThresholdType thresholdType) {
        Validate.paramNotNull(thresholdType, "thresholdType");
        this.breachedThresholdType = thresholdType;

        switch (thresholdType) {
            case UPPER_WARN:
                upperWarnValue = value;
                break;
            case UPPER_BAD:
                upperBadValue = value;
                break;
            case LOWER_WARN:
                lowerWarnValue = value;
                break;
            case LOWER_BAD:
                lowerBadValue = value;
                break;
        }
    }

    public void setUpperThresholdUnit(ThresholdUnit unit) {
        this.upperThresholdUnit = unit == null ? ThresholdUnit.UNKNOWN : unit;
    }

    public void setLowerThresholdUnit(ThresholdUnit unit) {
        this.lowerThresholdUnit = unit == null ? ThresholdUnit.UNKNOWN : unit;
    }

    @Override
    public void write(Kryo kryo, Output output) {
        super.write(kryo, output);

        // Fields for information display.

        output.writeString(metric);
        output.writeDouble(observedValue);
        output.writeDouble(predictedValue);
        output.writeDouble(upperWarnValue);
        output.writeDouble(upperBadValue);
        output.writeDouble(lowerWarnValue);
        output.writeDouble(lowerBadValue);
        output.writeByte(breachedThresholdType.getId());

        // Config settings.

        output.writeDouble(upperWarnThreshold);
        output.writeDouble(upperBadThreshold);
        output.writeByte(upperThresholdUnit.getId());
        output.writeDouble(lowerWarnThreshold);
        output.writeDouble(lowerBadThreshold);
        output.writeByte(lowerThresholdUnit.getId());

        // Data for visualization.

        writeLongs(output, timestampsSec);
        writeDoubles(output, observedValues);
        writeDoubles(output, predictedValues);
        writeDoubles(output, upperWarnValues);
        writeDoubles(output, upperBadValues);
        writeDoubles(output, lowerWarnValues);
        writeDoubles(output, lowerBadValues);
    }

    @Override
    public void read(Kryo kryo, Input input) {
        super.read(kryo, input);

        // Fields for information display.

        metric = input.readString();
        observedValue = input.readDouble();
        predictedValue = input.readDouble();
        upperWarnValue = input.readDouble();
        upperBadValue = input.readDouble();
        lowerWarnValue = input.readDouble();
        lowerBadValue = input.readDouble();
        breachedThresholdType = ThresholdType.valueOf(input.readByte());

        // Config settings.

        upperWarnThreshold = input.readDouble();
        upperBadThreshold = input.readDouble();
        upperThresholdUnit = ThresholdUnit.valueOf(input.readByte());
        lowerWarnThreshold = input.readDouble();
        lowerBadThreshold = input.readDouble();
        lowerThresholdUnit = ThresholdUnit.valueOf(input.readByte());

        // Data for visualization.

        timestampsSec = readLongs(input);
        observedValues = readDoubles(input);
        predictedValues = readDoubles(input);
        upperWarnValues = readDoubles(input);
        upperBadValues = readDoubles(input);
        lowerWarnValues = readDoubles(input);
        lowerBadValues = readDoubles(input);
    }

    @Override
    public String toString() {
        return "PeriodOverPeriodAlertEvent{" +
                super.toString() +
                ", metric='" + metric + '\'' +
                ", observedValue=" + observedValue +
                ", predictedValue=" + predictedValue +
                ", upperWarnValue=" + upperWarnValue +
                ", upperBadValue=" + upperBadValue +
                ", lowerWarnValue=" + lowerWarnValue +
                ", lowerBadValue=" + lowerBadValue +
                ", breachedThresholdType=" + breachedThresholdType +
                ", upperWarnThreshold=" + upperWarnThreshold +
                ", upperBadThreshold=" + upperBadThreshold +
                ", upperThresholdUnit=" + upperThresholdUnit +
                ", lowerWarnThreshold=" + lowerWarnThreshold +
                ", lowerBadThreshold=" + lowerBadThreshold +
                ", lowerThresholdUnit=" + lowerThresholdUnit +
                ", timestampsSec=" + Arrays.toString(timestampsSec) +
                ", observedValues=" + Arrays.toString(observedValues) +
                ", predictedValues=" + Arrays.toString(predictedValues) +
                ", upperWarnValues=" + Arrays.toString(upperWarnValues) +
                ", upperBadValues=" + Arrays.toString(upperBadValues) +
                ", lowerWarnValues=" + Arrays.toString(lowerWarnValues) +
                ", lowerBadValues=" + Arrays.toString(lowerBadValues) +
                '}';
    }

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
