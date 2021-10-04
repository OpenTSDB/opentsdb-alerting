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

package net.opentsdb.horizon.alerting.corona.processor.kafka.impl;

import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.kafka.AbstractReader;

/**
 * Facade abstraction.
 *
 * TODO: Add better description or remove the class.
 */
public class KafkaMessageKitReader extends AbstractReader<MessageKit> {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(KafkaMessageKitReader.class);

    /* ------------ Constructor ------------ */

    KafkaMessageKitReader(final Builder<?> builder)
    {
        super(builder);
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends AbstractReader.Builder<MessageKit, B>
    {
        /**
         * Build the {@link KafkaMessageKitReader}.
         *
         * Default logger is used if not set by the user.
         *
         * @return {@link KafkaMessageKitReader} MessageKit stream reader
         */
        public KafkaMessageKitReader build()
        {
            if (!hasLogger()) {
                setLogger(LOG);
            }
            return new KafkaMessageKitReader(this);
        }
    }

    private static class BuilderImpl extends Builder<BuilderImpl> {
        @Override
        protected BuilderImpl self()
        {
            return this;
        }
    }

    public static Builder<?> builder()
    {
        return new BuilderImpl();
    }
}
