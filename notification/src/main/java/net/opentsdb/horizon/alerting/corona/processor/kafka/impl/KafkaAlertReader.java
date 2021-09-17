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

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.processor.kafka.AbstractReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade abstraction.
 *
 * TODO: Add better description or remove the class.
 */
public class KafkaAlertReader extends AbstractReader<Alert> {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(KafkaAlertReader.class);

    /* ------------ Constructor ------------ */

    KafkaAlertReader(final Builder<?> builder)
    {
        super(builder);
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends AbstractReader.Builder<Alert, B>
    {

        /**
         * Build the {@link KafkaAlertReader}.
         * <p>
         * Default logger is used if not set by the user.
         *
         * @return {@link KafkaAlertReader} Kafka Stream alert reader
         */
        public KafkaAlertReader build()
        {
            if (!hasLogger()) {
                setLogger(LOG);
            }
            return new KafkaAlertReader(this);
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
