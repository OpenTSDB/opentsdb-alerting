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
import net.opentsdb.horizon.alerts.enums.AlertState;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class HealthCheckAlertEvent extends AlertEvent {

    private String application = "ALERTS";

    private String namespaceForData;

    //Typically will not have missing
    private AlertState[] statuses = new AlertState[0];

    private long[] timestamps = new long[0];

    private int threshold;

    private boolean isMissingRecovery;

    //Serves dual purpose of referring
    // to missing interval in case of missing
    // to recovery interval in case of auto recovery
    private int intervalInSeconds;

    //This doesnt gel well with a builder.
    // Will gradually move the code to refactor
    @Override
    public void write(Kryo kryo, Output output) {
        super.write(kryo,output);
        output.writeString(namespaceForData);
        output.writeString(application);
        output.writeInt(statuses.length);
        for(int i= 0; i < statuses.length; i++) {
            output.writeByte(statuses[i].getId());
            output.writeLong(timestamps[i]);
        }
        output.writeInt(threshold);
        output.writeBoolean(isMissingRecovery);
        output.writeInt(intervalInSeconds);
    }

    @Override
    public void read(Kryo kryo, Input input) {
        super.read(kryo, input);
        this.namespaceForData = input.readString();
        this.application = input.readString();
        final int length = input.readInt();
        this.statuses = new AlertState[length];
        this.timestamps = new long[length];
        for(int i =0 ; i < length; i++) {
            this.statuses[i] = AlertState.fromId(input.readByte());
            this.timestamps[i] = input.readLong();
        }
        this.threshold = input.readInt();
        this.isMissingRecovery = input.readBoolean();
        this.intervalInSeconds = input.readInt();
    }

    public String toString() {
        return super.toString() + " " +String.format("app: %s namespace for data: %s statuses: %s timestamps: %s threshold: %s isau %s interval in seconds %s",application,
                namespaceForData, Arrays.toString(statuses), Arrays.toString(timestamps), threshold, isMissingRecovery, intervalInSeconds);
    }

}
