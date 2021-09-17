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

package net.opentsdb.horizon.alerting.corona.model.contact;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.contact.impl.EmailContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.WebhookContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OpsGenieContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.SlackContact;

@Getter
public class Contacts {

    /* ------------ Static Methods ------------ */

    @SafeVarargs
    public static <C extends Contact> List<C> of(C... cs)
    {
        return Arrays.asList(cs);
    }

    private <C> List<C> normalize(List<C> contacts)
    {
        if (contacts == null || contacts.size() == 0) {
            return Collections.emptyList();
        }
        return contacts;
    }

    /* ------------ Fields ------------ */

    private final List<EmailContact> emailContacts;

    private final List<WebhookContact> webhookContacts;

    private final List<OcContact> ocContacts;

    private final List<OpsGenieContact> opsGenieContacts;

    private final List<SlackContact> slackContacts;

    /* ------------ Constructor ------------ */

    private Contacts(final List<EmailContact> emailContacts,
                     final List<WebhookContact> webhookContacts,
                     final List<OcContact> ocContacts,
                     final List<OpsGenieContact> opsGenieContacts,
                     final List<SlackContact> slackContacts)
    {
        this.emailContacts = normalize(emailContacts);
        this.webhookContacts = normalize(webhookContacts);
        this.ocContacts = normalize(ocContacts);
        this.opsGenieContacts = normalize(opsGenieContacts);
        this.slackContacts = normalize(slackContacts);
    }

    /* ------------ Methods ------------ */

    public Stream<Contact> stream()
    {
        return Stream.of(
                emailContacts,
                webhookContacts,
                ocContacts,
                opsGenieContacts,
                slackContacts
        ).flatMap(List::stream);
    }

    public List<? extends Contact> getContacts(final Contact.Type type)
    {
        switch (type) {
            case EMAIL:
                return getEmailContacts();
            case WEBHOOK:
                return getWebhookContacts();
            case OC:
                return getOcContacts();
            case OPSGENIE:
                return getOpsGenieContacts();
            case SLACK:
                return getSlackContacts();
        }
        return null;
    }

    public void forEach(
            final BiConsumer<Contact.Type, List<? extends Contact>> consumer)
    {
        Arrays.stream(Contact.Type.values()).forEach(t ->
                consumer.accept(t, getContacts(t))
        );
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Contacts that = (Contacts) o;
        return Objects.equals(emailContacts, that.emailContacts) &&
                Objects.equals(webhookContacts, that.webhookContacts) &&
                Objects.equals(ocContacts, that.ocContacts) &&
                Objects.equals(opsGenieContacts, that.opsGenieContacts) &&
                Objects.equals(slackContacts, that.slackContacts);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(
                emailContacts,
                webhookContacts,
                ocContacts,
                opsGenieContacts,
                slackContacts
        );
    }

    @Override
    public String toString()
    {
        return "Contacts{" +
                "emailContacts=" + emailContacts +
                ", webhookContacts=" + webhookContacts +
                ", ocContacts=" + ocContacts +
                ", opsGenieContacts=" + opsGenieContacts +
                ", slackContacts=" + slackContacts +
                '}';
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private List<EmailContact> emailContacts;

        private List<WebhookContact> webhookContacts;

        private List<OcContact> ocContacts;

        private List<OpsGenieContact> opsGenieContacts;

        private List<SlackContact> slackContacts;

        public Builder setEmailContacts(final List<EmailContact> emailContacts)
        {
            this.emailContacts = emailContacts;
            return this;
        }

        public Builder setWebhookContacts(final List<WebhookContact> webhookContacts)
        {
            this.webhookContacts = webhookContacts;
            return this;
        }

        public Builder setOcContacts(final List<OcContact> ocContacts)
        {
            this.ocContacts = ocContacts;
            return this;
        }

        public Builder setOpsGenieContacts(final List<OpsGenieContact> opsGenieContacts)
        {
            this.opsGenieContacts = opsGenieContacts;
            return this;
        }

        public Builder setSlackContacts(final List<SlackContact> slackContacts)
        {
            this.slackContacts = slackContacts;
            return this;
        }

        public Builder setEmailContacts(final EmailContact... emailContacts)
        {
            this.emailContacts = Arrays.asList(emailContacts);
            return this;
        }

        public Builder setWebhookContacts(final WebhookContact... webhookContacts)
        {
            this.webhookContacts = Arrays.asList(webhookContacts);
            return this;
        }

        public Builder setOcContacts(final OcContact... ocContacts)
        {
            this.ocContacts = Arrays.asList(ocContacts);
            return this;
        }

        public Builder setOpsGenieContacts(final OpsGenieContact... opsGenieContacts)
        {
            this.opsGenieContacts = Arrays.asList(opsGenieContacts);
            return this;
        }

        public Builder setSlackContacts(final SlackContact... slackContacts)
        {
            this.slackContacts = Arrays.asList(slackContacts);
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder setContacts(final Contact.Type type,
                                   final List<? extends Contact> contacts)
        {
            switch (type) {
                case EMAIL:
                    return setEmailContacts((List<EmailContact>) contacts);
                case WEBHOOK:
                    return setWebhookContacts((List<WebhookContact>) contacts);
                case OC:
                    return setOcContacts((List<OcContact>) contacts);
                case OPSGENIE:
                    return setOpsGenieContacts((List<OpsGenieContact>) contacts);
                case SLACK:
                    return setSlackContacts((List<SlackContact>) contacts);
            }
            return this;
        }

        public Contacts build()
        {
            return new Contacts(
                    emailContacts,
                    webhookContacts,
                    ocContacts,
                    opsGenieContacts,
                    slackContacts
            );
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
