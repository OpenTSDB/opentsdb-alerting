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

package net.opentsdb.horizon.alerting.corona.processor.emitter.prism.formatter;

import java.util.Objects;
import java.util.SortedMap;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OcMeta;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;
import net.opentsdb.horizon.alerting.corona.processor.emitter.oc.AlertHasher;
import net.opentsdb.horizon.alerting.corona.processor.emitter.prism.PrismEvent;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;

public abstract class PrismAbstractAlertFacade<
        A extends Alert,
        V extends AlertView,
        M extends MessageKitView<A, V>
        >
        implements PrismEvent {

    private static final OcSeverity DEFAULT_SEVERITY = OcSeverity.SEV_3;
    private static final OcTier DEFAULT_TIER = OcTier.TIER_1;

    private final String hostname;
    protected final M messageKitView;
    protected final V alertView;
    protected final OcMeta meta;
    protected final OcContact contact;

    public PrismAbstractAlertFacade(final String hostname,
                                    final M messageKitView,
                                    final V alertView,
                                    final OcMeta meta,
                                    final OcContact contact) {
        Objects.requireNonNull(hostname, "hostname cannot be null");
        Objects.requireNonNull(messageKitView, "messageKitView cannot be null");
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
    public String getSignature() {
        return AlertHasher.hash(messageKitView, alertView) + ":prism";
    }

    @Override
    public String getSource() {
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

    abstract protected String getAlertSpecificSource();

    @Override
    public int getSeverity() {
        if (alertView.isRecovery()) {
            return 0;
        }

        OcSeverity severity = meta.getOcSeverity();
        if (severity == OcSeverity.NOT_SET) {
            severity = DEFAULT_SEVERITY;
        }

        return severity.getId();
    }

    @Override
    public String getOpsDbProperty() {
        return contact.getOpsdbProperty();
    }

    @Override
    public long getAlertTimeSec() {
        return alertView.getTimestampMs() / 1000L;
    }

    @Override
    public String getNamespace() {
        return alertView.getNamespace();
    }

    @Override
    public String getAgent() {
        return "corona-notification";
    }

    @Override
    public String getAgentLocation() {
        return hostname;
    }

    @Override
    public String getRunbookId() {
        return meta.getRunbookId();
    }

    @Override
    public boolean isProduction() {
        return "live".equalsIgnoreCase(contact.getContext());
    }

    @Override
    public int getEscalationTier() {
        OcTier tier = meta.getOcTier();
        if (tier == OcTier.NOT_SET) {
            tier = DEFAULT_TIER;
        }
        return tier.getId();
    }

    @Override
    public String getDashboardUrl() {
        return Views.alertViewUrl(messageKitView.getAlertId());
    }
}
