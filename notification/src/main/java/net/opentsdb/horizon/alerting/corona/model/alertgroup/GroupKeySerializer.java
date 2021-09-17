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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;

public class GroupKeySerializer
        extends AbstractSerializer<GroupKey>
{

    @Override
    protected Class<GroupKey> getSerializableClass()
    {
        return GroupKey.class;
    }

    @Override
    public void write(final Kryo kryo,
                      final Output output,
                      final GroupKey groupKey)
    {
        output.writeString(groupKey.getNamespace());
        output.writeLong(groupKey.getAlertId());
        output.writeByte(groupKey.getAlertType().getId());
        writeArray(output, groupKey.getKeys(), Output::writeString);
        writeArray(output, groupKey.getValues(), Output::writeString);
    }

    @Override
    public GroupKey read(final Kryo kryo,
                         final Input input,
                         final Class<GroupKey> aClass)
    {
        if (aClass != GroupKey.class) {
            throw new IllegalArgumentException("Unexpected class: " + aClass);
        }

        return GroupKey.builder()
                .setNamespace(input.readString())
                .setAlertId(input.readLong())
                .setAlertType(AlertType.valueFrom(input.readByte()))
                .setKeys(readStringArray(input))
                .setValues(readStringArray(input))
                .build();
    }
}
