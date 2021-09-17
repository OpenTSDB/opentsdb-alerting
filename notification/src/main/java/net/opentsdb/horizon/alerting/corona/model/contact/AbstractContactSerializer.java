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

package net.opentsdb.horizon.alerting.corona.model.contact;

import java.util.Objects;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.Factory;

public abstract class AbstractContactSerializer<
        C extends Contact,
        B extends AbstractContact.Builder<C, ?>
        >
        extends AbstractSerializer<C>
{

    /* ------------ Fields ------------ */

    private final Factory<B> builderFactory;

    /* ------------ Constructor ------------ */

    public AbstractContactSerializer(final Factory<B> builderFactory)
    {
        Objects.requireNonNull(builderFactory, "builderFactory cannot be null");
        this.builderFactory = builderFactory;
    }

    /* ------------ Methods ------------ */

    /**
     * Subclasses have to serialize added fields over the base class.
     *
     * @param kryo    Kryo instance
     * @param output  output to write to
     * @param contact contact to serialize
     */
    protected abstract void writeAddedFields(final Kryo kryo,
                                             final Output output,
                                             final C contact);

    @Override
    public final void write(final Kryo kryo,
                            final Output output,
                            final C contact)
    {
        // Do not serialize the type, as it comes from the builder.

        output.writeString(contact.getName());
        writeAddedFields(kryo, output, contact);
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
                                            final Class<C> type);

    @Override
    public final C read(final Kryo kryo, final Input input, final Class<C> type)
    {
        final B builder = builderFactory.create();
        builder.setName(input.readString());
        readAddedFields(builder, kryo, input, type);
        return builder.build();
    }
}
