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

package net.opentsdb.horizon.alerting.corona.processor.emitter.slack;

import java.util.List;
import java.util.Objects;

import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.SlackContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.Processor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackException;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackRequest;

public class SlackEmitter implements Processor<MessageKit> {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(SlackEmitter.class);

    /* ------------ Fields ------------ */

    private final SlackClient client;

    private final Formatter<MessageKit, List<SlackRequest>> formatter;

    /* ------------ Constructors ------------ */

    private SlackEmitter(
            final SlackClient client,
            final Formatter<MessageKit, List<SlackRequest>> formatter)
    {
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(client, "formatter cannot be null");
        this.client = client;
        this.formatter = formatter;
    }

    /* ------------ Methods ------------ */

    @Override
    public void process(final MessageKit mk)
    {
        if (mk.getType() != Contact.Type.SLACK) {
            LOG.error("Unexpected MessageKit: type={}", mk.getType());
            return;
        }

        final String namespace = mk
                .getAlertGroup()
                .getGroupKey()
                .getNamespace();

        final List<SlackRequest> requests;
        try {
            requests = formatter.format(mk);
        } catch (Exception e) {
            AppMonitor.get().countAlertFormatFailed(namespace);
            LOG.error("Format failed: message_kit={}, reason={}", mk, e.getMessage());
            return;
        }

        @SuppressWarnings("unchecked")
        final List<SlackContact> contacts =
                (List<SlackContact>) mk.getContacts();
        send(requests, contacts, namespace);
    }

    private void send(final List<SlackRequest> requests,
                      final List<SlackContact> contacts,
                      final String namespace)
    {
        for (SlackContact contact : contacts) {
            for (SlackRequest request : requests) {
                boolean ok = sendOne(request, contact);
                if (ok) {
                    AppMonitor.get().countAlertSendSuccess(namespace);
                } else {
                    AppMonitor.get().countAlertSendFailed(namespace);
                }
            }
        }
    }

    /**
     * Send single request to the given Slack contact.
     *
     * @param request Slack request
     * @param contact Slack contact
     * @return true on success, false on failure.
     */
    private boolean sendOne(final SlackRequest request, final SlackContact contact)
    {
        try {
            return client.send(request, contact.getEndpoint()).isOk();
        } catch (SlackException e) {
            LOG.error("Client error: request={}, contact={}, reason='{}'",
                    request, contact, e.getMessage());
        }
        return false;
    }

    /* ------------ Builder ------------ */

    public static class Builder
            implements net.opentsdb.horizon.alerting.Builder<Builder, SlackEmitter>
    {

        private SlackClient client;

        private Formatter<MessageKit, List<SlackRequest>> formatter;

        public Builder setClient(final SlackClient client)
        {
            this.client = client;
            return this;
        }

        public Builder setFormatter(
                final Formatter<MessageKit, List<SlackRequest>> formatter)
        {
            this.formatter = formatter;
            return this;
        }

        public SlackEmitter build()
        {
            return new SlackEmitter(client, formatter);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
