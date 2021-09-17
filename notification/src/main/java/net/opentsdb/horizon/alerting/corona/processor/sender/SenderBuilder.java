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

package net.opentsdb.horizon.alerting.corona.processor.sender;

import java.util.Objects;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.messagekit.PrePackedMessageKit;
import net.opentsdb.horizon.alerting.corona.processor.Processor;


// TODO: Reconsider this design.
public class SenderBuilder {

    public static SenderBuilder create()
    {
        return new SenderBuilder();
    }

    private Processor<byte[]> sink;

    private AbstractSerializer<PrePackedMessageKit> serializer;

    SenderBuilder() { }

    public SenderBuilder setSink(final Processor<byte[]> sink)
    {
        this.sink = sink;
        return this;
    }

    public SenderBuilder setSerializer(
            final AbstractSerializer<PrePackedMessageKit> serializer)
    {
        this.serializer = serializer;
        return this;
    }

    public Processor<PrePackedMessageKit> build()
    {
        Objects.requireNonNull(sink, "sink cannot be null");
        Objects.requireNonNull(serializer, "serializer cannot be null");

        return message -> {
            final byte[] bytes = serializer.toBytes(message);
            sink.process(bytes);
        };
    }
}
