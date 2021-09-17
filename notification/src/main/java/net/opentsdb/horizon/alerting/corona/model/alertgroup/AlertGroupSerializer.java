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

package net.opentsdb.horizon.alerting.corona.model.alertgroup;

import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.GenericAlertSerializer;

public class AlertGroupSerializer extends AbstractSerializer<AlertGroup> {

    private static final GroupKeySerializer groupKeySerializer =
            new GroupKeySerializer();

    private static final AbstractSerializer<Alert> alertSerializer =
            new GenericAlertSerializer();

    @Override
    protected Class<AlertGroup> getSerializableClass()
    {
        return AlertGroup.class;
    }

    @Override
    public void write(final Kryo kryo,
                      final Output output,
                      final AlertGroup alertGroup)
    {
        groupKeySerializer.write(kryo, output, alertGroup.getGroupKey());
        writeCollection(kryo, output, alertGroup.getAlerts(),
                alertSerializer::write);
    }

    @Override
    public AlertGroup read(final Kryo kryo,
                           final Input input,
                           final Class<AlertGroup> aClass)
    {
        final GroupKey groupKey =
                groupKeySerializer.read(kryo, input, GroupKey.class);
        final List<Alert> alerts =
                readList(kryo, input, Alert.class, alertSerializer::read);
        return new AlertGroup(groupKey, alerts);
    }
}
