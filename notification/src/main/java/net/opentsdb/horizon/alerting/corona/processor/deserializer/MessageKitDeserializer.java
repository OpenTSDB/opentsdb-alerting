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

package net.opentsdb.horizon.alerting.corona.processor.deserializer;

import java.util.Objects;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKitSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;

public class MessageKitDeserializer
        extends ChainableProcessor<byte[], MessageKit>
{

    // ------------ Constants ------------ //

    private static final Logger LOG =
            LoggerFactory.getLogger(MessageKitDeserializer.class);

    // ------------ Static Methods ------------ //

    public static MessageKitDeserializer create(
            final Processor<MessageKit> next,
            final AbstractSerializer<MessageKit> serializer)
    {
        return new MessageKitDeserializer(next, serializer);
    }

    public static MessageKitDeserializer create(final Processor<MessageKit> next)
    {
        return MessageKitDeserializer.create(next, MessageKitSerializer.instance());
    }

    // ------------ Fields ------------ //

    private final AbstractSerializer<MessageKit> serializer;


    // ------------ Constructor ------------ //

    MessageKitDeserializer(final Processor<MessageKit> next,
                           final AbstractSerializer<MessageKit> serializer)
    {
        super(next);
        Objects.requireNonNull(serializer, "serializer cannot be null");
        this.serializer = serializer;
    }

    // ------------ Methods ------------ //

    @Override
    public void process(final byte[] serialized)
    {
        final MessageKit messageKit;
        try {
            messageKit = serializer.fromBytes(serialized);
        } catch (Exception e) {
            LOG.error("Failed to deserialize a messageKit", e);
            return;
        }
        submit(messageKit);
    }
}
