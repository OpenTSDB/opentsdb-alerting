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

package net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest;

import net.opentsdb.horizon.alerting.core.validate.Validate;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OcMeta;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class OcRestEmitter implements Processor<MessageKit> {

    private static final Logger LOG =
            LoggerFactory.getLogger(OcRestEmitter.class);

    private static final String LIVE_CONTEXT = "live";

    private OcRestClient client;

    private OcRestFormatter formatter;

    private OcRestEmitter(final DefaultBuilder builder)
    {
        this.client = buildClient(builder);
        this.formatter = buildFormatter(builder);
    }

    private OcRestClient buildClient(final DefaultBuilder builder)
    {
        Validate.isTrue((builder.client == null && builder.clientBuilder != null) ||
                        (builder.client != null && builder.clientBuilder == null),
                "Either client or clientBuilder has to be set.");
        if (builder.client != null) {
            return builder.client;
        }
        return builder.clientBuilder.build();
    }

    private OcRestFormatter buildFormatter(final DefaultBuilder builder)
    {
        Validate.isTrue((builder.formatter == null && builder.formatterBuilder != null) ||
                        (builder.formatter != null && builder.formatterBuilder == null),
                "Either formatter or formatterBuilder has to be set.");
        if (builder.formatter != null) {
            return builder.formatter;
        }
        return builder.formatterBuilder.build();
    }

    @Override
    public void process(MessageKit messageKit)
    {
        if (messageKit.getType() != Contact.Type.OC) {
            LOG.error("Unexpected MessageKit type: {}", messageKit);
            return;
        }

        final String namespace = messageKit.getNamespace();
        final OcMeta meta = (OcMeta) messageKit.getMeta();
        final MessageKitView messageKitView = Views.of(messageKit);
        @SuppressWarnings("unchecked") final List<AlertView> alertViews = messageKitView.getAllViews();
        @SuppressWarnings("unchecked") final List<OcContact> contacts =
                (List<OcContact>) messageKit.getContacts();

        for (final AlertView alertView : alertViews) {
            for (final OcContact contact : contacts) {
                if (LIVE_CONTEXT.equalsIgnoreCase(contact.getContext())) {
                    doProcess(namespace, messageKitView, alertView, meta, contact);
                }
            }
        }
    }

    private void doProcess(final String namespace,
                           final MessageKitView messageKitView,
                           final AlertView alertView,
                           final OcMeta meta,
                           final OcContact contact)
    {
        final OcRestEvent event;
        try {
            event = formatter.format(messageKitView, alertView, meta, contact);
        } catch (Exception e) {
            AppMonitor.get().countAlertFormatFailed(namespace);
            LOG.error("Failed to format: alert_id={}, ns={}, view={}, meta={}, contact={}",
                    messageKitView.getAlertId(), namespace, alertView, meta, contact);
            return;
        }

        try {
            client.send(event);
            AppMonitor.get().countAlertSendSuccess(namespace);
        } catch (Exception e) {
            AppMonitor.get().countAlertSendFailed(namespace);
            LOG.error("Send failed:", e);
        }
    }

    public interface Builder extends net.opentsdb.horizon.alerting.Builder<Builder, OcRestEmitter> {

        Builder setClient(OcRestClient client);

        Builder setClientBuilder(OcRestClient.Builder clientBuilder);

        Builder setFormatter(OcRestFormatter formatter);

        Builder setFormatterBuilder(OcRestFormatter.Builder formatterBuilder);

    }

    private static final class DefaultBuilder implements Builder {

        private OcRestClient client;
        private OcRestClient.Builder<?> clientBuilder;
        private OcRestFormatter formatter;
        private OcRestFormatter.Builder<?> formatterBuilder;

        @Override
        public Builder setClient(OcRestClient client)
        {
            this.client = client;
            return this;
        }

        @Override
        public Builder setClientBuilder(OcRestClient.Builder clientBuilder)
        {
            this.clientBuilder = clientBuilder;
            return this;
        }

        @Override
        public Builder setFormatter(OcRestFormatter formatter)
        {
            this.formatter = formatter;
            return this;
        }

        @Override
        public Builder setFormatterBuilder(OcRestFormatter.Builder formatterBuilder)
        {
            this.formatterBuilder = formatterBuilder;
            return this;
        }

        @Override
        public OcRestEmitter build()
        {
            return new OcRestEmitter(this);
        }
    }

    public static Builder builder()
    {
        return new DefaultBuilder();
    }
}
