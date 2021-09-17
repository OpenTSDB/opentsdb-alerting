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

package net.opentsdb.horizon.alerting.corona.processor.dispatcher;

import java.util.Objects;
import java.util.Optional;

import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.component.Triple;
import net.opentsdb.horizon.alerting.corona.config.MetadataProvider;
import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;

public class MetadataAppender
        extends ChainableProcessor<
        AlertGroup,
        Triple<AlertGroup, Metadata, Contacts>
        >
{

    private static final Logger LOG =
            LoggerFactory.getLogger(MetadataAppender.class);

    /* ------------ Fields ------------ */

    private final MetadataProvider metadataProvider;

    /* ------------ Constants ------------ */

    MetadataAppender(final Builder<?> builder)
    {
        super(builder);
        Objects.requireNonNull(builder.metadataProvider,
                "metadataProvider cannot be null");
        this.metadataProvider = builder.metadataProvider;
    }

    /* ------------ Methods ------------ */


    private void submit(final AlertGroup alertGroup,
                        final Metadata metadata,
                        final Contacts contacts)
    {
        submit(new Triple<>(alertGroup, metadata, contacts));
    }

    private void handleStranded(final long alert_id, final AlertGroup alertGroup)
    {
        AppMonitor.get().countAddresseeAppenderStranded(alert_id);
        LOG.error("Stranded alert group = {}", alertGroup);
    }

    @Override
    public void process(final AlertGroup alertGroup)
    {
        final long alertId = alertGroup.getGroupKey().getAlertId();
        LOG.trace("Appending metadata to: alert_id={}, alert_group={}", alertId, alertGroup);

        final Optional<Metadata> metadata =
                metadataProvider.getMetadata(alertId);
        final Optional<Contacts> contacts =
                metadataProvider.getContacts(alertId);

        if (contacts.isPresent() && metadata.isPresent()) {
            submit(alertGroup, metadata.get(), contacts.get());
        } else {
            handleStranded(alertId, alertGroup);
        }
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends ChainableProcessor.Builder<
            Triple<AlertGroup, Metadata, Contacts>,
            B
            >
    {

        private MetadataProvider metadataProvider;

        public B setMetadataProvider(final MetadataProvider metadataProvider)
        {
            this.metadataProvider = metadataProvider;
            return self();
        }

        public MetadataAppender build()
        {
            return new MetadataAppender(this);
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
