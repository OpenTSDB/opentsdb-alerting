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

package net.opentsdb.horizon.alerting.corona.model.alert;

import javax.annotation.concurrent.ThreadSafe;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.opentsdb.horizon.alerting.corona.model.Factory;
import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricSimpleAlertSerializer;

/**
 * @param <A> concrete alert type to be serialized
 * @param <B> builder type for the concrete alert type
 * @see SingleMetricSimpleAlertSerializer for a reference implementation.
 */
@ThreadSafe
public abstract class AlertSerializer<
        A extends Alert,
        B extends Alert.Builder<A, ?>
        >
        extends AbstractSerializer<A>
{

    private final Factory<B> builderFactory;

    public AlertSerializer(final Factory<B> builderFactory)
    {
        this.builderFactory = builderFactory;
    }

    /* ------------ Abstract Methods ------------ */

    /**
     * Subclasses have to serialize added fields.
     * <p>
     * The parent class's fields are serialized before this method is called.
     *
     * @param kryo   Kryo instance
     * @param output output to write to
     * @param alert  alert to serialize
     */
    protected abstract void writeAddedFields(final Kryo kryo,
                                             final Output output,
                                             final A alert);

    /**
     * Subclasses have to read added fields and set them in the [builder].
     * <p>
     * The parent class's fields are set before this method is called.
     *
     * @param builder builder to set parameters to
     * @param kryo    Kryo instance (not used)
     * @param input   input to readAddedFields from
     * @param type    type of the object being readAddedFields
     */
    protected abstract void readAddedFields(final B builder,
                                            final Kryo kryo,
                                            final Input input,
                                            final Class<A> type);

    /* ------------ Methods ------------ */

    @Override
    public final void write(final Kryo kryo, final Output output, final A alert)
    {
        output.writeLong(alert.getTimestampSec());
        output.writeString(alert.getNamespace());
        output.writeString(alert.getDetails());
        output.writeByte(alert.getStateFrom().getId());
        output.writeByte(alert.getState().getId());
        output.writeBoolean(alert.isNag());
        output.writeBoolean(alert.isSnoozed());
        output.writeLong(alert.getId());
        writeStringMap(output, alert.getTags());
        writeStringMap(output, alert.getProperties());
        writeAddedFields(kryo, output, alert);
    }

    @Override
    public final A read(final Kryo kryo, final Input input, final Class<A> type)
    {
        final B builder = builderFactory.create();
        builder.setTimestampSec(input.readLong())
                .setNamespace(input.readString())
                .setDetails(input.readString())
                .setStateFrom(State.valueFrom(input.readByte()))
                .setState(State.valueFrom(input.readByte()))
                .setIsNag(input.readBoolean())
                .setIsSnoozed(input.readBoolean())
                .setId(input.readLong())
                .setTags(readStringMap(input))
                .setProperties(readStringMap(input));
        readAddedFields(builder, kryo, input, type);
        return builder.build();
    }
}
