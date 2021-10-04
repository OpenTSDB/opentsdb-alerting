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

package net.opentsdb.horizon.alerting.corona.processor.emitter.oc;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.Processor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;

public class OcEmitter implements Processor<MessageKit> {

    private static final Logger LOG = LoggerFactory.getLogger(OcEmitter.class);

    private static final int TRY_TIMES = 5;

    /* ------------ Fields ------------ */

    private final OcClient client;

    private final Formatter<MessageKit, List<OcCommand>> formatter;

    private final Set<String> deniedNamespaces;

    /* ------------ Constructors ------------ */

    private OcEmitter(final OcClient client,
                      final Formatter<MessageKit, List<OcCommand>> formatter,
                      final List<String> deniedNamespaces) {
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(formatter, "formatter cannot be null");
        this.client = client;
        this.formatter = formatter;

        final Set<String> denied;
        if (deniedNamespaces == null || deniedNamespaces.isEmpty()) {
            denied = Collections.emptySet();
        } else {
            denied = deniedNamespaces.stream()
                    .filter(Objects::nonNull)
                    .map(namespace -> namespace.toLowerCase().trim())
                    .filter(namespace -> !namespace.isEmpty())
                    .collect(Collectors.toSet());
        }
        this.deniedNamespaces = denied;
    }

    /* ------------ Methods ------------ */

    private void sendOne(final String namespace,
                         final OcCommand command,
                         final List<OcContact> contacts)
    {
        for (OcContact contact : contacts) {
            sendWithRetry(namespace, command, contact);
        }
    }

    private void sendWithRetry(final String namespace,
                               final OcCommand command,
                               final OcContact contact)
    {
        for (int i = 1; i <= TRY_TIMES; i++) {
            try {
                client.send(command, contact);
                AppMonitor.get().countAlertSendSuccess(namespace);
                break;
            } catch (Exception e) {
                if (i < TRY_TIMES) {
                    // Try again.
                    continue;
                }
                AppMonitor.get().countAlertSendFailed(namespace);
                LOG.error("Failed to execute command: command={}, contact={}, error='{}'",
                        command, contact, e.getMessage());
            }
        }
    }

    private void send(final String namespace,
                      final List<OcCommand> commands,
                      final List<OcContact> contacts) {
        for (OcCommand command : commands) {
            sendOne(namespace, command, contacts);
        }
    }

    @Override
    public void process(final MessageKit messageKit) {
        if (messageKit.getType() != Contact.Type.OC) {
            LOG.error("Unexpected MessageKit type: {}", messageKit);
            return;
        }

        final String namespace = messageKit.getNamespace();
        @SuppressWarnings("unchecked") final List<OcContact> contacts =
                (List<OcContact>) messageKit.getContacts();

        if (deniedNamespaces.contains(namespace.toLowerCase().trim())) {
            AppMonitor.get().countYwrMessageKitDenied(namespace,  messageKit.getAlertId());
            LOG.debug("Deny OC messagekit: namespace={}, alert_id={}",
                    namespace, messageKit.getAlertId());
            return;
        }

        final List<OcCommand> commands;
        try {
            commands = formatter.format(messageKit);
        } catch (Exception e) {
            AppMonitor.get().countAlertFormatFailed(namespace);
            LOG.error("Format failed: message_kit={}", messageKit, e);
            return;
        }
        send(namespace, commands, contacts);
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private OcClient client;

        private Formatter<MessageKit, List<OcCommand>> formatter;

        private List<String> deniedNamespaces;

        public Builder setClient(final OcClient client) {
            this.client = client;
            return this;
        }

        public Builder setFormatter(final Formatter<MessageKit, List<OcCommand>> formatter) {
            this.formatter = formatter;
            return this;
        }

        public Builder setDeniedNamespaces(final List<String> deniedNamespaces) {
            this.deniedNamespaces = deniedNamespaces;
            return this;
        }

        public OcEmitter build() {
            return new OcEmitter(client, formatter, deniedNamespaces);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
