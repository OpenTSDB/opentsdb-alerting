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

package net.opentsdb.horizon.alerting.corona.processor.filter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.opentsdb.horizon.alerting.corona.component.Triple;
import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;

public class SnoozedFilter extends ChainableProcessor<
        Triple<AlertGroup, Metadata, Contacts>,
        Triple<AlertGroup, Metadata, Contacts>
        >
{

    /* ------------ Constructors ------------ */

    public SnoozedFilter(
            final Processor<Triple<AlertGroup, Metadata, Contacts>> next)
    {
        super(next);
    }

    /* ------------ Methods ------------ */

    private void reportSnoozedCount(final int count, final String namespace)
    {
        AppMonitor.get().countAlertSnoozed(count, namespace);
    }

    private List<Alert> getActiveAlerts(final List<Alert> allAlerts)
    {
        return allAlerts.stream()
                .filter(alert -> !alert.isSnoozed())
                .collect(Collectors.toList());
    }

    private Optional<Triple<AlertGroup, Metadata, Contacts>> process(
            final AlertGroup alertGroup,
            final Metadata metadata,
            final Contacts contacts)
    {
        final List<Alert> allAlerts = alertGroup.getAlerts();
        final List<Alert> activeAlerts = getActiveAlerts(allAlerts);

        if (activeAlerts.isEmpty()) {
            return Optional.empty();
        }

        reportSnoozedCount(
                allAlerts.size() - activeAlerts.size(),
                alertGroup.getGroupKey().getNamespace()
        );

        final AlertGroup newAlertGroup = AlertGroup.builder()
                .setGroupKey(alertGroup.getGroupKey())
                .setAlerts(activeAlerts)
                .build();

        return Optional.of(new Triple<>(newAlertGroup, metadata, contacts));
    }

    @Override
    public void process(final Triple<AlertGroup, Metadata, Contacts> triple)
    {
        process(triple.getFirst(), triple.getSecond(), triple.getLast())
                .ifPresent(this::submit);
    }
}
