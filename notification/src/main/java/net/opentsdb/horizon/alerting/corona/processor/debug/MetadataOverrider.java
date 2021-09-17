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

package net.opentsdb.horizon.alerting.corona.processor.debug;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.EmailContact;
import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.config.MetadataProvider;

public class MetadataOverrider implements MetadataProvider {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(MetadataOverrider.class);

    /* ------------ Fields ------------ */

    private final MetadataProvider original;

    /* ------------ Constructors ------------ */

    public MetadataOverrider(final MetadataProvider original)
    {
        Objects.requireNonNull(original, "original cannot be null");
        this.original = original;
    }

    /* ------------ Methods ------------ */

    @Override
    public Optional<List<String>> getGroupingRules(final long alertId)
    {
        if (alertId == 963L) {
            LOG.debug("Overriding grouping rules.");
            return Optional.of(Collections.emptyList());
        }
        return original.getGroupingRules(alertId);
    }

    @Override
    public Optional<Contacts> getContacts(final long alertId)
    {
        if (alertId == 963L) {
            LOG.debug("Overriding contacts.");
            return Optional.of(Contacts.builder()
                    .setEmailContacts(
                            EmailContact.builder()
                                    .setName("foo@opentsdb.net")
                                    .setEmail("foo@opentsdb.net")
                                    .build()
                    )
                    .build()
            );
        }

        return original.getContacts(alertId);
    }

    @Override
    public Optional<Metadata> getMetadata(final long alertId)
    {
        if (alertId == 963L) {
            LOG.debug("Overriding metadata");
            return Optional.of(
                    Metadata.builder()
                            .setSubject("Status alert demo")
                            .setBody("Status alert demo")
                            .setLabels("status")
                            .build()
            );
        }
        return original.getMetadata(alertId);
    }
}
