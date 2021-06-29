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
import net.opentsdb.horizon.alerts.enums.SummaryType;

import java.util.Arrays;


public class SummarySingleMetricAlertEvent extends SingleMetricAlertEvent {

    //List of timestamps is in the Single Metric
    private double[] valueWhichRaisedTheAlert;

    private SummaryType summary = null;

    public double[] getSummaryValues() {
        return valueWhichRaisedTheAlert;
    }

    public void setSummaryValues(double[] valueWhichRaisedTheAlert) {
        this.valueWhichRaisedTheAlert = valueWhichRaisedTheAlert;
    }

    public SummaryType getSummaryType() {
        return summary;
    }

    public void setSummaryType(SummaryType summary) {
        this.summary = summary;
    }

    @Override
    public void write(Kryo kryo, Output output) {
        super.write(kryo,output);
        output.writeByte(summary.getId());

        int dsize = valueWhichRaisedTheAlert.length;
        output.writeInt(dsize);
        for(int i = 0; i < dsize; i++) {
            output.writeDouble(valueWhichRaisedTheAlert[i]);
        }
    }

    @Override
    public void read(Kryo kryo, Input input) {
        super.read(kryo,input);
        summary = SummaryType.getSummaryFromId(input.readByte());
        int dsize = input.readInt();
        valueWhichRaisedTheAlert = new double[dsize];
        for(int i =0;i < dsize; i++) {
            valueWhichRaisedTheAlert[i] = input.readDouble();
        }
    }

    @Override
    public String toString() {
        if(valueWhichRaisedTheAlert == null) {
            return super.toString() + String.format(" [ summary: %s]"
                    , summary.name());
        } else if(valueWhichRaisedTheAlert.length > 60) {

            return super.toString() + String.format(" [summary values size: %s , summary: %s]"
                    , valueWhichRaisedTheAlert.length, summary.name());

        } else {
            return super.toString() + String.format(" [summary values : %s , summary: %s]"
                    , Arrays.toString(valueWhichRaisedTheAlert), summary.name());
        }

    }
}
