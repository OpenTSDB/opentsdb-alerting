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

package net.opentsdb.horizon.alerting.corona.processor.emitter.prism;

import java.util.List;

import net.opentsdb.horizon.alerting.core.validate.Validate;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OcMeta;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.Processor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PrismEmitter implements Processor<MessageKit> {

    private static final Logger LOG = LoggerFactory.getLogger(PrismEmitter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_PAYLOAD_SIZE_BYTES = 512 * 1024;

    private final PrismClient client;
    private final PrismFormatter formatter;
    private final int maxPayloadSizeBytes;

    private PrismEmitter(final DefaultBuilder builder) {
        this.client = buildClient(builder);
        this.formatter = buildFormatter(builder);
        this.maxPayloadSizeBytes = builder.maxPayloadSizeBytes > 0
                ? builder.maxPayloadSizeBytes : MAX_PAYLOAD_SIZE_BYTES;
    }

    private PrismClient buildClient(final DefaultBuilder builder) {
        Validate.isTrue((builder.client == null && builder.clientBuilder != null) ||
                        (builder.client != null && builder.clientBuilder == null),
                "Either client or clientBuilder has to be set.");
        if (builder.client != null) {
            return builder.client;
        }
        return builder.clientBuilder.build();
    }

    private PrismFormatter buildFormatter(final DefaultBuilder builder) {
        Validate.isTrue((builder.formatter == null && builder.formatterBuilder != null) ||
                        (builder.formatter != null && builder.formatterBuilder == null),
                "Either formatter or formatterBuilder has to be set.");
        if (builder.formatter != null) {
            return builder.formatter;
        }
        return builder.formatterBuilder.build();
    }

    @Override
    public void process(MessageKit messageKit) {
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

        final StringBuilder buf = new StringBuilder(MAX_PAYLOAD_SIZE_BYTES);
        int eventCount = 0;
        for (final AlertView alertView : alertViews) {
            for (final OcContact contact : contacts) {
                final String data;
                try {
                    final PrismEvent event = formatter.format(messageKitView, alertView, meta, contact);
                    data = OBJECT_MAPPER.writeValueAsString(event);
                } catch (Exception e) {
                    AppMonitor.get().countAlertFormatFailed(namespace);
                    LOG.error("Failed to format: alert_id={}, ns={}, view={}, meta={}, contact={}",
                            messageKitView.getAlertId(), namespace, alertView, meta, contact, e);
                    continue;
                }

                if (data.length() > maxPayloadSizeBytes) {
                    AppMonitor.get().countAlertEventTooBig(namespace, messageKit.getAlertId());
                    LOG.error("Event is too big: size={}, max_size={}, event={}",
                            data.length(), maxPayloadSizeBytes, data);
                    continue;
                }

                if (buf.length() + data.length() > maxPayloadSizeBytes) {
                    flush(namespace, buf.toString(), eventCount);
                    buf.setLength(0);
                    eventCount = 0;
                }
                buf.append(data);
                eventCount++;
            }
        }
        if (buf.length() != 0) {
            flush(namespace, buf.toString(), eventCount);
            buf.setLength(0);
        }
    }

    private void flush(String namespace, String payload, int eventCount) {
        try {
            client.send(payload);

            final AppMonitor appMonitor = AppMonitor.get();
            appMonitor.gaugeAlertSentEventsSize(eventCount, namespace);
            appMonitor.countAlertSendSuccess(namespace);
        } catch (Exception e) {
            AppMonitor.get().countAlertSendFailed(namespace);
            LOG.error("Send failed: ns={}, payload=<<{}>>", namespace, payload, e);
        }
    }

    public interface Builder extends net.opentsdb.horizon.alerting.Builder<Builder, PrismEmitter> {

        Builder setClient(PrismClient client);

        Builder setClientBuilder(PrismClient.Builder clientBuilder);

        Builder setFormatter(PrismFormatter formatter);

        Builder setFormatterBuilder(PrismFormatter.Builder formatterBuilder);

        Builder setMaxPayloadSizeBytes(int maxPayloadSizeBytes);

    }

    private static final class DefaultBuilder implements Builder {

        private PrismClient client;
        private PrismClient.Builder<?> clientBuilder;
        private PrismFormatter formatter;
        private PrismFormatter.Builder<?> formatterBuilder;
        private int maxPayloadSizeBytes;

        @Override
        public Builder setClient(PrismClient client) {
            this.client = client;
            return this;
        }

        @Override
        public Builder setClientBuilder(PrismClient.Builder clientBuilder) {
            this.clientBuilder = clientBuilder;
            return this;
        }

        @Override
        public Builder setFormatter(PrismFormatter formatter) {
            this.formatter = formatter;
            return this;
        }

        @Override
        public Builder setFormatterBuilder(PrismFormatter.Builder formatterBuilder) {
            this.formatterBuilder = formatterBuilder;
            return this;
        }

        @Override
        public Builder setMaxPayloadSizeBytes(int maxPayloadSizeBytes) {
            this.maxPayloadSizeBytes = maxPayloadSizeBytes;
            return this;
        }

        @Override
        public PrismEmitter build() {
            return new PrismEmitter(this);
        }
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }
}
