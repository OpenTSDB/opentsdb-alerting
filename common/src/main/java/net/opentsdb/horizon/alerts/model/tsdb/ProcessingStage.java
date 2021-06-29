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

package net.opentsdb.horizon.alerts.model.tsdb;

public enum ProcessingStage {
    Null((byte)0), // events from spout carry this stage in the events
    Received((byte)1),
    Normal((byte)2),
    Output((byte)3),
    Requeue((byte)4),
    AdditionalProps((byte)5),
    ComplexMetric((byte)6);

    byte id;

    private ProcessingStage(byte id) {
        this.id = id;
    }

    public ProcessingStage getPreviousStage() {
        return getById((byte)(getId() - 1));
    }

    public boolean pastStage(ProcessingStage stage) {
        return stage.ordinal() - this.ordinal() > 0 ? false : true;
    }

    public byte getId() {
        return id;
    }

    public static ProcessingStage getById(Byte b) {
        switch (b) {
            case 0:
                return Null;
            case 1:
                return Received;
            case 2:
                return Normal;
            case 3:
                return Output;
            case 4:
                return Requeue;
            case 5:
                return AdditionalProps;
            case 6:
                return ComplexMetric;
        }

        throw new AssertionError("Bad identifier for ProcessingStage: " + b);
    }
}
