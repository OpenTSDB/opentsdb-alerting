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

package net.opentsdb.horizon.alerting.corona.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import kafka.serializer.Decoder;
import kafka.serializer.Encoder;

public abstract class AbstractSerializer<T>
        extends com.esotericsoftware.kryo.Serializer<T>
        implements Decoder<T>, Encoder<T>
{

    /* ------------ Static Methods ------------ */

    protected static void writeStringMap(final Output output,
                                         final Map<String, String> map)
    {
        if (map == null || map.isEmpty()) {
            output.writeShort(0);
        } else {
            output.writeShort(map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                output.writeString(entry.getKey());
                output.writeString(entry.getValue());
            }
        }
    }

    protected static Map<String, String> readStringMap(final Input input)
    {
        int size = input.readShort();
        Map<String, String> map;

        if (size == 0) {
            map = Collections.emptyMap();
        } else {
            map = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                map.put(input.readString(), input.readString());
            }
            map = Collections.unmodifiableMap(map);
        }

        return map;
    }

    protected static <I> void writeArray(
            final Output output,
            final I[] values,
            final BiConsumer<Output, I> valueWriter)
    {
        if (values == null) {
            output.writeInt(0);
            return;
        }

        output.writeInt(values.length);
        for (I value : values) {
            valueWriter.accept(output, value);
        }
    }

    protected static String[] readStringArray(final Input input)
    {
        final int size = input.readInt();
        final String[] result = new String[size];

        for (int i = 0; i < size; i++) {
            result[i] = input.readString();
        }

        return result;
    }

    protected static void writeDoubleArray(
            final Output output,
            final double[] values)
    {
        if (values == null) {
            output.writeInt(0);
            return;
        }

        output.writeInt(values.length);
        for (Double value : values) {
            output.writeDouble(value);
        }
    }

    protected static double[] readDoubleArray(final Input input)
    {
        final int size = input.readInt();
        final double[] result = new double[size];

        for (int i = 0; i < size; i++) {
            result[i] = input.readDouble();
        }

        return result;
    }

    protected static <I> void writeCollection(
            final Kryo kryo,
            final Output output,
            final Collection<I> values,
            final TriConsumer<Kryo, Output, I> valueWriter)
    {
        if (values == null) {
            output.writeInt(0);
            return;
        }

        output.writeInt(values.size());
        for (I value : values) {
            valueWriter.accept(kryo, output, value);
        }
    }

    protected static <I> List<I> readList(
            final Kryo kryo,
            final Input input,
            final Class<I> clazz,
            final TriFunction<Kryo, Input, Class<I>, I> valueReader)
    {
        final int size = input.readInt();
        if (size == 0) {
            return Collections.emptyList();
        }

        final List<I> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(valueReader.apply(kryo, input, clazz));
        }

        return Collections.unmodifiableList(values);
    }

    /* ------------ Abstract Methods ------------ */

    protected abstract Class<T> getSerializableClass();

    /* ------------ Methods ------------ */

    @Override
    public T fromBytes(byte[] bytes)
    {
        try (Input input = new Input(bytes)) {
            return read(null, input, getSerializableClass());
        }
    }

    @Override
    public byte[] toBytes(T t)
    {
        try (Output output = new Output(1024, -1)) {
            write(null, output, t);
            return output.toBytes();
        }
    }
}
