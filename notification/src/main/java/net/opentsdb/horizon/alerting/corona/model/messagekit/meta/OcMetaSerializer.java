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

package net.opentsdb.horizon.alerting.corona.model.messagekit.meta;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;

public class OcMetaSerializer
        extends MetaSerializer<OcMeta, OcMeta.Builder<?>>
{

    /* ------------ Constructor ------------ */

    public OcMetaSerializer()
    {
        super(OcMeta::builder);
    }

    /* ------------ Methods ------------ */

    @Override
    protected Class<OcMeta> getSerializableClass()
    {
        return OcMeta.class;
    }

    @Override
    protected void writeAddedFields(final Kryo kryo,
                                    final Output output,
                                    final OcMeta meta)
    {
        output.writeByte(meta.getOcSeverity().getId());
        output.writeByte(meta.getOcTier().getId());
        output.writeString(meta.getRunbookId());
    }


    @Override
    protected void readAddedFields(final OcMeta.Builder<?> builder,
                                   final Kryo kryo,
                                   final Input input,
                                   final Class<OcMeta> type)
    {
        builder.setOcSeverity(OcSeverity.fromId(input.readByte()))
                .setOcTier(OcTier.fromId(input.readByte()))
                .setRunbookId(input.readString());
    }
}
