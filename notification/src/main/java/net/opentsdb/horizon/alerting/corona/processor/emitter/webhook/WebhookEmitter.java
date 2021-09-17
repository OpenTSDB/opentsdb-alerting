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

package net.opentsdb.horizon.alerting.corona.processor.emitter.webhook;

import net.opentsdb.horizon.alerting.core.validate.Validate;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.WebhookContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.WebhookMeta;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WebhookEmitter implements Processor<MessageKit> {
    private static final Logger LOG = LoggerFactory.getLogger(WebhookEmitter.class);

    private WebhookClient client;
    private WebhookFormatter formatter;

    private WebhookEmitter(final DefaultBuilder builder)
    {
        Validate.isTrue(builder != null, "Builder cannot be null.");
        Validate.isTrue(builder.client != null, "Client has to be set.");
        Validate.isTrue(builder.formatter != null, "Formatter has to be set.");
        this.client = builder.client;
        this.formatter = builder.formatter;
    }

    @Override
    public void process(MessageKit messageKit)
    {
        if (messageKit.getType() != Contact.Type.WEBHOOK) {
            LOG.error("Unexpected MessageKit type: {}", messageKit);
            return;
        }

        final String namespace = messageKit.getNamespace();
        final WebhookMeta meta = (WebhookMeta) messageKit.getMeta();
        final MessageKitView messageKitView = Views.of(messageKit);
        @SuppressWarnings("unchecked") final List<AlertView> alertViews = messageKitView.getAllViews();
        @SuppressWarnings("unchecked") final List<WebhookContact> contacts =
                (List<WebhookContact>) messageKit.getContacts();

        final List<WebhookEvent> events = new ArrayList<>();
        for (AlertView alertView : alertViews) {
            try {
                events.add(formatter.format(messageKitView, alertView, meta));
            } catch (Exception e) {
                AppMonitor.get().countAlertFormatFailed(namespace);
                LOG.error("Failed to format: alert_id={}, ns={}, view={}, meta={}",
                        messageKitView.getAlertId(), namespace, alertViews, meta);
                return;
            }
        }

        for (final WebhookContact contact : contacts) {
            doProcess(namespace, messageKitView.getAlertId(), events, contact);
        }
    }

    private void doProcess(final String namespace,
                           final long alertId,
                           final List<WebhookEvent> events,
                           final WebhookContact contact)
    {
        try {
            client.send(events, contact.getEndpoint());
            AppMonitor.get().countAlertWebhookSendSuccess(namespace, alertId, contact.getName());
        } catch (Exception e) {
            AppMonitor.get().countAlertWebhookSendFailed(namespace, alertId, contact.getName());
            LOG.error("Send failed:", e);
        }
    }

    public interface Builder extends net.opentsdb.horizon.alerting.Builder<Builder, WebhookEmitter> {
        Builder setClient(WebhookClient client);
        Builder setFormatter(WebhookFormatter formatter);
    }

    private static final class DefaultBuilder implements Builder {
        private WebhookClient client;
        private WebhookFormatter formatter;

        @Override
        public Builder setClient(WebhookClient client)
        {
            this.client = client;
            return this;
        }

        @Override
        public Builder setFormatter(WebhookFormatter formatter)
        {
            this.formatter = formatter;
            return this;
        }

        @Override
        public WebhookEmitter build()
        {
            return new WebhookEmitter(this);
        }
    }

    public static Builder builder() {  return new DefaultBuilder(); }
}
