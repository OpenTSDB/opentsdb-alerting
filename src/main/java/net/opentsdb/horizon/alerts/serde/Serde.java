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

package net.opentsdb.horizon.alerts.serde;

import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.EventAlertEvent;
import net.opentsdb.horizon.alerts.model.HealthCheckAlertEvent;
import net.opentsdb.horizon.alerts.model.PeriodOverPeriodAlertEvent;
import net.opentsdb.horizon.alerts.model.SingleMetricAlertEvent;
import net.opentsdb.horizon.alerts.model.SummarySingleMetricAlertEvent;
import net.opentsdb.horizon.alerts.model.tsdb.YmsEvent;
import net.opentsdb.horizon.alerts.model.tsdb.YmsStatusEvent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Serde {

    private Kryo kryo = new Kryo();

    public byte[] kryoSerializeEvent(AlertEvent event) {
        Output output = new Output(1024, -1);
        //Version byte change version to 2 for adding window sampler
        //Version byte change version to 3 for adding prev signal instead of transition
        //Version byte change version to 4 for adding alert state
        //Version byte change version to 5 for adding snooze boolean
        //Version byte left version 5 as a non breaking change for adding event alert
        //Version byte left version 5 as a non breaking change for adding period over period alert
        output.writeByte(5);

        if (event.getClass() == SingleMetricAlertEvent.class) {
            output.writeByte(0);
        } else if (event.getClass() == SummarySingleMetricAlertEvent.class) {
            output.writeByte(1);
        } else if (event.getClass() == HealthCheckAlertEvent.class) {
            output.writeByte(2);
        } else if (event.getClass() == EventAlertEvent.class) {
            output.writeByte(3);
        } else if (event.getClass() == PeriodOverPeriodAlertEvent.class) {
            output.writeByte(4);
        } else {
            output.close();
            throw new AssertionError("Event class not registered for serialization!!");
        }

        event.write(kryo, output);
        return output.toBytes();
    }

    public AlertEvent kryoDeserializeEvent(byte[] serialized) {
        Input input = new Input(serialized);

        AlertEvent alertEvent;

        byte version = input.readByte();

        if (version == 4) {
            byte type = input.readByte();

            if (type == 0) {
                alertEvent = new SingleMetricAlertEvent();
            } else if (type == 1) {

                alertEvent = new SummarySingleMetricAlertEvent();

            } else if (type == 2) {

                alertEvent = new HealthCheckAlertEvent();

            } else if (type == 3) {

                alertEvent = new EventAlertEvent();

            } else {
                throw new AssertionError("Unrecognized event type " + type);
            }

        } else {
            throw new AssertionError("Unsupported version in deserializtion");
        }

        alertEvent.read(kryo, input);
        return alertEvent;

    }

    public byte[] kryoSerializeYmsEvent(YmsEvent event) {
        Output output = new Output(1024, -1);

        if (event.getClass() == YmsEvent.class) {
            output.writeByte(2);
        } else if (event.getClass() == YmsStatusEvent.class) {
            output.writeByte(5);
        } else {
            output.close();
            throw new AssertionError("Event class not registered!!");
        }

        event.write(kryo, output);
        output.close();

        return output.toBytes();

    }

    public YmsEvent kryoDeserializeYmsEvent(byte[] serialized) {
        Input input = new Input(serialized);
        YmsEvent event;

        byte type = input.readByte();

        if (type == 2) {
            event = new YmsEvent();

        } else if (type == 5) {
            event = new YmsStatusEvent();
        } else {
            throw new AssertionError("Unrecognized event type " + type);
        }
        event.read(kryo, input);
        return event;

    }
}
