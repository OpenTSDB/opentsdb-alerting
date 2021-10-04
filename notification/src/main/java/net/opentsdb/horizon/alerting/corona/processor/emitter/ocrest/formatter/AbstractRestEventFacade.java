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

package net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.formatter;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;

import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OcMeta;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;
import net.opentsdb.horizon.alerting.corona.processor.emitter.oc.AlertHasher;
import net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.OcRestEvent;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;

import java.util.Objects;
import java.util.SortedMap;

public abstract class AbstractRestEventFacade<
        A extends Alert,
        V extends AlertView,
        M extends MessageKitView<A, V>>
        implements OcRestEvent
{

    private static final OcSeverity DEFAULT_SEVERITY = OcSeverity.SEV_3;

    private static final OcTier DEFAULT_TIER = OcTier.TIER_1;

    private final String hostname;

    protected M messageKitView;

    protected V alertView;

    protected OcMeta meta;

    protected OcContact contact;

    public AbstractRestEventFacade(final String hostname,
                                   final M messageKitView,
                                   final V alertView,
                                   final OcMeta meta,
                                   final OcContact contact)
    {
        Objects.requireNonNull(hostname, "hostname cannot be null");
        Objects.requireNonNull(messageKitView, "hostname cannot be null");
        Objects.requireNonNull(alertView, "alertView cannot be null");
        Objects.requireNonNull(meta, "meta cannot be null");
        Objects.requireNonNull(contact, "contact cannot be null");
        this.hostname = hostname;
        this.messageKitView = messageKitView;
        this.alertView = alertView;
        this.meta = meta;
        this.contact = contact;
    }

    @Override
    public String getAlertHash()
    {
        return AlertHasher.hash(messageKitView, alertView);
    }

    @Override
    public String getOpsDbProperty()
    {
        return contact.getOpsdbProperty();
    }

    @Override
    public String getNamespace()
    {
        return alertView.getNamespace();
    }

    @Override
    public String getHostname()
    {
        return hostname;
    }

    @Override
    public String getSource()
    {
        final SortedMap<String, String> tags = alertView.getSortedTags();
        if (tags.containsKey("host")) {
            return tags.get("host");
        } else if (tags.containsKey("hostgroup")) {
            return tags.get("hostgroup");
        } else if (tags.containsKey("InstanceId")) {
            return tags.get("InstanceId");
        }

        return getAlertSpecificSource();
    }

    /**
     * Return alert-specific source.
     *
     * @return alert specific source or empty string if none.
     */
    protected abstract String getAlertSpecificSource();

    @Override
    public int getSeverity()
    {
        if (alertView.isRecovery()) {
            return 0;
        }

        OcSeverity severity = meta.getOcSeverity();
        if (severity == OcSeverity.NOT_SET) {
            severity = DEFAULT_SEVERITY;
        }

        OcTier tier = meta.getOcTier();
        if (tier == OcTier.NOT_SET) {
            tier = DEFAULT_TIER;
        }

        if (tier == OcTier.TIER_1) {
            return severity.getId();
        }

        return severity.getId() * 10 + tier.getId();
    }

    @Override
    public long getAlertTimeSec()
    {
        return alertView.getTimestampMs() / 1000L;
    }

    @Override
    public String getRunbookId()
    {
        return meta.getRunbookId();
    }

    @Override
    public boolean isRecovery()
    {
        return alertView.isRecovery();
    }

    @Override
    public String getDashboardUrl()
    {
        return Views.alertViewUrl(messageKitView.getAlertId());
    }

    @Override
    public boolean isNag()
    {
        return alertView.isNag();
    }

    @Override
    public String getSubject()
    {
        return messageKitView.interpolateSubject(alertView);
    }

    @Override
    public String getBody()
    {
        return messageKitView.interpolateBody(alertView);
    }

    @Override
    public long getAlertId()
    {
        return messageKitView.getAlertId();
    }

    @Override
    public SortedMap<String, String> getTags()
    {
        return alertView.getSortedTags();
    }

    @Override
    public String getStateFrom()
    {
        return alertView.getStateFrom();
    }

    @Override
    public String getStateTo()
    {
        return alertView.getStateTo();
    }
}
