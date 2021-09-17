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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class OpsGenieMetaSerializer
        extends MetaSerializer<OpsGenieMeta, OpsGenieMeta.Builder<?>>
{

    /* ------------ Constructor ------------ */

    public OpsGenieMetaSerializer()
    {
        super(OpsGenieMeta::builder);
    }

    /* ------------ Methods ------------ */

    @Override
    protected Class<OpsGenieMeta> getSerializableClass()
    {
        return OpsGenieMeta.class;
    }


    @Override
    protected void writeAddedFields(final Kryo kryo,
                                    final Output output,
                                    final OpsGenieMeta meta)
    {
        output.writeString(meta.getSubject());
        output.writeString(meta.getBody());
        output.writeString(meta.getOpsGeniePriority());
        output.writeBoolean(meta.isOpsGenieAutoClose());
        final List<String> opsGenieTags = meta.getOpsGenieTags();
        output.writeInt(opsGenieTags.size());
        for (String tag: opsGenieTags) {
            output.writeString(tag);
        }
    }

    @Override
    protected void readAddedFields(final OpsGenieMeta.Builder<?> builder,
                                   final Kryo kryo,
                                   final Input input,
                                   final Class<OpsGenieMeta> type)
    {
        builder.setSubject(input.readString())
                .setBody(input.readString())
                .setOpsGeniePriority(input.readString())
                .setOpsGenieAutoClose(input.readBoolean());
        final int opsGenieTagsSize = input.readInt();
        final List<String> opsGenieTags;
        if (opsGenieTagsSize == 0) {
            opsGenieTags = Collections.emptyList();
        } else {
            opsGenieTags = new ArrayList<>(opsGenieTagsSize);
            for (int i = 0; i < opsGenieTagsSize; ++i) {
                opsGenieTags.add(input.readString());
            }
        }
        builder.setOpsGenieTags(opsGenieTags);
    }
}
