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

package net.opentsdb.horizon.alerting.corona.processor.serializer;

import java.util.Objects;

import javax.annotation.concurrent.ThreadSafe;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.messagekit.PrePackedMessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.PrePackedMessageKitSerializer;
import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;

@ThreadSafe
public class PreSendSerializer
        extends ChainableProcessor<PrePackedMessageKit, byte[]>
{

    public static PreSendSerializer create(
            final Processor<byte[]> next,
            final AbstractSerializer<PrePackedMessageKit> serializer)
    {
        return new PreSendSerializer(next, serializer);
    }

    public static PreSendSerializer create(final Processor<byte[]> next)
    {
        return create(next, PrePackedMessageKitSerializer.instance());
    }

    final AbstractSerializer<PrePackedMessageKit> serializer;

    PreSendSerializer(final Processor<byte[]> next,
                      final AbstractSerializer<PrePackedMessageKit> serializer)
    {
        super(next);
        Objects.requireNonNull(serializer, "serializer cannot be null");
        this.serializer = serializer;
    }

    @Override
    public void process(PrePackedMessageKit prePackedMessageKit)
    {
        // TODO: Add exception handling.
        final byte[] bytes = serializer.toBytes(prePackedMessageKit);
        submit(bytes);
    }
}
