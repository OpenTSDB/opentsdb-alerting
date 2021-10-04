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

import java.util.Objects;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.Factory;

/**
 * @param <M> concrete metadata type
 */
public abstract class MetaSerializer<M extends Meta, B extends Meta.Builder<M, ?>>
        extends AbstractSerializer<M>
{

    /* ------------ Fields ------------ */

    private final Factory<B> builderFactory;

    /* ------------ Constructor ------------ */

    public MetaSerializer(final Factory<B> builderFactory)
    {
        Objects.requireNonNull(builderFactory,
                "builderFactory cannot be null");
        this.builderFactory = builderFactory;
    }

    /* ------------ Methods ------------ */


    /**
     * Subclasses have to serialize added fields over the base class.
     *
     * @param kryo   Kryo instance
     * @param output output to write to
     * @param meta   meta to serialize
     */
    protected abstract void writeAddedFields(final Kryo kryo,
                                             final Output output,
                                             final M meta);

    @Override
    public final void write(final Kryo kryo, final Output output, final M meta)
    {
        output.writeString(meta.getSubject());
        output.writeString(meta.getBody());
        writeArray(output, meta.getLabels(),Output::writeString);
        writeAddedFields(kryo, output, meta);
    }

    /**
     * Subclasses have to use this method in their implementation of
     * {@link #read(Kryo, Input, Class)}.
     *
     * @param builder builder to set parameters to
     * @param kryo    Kryo instance (not used)
     * @param input   input to readAddedFields from
     * @param type    type of the object being readAddedFields
     */
    protected abstract void readAddedFields(final B builder,
                                            final Kryo kryo,
                                            final Input input,
                                            final Class<M> type);

    @Override
    public final M read(final Kryo kryo, final Input input, final Class<M> type)
    {
        final B builder = builderFactory.create();
        builder.setSubject(input.readString())
                .setBody(input.readString())
                .setLabels(readStringArray(input));
        readAddedFields(builder, kryo, input, type);
        return builder.build();
    }
}
