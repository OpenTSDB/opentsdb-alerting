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

package net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.formatter;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.WebhookMeta;
import net.opentsdb.horizon.alerting.corona.processor.emitter.oc.AlertHasher;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.WebhookEvent;

import java.util.Objects;
import java.util.SortedMap;

public abstract class WebhookAbstractAlertFacade<
        A extends Alert,
        V extends AlertView,
        M extends MessageKitView<A, V>>
        implements WebhookEvent
{
    protected M messageKitView;
    protected V alertView;
    protected final WebhookMeta meta;

    public WebhookAbstractAlertFacade(final M messageKitView,
                                      final V alertView,
                                      final WebhookMeta meta)
    {
        Objects.requireNonNull(alertView, "alertView cannot be null");
        Objects.requireNonNull(meta, "meta cannot be null");
        this.messageKitView = messageKitView;
        this.alertView = alertView;
        this.meta = meta;
    }

    @Override
    public long getAlertId() { return messageKitView.getAlertId(); }

    @Override
    public String getBody()
    {
        return messageKitView.interpolateBody(alertView);
    }

    @Override
    public String getDescription() {
        return alertView.getDescription();
    }

    @Override
    public boolean isNag()
    {
        return alertView.isNag();
    }

    @Override
    public String getNamespace() {
        return alertView.getNamespace();
    }

    @Override
    public boolean isSnoozed() {
        return alertView.isSnoozed();
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

    @Override
    public String getSubject()
    {
        return messageKitView.interpolateSubject(alertView);
    }

    @Override
    public SortedMap<String, String> getTags()
    {
        return alertView.getSortedTags();
    }

    @Override
    public long getAlertTimeSec()
    {
        return alertView.getTimestampMs() / 1000L;
    }

    @Override
    public String getUrl()
    {
        return Views.alertViewUrl(messageKitView.getAlertId());
    }

    @Override
    public String getSignature() {
        return AlertHasher.hash(messageKitView, alertView);
    }

    @Override
    public boolean isRecovery() {
        return alertView.isRecovery();
    }
}
