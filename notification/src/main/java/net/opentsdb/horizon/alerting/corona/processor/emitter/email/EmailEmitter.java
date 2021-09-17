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

package net.opentsdb.horizon.alerting.corona.processor.emitter.email;

import java.util.List;
import java.util.Objects;

import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.EmailContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.mail.EmailException;

import net.opentsdb.horizon.alerting.corona.processor.Processor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;

public class EmailEmitter implements Processor<MessageKit> {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(EmailEmitter.class);

    /* ------------ Fields ------------ */

    private final EmailClient client;

    private final Formatter<MessageKit, EmailMessage> formatter;

    private final int maxSendAttempts;

    /* ------------ Constructor ------------ */

    protected EmailEmitter(final Builder builder)
    {
        Objects.requireNonNull(builder.client, "client cannot be null");
        Objects.requireNonNull(builder.formatter, "formatter cannot be null");
        if (builder.maxSendAttempts <= 0) {
            throw new IllegalArgumentException(
                    "maxSendAttempts cannot be <= 0. Given: " +
                            builder.maxSendAttempts);
        }
        this.client = builder.client;
        this.formatter = builder.formatter;
        this.maxSendAttempts = builder.maxSendAttempts;
    }

    /* ------------ Methods ------------ */

    private String[] getEmails(final List<? extends Contact> contacts)
    {
        @SuppressWarnings("unchecked")
        final List<EmailContact> emailContacts = (List<EmailContact>) contacts;

        return emailContacts.stream()
                .map(c -> c.getEmail() != null ? c.getEmail() : c.getName())
                .filter(Objects::nonNull)
                .filter(email -> !email.trim().isEmpty())
                .toArray(String[]::new);
    }

    private void timedSend(final EmailMessage message, final String[] emails)
            throws EmailException
    {
        final long start = System.currentTimeMillis();

        {
            // TODO: Extract the inner MultiPartEmail and resend it,
            // instead of rebuilding every time. Maybe...
            client.send(message, emails);
        }

        final long latency = System.currentTimeMillis() - start;
        AppMonitor.get().timeAlertSendLatencyMs(latency);
    }

    /**
     * Sends message to the given recipients.
     * <p>
     * Retry logic is used. {@code false} is returned only if we failed
     * after retries.
     *
     * @param message email message to send
     * @param emails  recipient emails
     * @return {@code true} on success, {@code false} on error.
     */
    private boolean send(final EmailMessage message, final String[] emails)
    {
        for (int i = 0; i < maxSendAttempts; i++) {
            try {
                timedSend(message, emails);
                return true;
            } catch (EmailException e) {
                LOG.debug("Failed to send: subject='{}', to='{}', reason='{}'",
                        message.getSubject(), emails, e.getMessage());
            }
        }
        LOG.error("Failed send after {} attempts: subject='{}', to='{}'",
                maxSendAttempts, message.getSubject(), emails);
        return false;
    }

    @Override
    public void process(final MessageKit messageKit)
    {
        if (messageKit.getType() != Contact.Type.EMAIL) {
            LOG.error("Unexpected MessageKit type: {}", messageKit);
            return;
        }

        final String[] emails;
        try {
            emails = getEmails(messageKit.getContacts());
        } catch (Exception e) {
            LOG.error("Failed to get email contacts: reason={}", e.getMessage());
            return;
        }

        final String namespace =
                messageKit
                        .getAlertGroup()
                        .getGroupKey()
                        .getNamespace();

        final EmailMessage message;
        try {
            message = formatter.format(messageKit);
        } catch (Exception e) {
            AppMonitor.get().countAlertFormatFailed(namespace);
            LOG.error("Failed to format: message_kit={}, reason='{}'",
                    messageKit, e.getMessage());
            return;
        }

        final boolean sendOk = send(message, emails);
        if (sendOk) {
            AppMonitor.get().countAlertSendSuccess(namespace);
        } else {
            AppMonitor.get().countAlertSendFailed(namespace);
        }
    }

    /* ------------ Builder ------------ */

    public final static class Builder {

        private EmailClient client;

        private Formatter<MessageKit, EmailMessage> formatter;

        private int maxSendAttempts = 3;

        private Builder() { }

        public Builder setEmailClient(final EmailClient client)
        {
            this.client = client;
            return this;
        }

        public Builder setFormatter(
                final Formatter<MessageKit, EmailMessage> formatter)
        {
            this.formatter = formatter;
            return this;
        }

        public Builder setMaxSendAttempts(final int maxSendAttempts)
        {
            this.maxSendAttempts = maxSendAttempts;
            return this;
        }

        public EmailEmitter build()
        {
            return new EmailEmitter(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}