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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.alert.Alert;

public class GenericAlertSerializer extends AbstractSerializer<Alert> {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(GenericAlertSerializer.class);

    private static final byte SUPPORTED_VERSION = (byte) 5;

    /* ------------ Fields ------------ */

    private final Serializer<SingleMetricSimpleAlert>
            simpleSingleMetricAlertSerializer;

    private final Serializer<SingleMetricSummaryAlert>
            summarySingleMetricAlertSerializer;

    private final Serializer<HealthCheckAlert>
            healthCheckAlertSerializer;

    private final Serializer<EventAlert>
            eventAlertSerializer;

    private final Serializer<PeriodOverPeriodAlert>
            periodOverPeriodAlertSerializer;

    /* ------------ Constructors ------------ */

    public GenericAlertSerializer() {
        this.simpleSingleMetricAlertSerializer =
                new SingleMetricSimpleAlertSerializer();
        this.summarySingleMetricAlertSerializer =
                new SingleMetricSummaryAlertSerializer();
        this.healthCheckAlertSerializer =
                new HealthCheckAlertSerializer();
        this.eventAlertSerializer =
                new EventAlertSerializer();
        this.periodOverPeriodAlertSerializer =
                new PeriodOverPeriodAlertSerializer();
    }

    /* ------------ Methods ------------ */

    @Override
    protected Class<Alert> getSerializableClass() {
        return Alert.class;
    }

    @Override
    public void write(final Kryo kryo, final Output output, final Alert alert) {
        output.writeByte(SUPPORTED_VERSION);

        final Class<?> alertClazz = alert.getClass();
        if (alertClazz == SingleMetricSimpleAlert.class) {
            output.writeByte(0);
            simpleSingleMetricAlertSerializer
                    .write(kryo, output, (SingleMetricSimpleAlert) alert);
        } else if (alertClazz == SingleMetricSummaryAlert.class) {
            output.writeByte(1);
            summarySingleMetricAlertSerializer
                    .write(kryo, output, (SingleMetricSummaryAlert) alert);
        } else if (alertClazz == HealthCheckAlert.class) {
            output.writeByte(2);
            healthCheckAlertSerializer
                    .write(kryo, output, (HealthCheckAlert) alert);
        } else if (alertClazz == EventAlert.class) {
            output.writeByte(3);
            eventAlertSerializer
                    .write(kryo, output, (EventAlert) alert);
        } else if (alertClazz == PeriodOverPeriodAlert.class) {
            output.writeByte(4);
            periodOverPeriodAlertSerializer
                    .write(kryo, output, (PeriodOverPeriodAlert) alert);
        } else {
            throw new IllegalArgumentException(
                    "Unexpected alert type: " + alertClazz);
        }

    }

    @Override
    public Alert read(final Kryo kryo,
                      final Input input,
                      final Class<Alert> type) {
        final byte version = input.readByte();
        if (version != SUPPORTED_VERSION) {
            LOG.trace("Unsupported: version={}, bytes={}",
                    version, input.getBuffer());
            throw new RuntimeException("Unsupported version: " + version);
        }

        final byte classId = input.readByte();
        switch (classId) {
            case 0:
                return simpleSingleMetricAlertSerializer
                        .read(kryo, input, SingleMetricSimpleAlert.class);
            case 1:
                return summarySingleMetricAlertSerializer
                        .read(kryo, input, SingleMetricSummaryAlert.class);
            case 2:
                return healthCheckAlertSerializer
                        .read(kryo, input, HealthCheckAlert.class);
            case 3:
                return eventAlertSerializer
                        .read(kryo, input, EventAlert.class);
            case 4:
                return periodOverPeriodAlertSerializer
                        .read(kryo, input, PeriodOverPeriodAlert.class);
            default:
                throw new IllegalStateException("Unknown class id: " + classId);
        }
    }
}
