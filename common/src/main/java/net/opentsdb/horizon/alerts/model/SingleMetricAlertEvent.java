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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import net.opentsdb.horizon.alerts.enums.ComparatorType;
import net.opentsdb.horizon.alerts.enums.WindowSampler;
import net.opentsdb.horizon.alerts.helpers.AlertEventHelper;

import java.util.Arrays;

public class SingleMetricAlertEvent extends AlertEvent {

    private double[] valuesInWindow = new double[0];

    //In secs
    private long[] timestamps = new long[0];

    private double threshold;

    private String metricName = null;

    //In secs
    private Integer windowSize = null;

    private ComparatorType comparator = null;

    private WindowSampler windowSampler = null;


    public double[] getValuesInWindow() {
        return valuesInWindow;
    }

    public void setValuesInWindow(double[] valuesInWindow) {
        this.valuesInWindow = valuesInWindow;
    }

    public long[] getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(long[] timestamps) {
        this.timestamps = timestamps;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public ComparatorType getComparator() {
        return comparator;
    }

    public void setComparator(ComparatorType comparator) {
        this.comparator = comparator;
    }

    public Integer getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(Integer windowSize) {
        this.windowSize = windowSize;
    }

    public WindowSampler getWindowSampler() {
        return windowSampler;
    }

    public void setWindowSampler(WindowSampler windowSampler) {
        this.windowSampler = windowSampler;
    }

    @Override
    public void write(Kryo kryo, Output output) {
        super.write(kryo,output);
        int size = valuesInWindow.length;
        output.writeInt(size);
        for (int i =0 ; i < size ; i++) {
            output.writeDouble(valuesInWindow[i]);
            output.writeLong(timestamps[i],true);
        }
        output.writeString(metricName);
        output.writeDouble(threshold);
        output.writeInt(windowSize);
        output.writeByte(comparator.getId());
        output.writeByte(windowSampler.getId());
    }

    @Override
    public void read(Kryo kryo, Input input) {
        super.read(kryo,input);
        int size = input.readInt();

        valuesInWindow = new double[size];
        timestamps = new long[size];
        for(int i = 0; i < size; i++) {
            valuesInWindow[i] = input.readDouble();
            timestamps[i] = input.readLong(true);
        }
        metricName = input.readString();
        threshold = input.readDouble();
        windowSize = input.readInt();
        comparator = ComparatorType.getComparatorTypeFromId(input.readByte());
        windowSampler = WindowSampler.getWindowAggregatorTypeFromId(input.readByte());
    }

    public void absorbValues(double[] values) {

        setValuesInWindow(values);
        //Do nothing
    }

    public void setBreachingIndex(int i) {
        AlertEventHelper.addBreachingIndex(this, i);
    }

    public int getBreachingIndex() {
        return AlertEventHelper.getBreachingIndex(this);
    }

    @Override
    public String toString() {

        if(timestamps.length > 60 ) {
            return super.toString() + String.format(" [ size of timestamps: %s , size of valuesInWindow: %s, threshold: %s,  metricName: %s , comparator: %s ]"
                    , timestamps.length, valuesInWindow.length, threshold, metricName, comparator);

        } else {
            return super.toString() + String.format(" [timestamps: %s , valuesInWindow: %s, threshold: %s,  metricName: %s , comparator: %s ]"
                    , Arrays.toString(timestamps), Arrays.toString(valuesInWindow), threshold, metricName, comparator);
        }

    }
}
