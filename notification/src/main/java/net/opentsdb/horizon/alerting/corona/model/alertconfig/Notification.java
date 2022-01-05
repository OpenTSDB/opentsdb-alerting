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

package net.opentsdb.horizon.alerting.corona.model.alertconfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;

public class Notification {

    /* ------------ Fields ------------ */

    @Getter
    private final List<String> transitionsToNotify;

    @Getter
    private final Map<Contact.Type, List<Recipient>> recipients;

    @Getter
    private final String subject;

    @Getter
    private final String body;

    @Getter
    private final String opsGeniePriority;

    @Getter
    private final boolean opsGenieAutoClose;

    @Getter
    private final List<String> opsGenieTags;

    @Getter
    private final String runbookId;

    @Getter
    private final OcSeverity ocSeverity;

    @Getter
    private final OcTier ocTier;

    @Getter
    private final boolean pagerDutyAutoClose;

    /* ------------ Constructor ------------ */

    Notification(final Builder builder)
    {
        Objects.requireNonNull(builder, "builder cannot be null");
        this.transitionsToNotify = builder.transitionsToNotify;
        this.recipients = safetify(builder.recipients);
        this.subject = builder.subject;
        this.body = builder.body;
        this.opsGeniePriority = builder.opsGeniePriority;
        this.opsGenieAutoClose = builder.opsGenieAutoClose;
        this.opsGenieTags = builder.opsGenieTags == null
                ? Collections.emptyList()
                : builder.opsGenieTags;
        this.runbookId = builder.runbookId;
        this.ocSeverity = builder.ocSeverity == null
                ? OcSeverity.NOT_SET
                : builder.ocSeverity;
        this.ocTier = builder.ocTier == null
                ? OcTier.NOT_SET
                : builder.ocTier;
        this.pagerDutyAutoClose = builder.pagerDutyAutoClose;
    }

    private static Map<Contact.Type, List<Recipient>> safetify(
            final Map<Contact.Type, List<Recipient>> recipients)
    {
        if (recipients == null) {
            return Collections.emptyMap();
        }
        final Map<Contact.Type, List<Recipient>> secured =
                new HashMap<>(recipients.size());

        recipients.forEach((type, list) -> {
            if (list == null || list.isEmpty()) {
                return;
            }
            list = list.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!list.isEmpty()) {
                secured.put(type, list);
            }
        });

        return secured;
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Notification that = (Notification) o;
        return Objects.equals(transitionsToNotify, that.transitionsToNotify) &&
                Objects.equals(recipients, that.recipients) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(body, that.body) &&
                Objects.equals(opsGeniePriority, that.opsGeniePriority) &&
                Objects.equals(opsGenieAutoClose, that.opsGenieAutoClose) &&
                Objects.equals(opsGenieTags, that.opsGenieTags) &&
                Objects.equals(runbookId, that.runbookId) &&
                Objects.equals(ocSeverity, that.ocSeverity) &&
                Objects.equals(ocTier, that.ocTier) &&
                Objects.equals(pagerDutyAutoClose, that.pagerDutyAutoClose);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(
                transitionsToNotify,
                recipients,
                subject,
                body,
                opsGeniePriority,
                opsGenieAutoClose,
                opsGenieTags,
                runbookId,
                ocSeverity,
                ocTier,
                pagerDutyAutoClose
        );
    }

    @Override
    public String toString()
    {
        return "Notification{" +
                "transitionsToNotify=" + transitionsToNotify +
                ", recipients=" + recipients +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", opsGeniePriority='" + opsGeniePriority + '\'' +
                ", opsGenieAutoClose=" + opsGenieAutoClose +
                ", opsGenieTags=" + opsGenieTags +
                ", runbookId='" + runbookId + '\'' +
                ", ocSeverity=" + ocSeverity +
                ", ocTier=" + ocTier +
                ", pagerDutyAutoClose=" + pagerDutyAutoClose +
                '}';
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        /* ------------ Fields ------------ */

        private List<String> transitionsToNotify;

        private Map<Contact.Type, List<Recipient>> recipients;

        private String subject;

        private String body;

        private String opsGeniePriority;

        private boolean opsGenieAutoClose;

        private List<String> opsGenieTags;

        private String runbookId;

        private OcSeverity ocSeverity;

        private OcTier ocTier;

        private boolean pagerDutyAutoClose;

        /* ------------ Constructor ------------ */

        private Builder() {}

        /* ------------ Methods ------------ */

        public Builder setTransitionsToNotify(
                List<String> transitionsToNotify)
        {
            this.transitionsToNotify = transitionsToNotify;
            return this;
        }

        public Builder setRecipients(
                Map<Contact.Type, List<Recipient>> recipients)
        {
            this.recipients = recipients;
            return this;
        }

        public Builder setSubject(String subject)
        {
            this.subject = subject;
            return this;
        }

        public Builder setBody(String body)
        {
            this.body = body;
            return this;
        }

        public Builder setOpsGeniePriority(String opsGeniePriority)
        {
            this.opsGeniePriority = opsGeniePriority;
            return this;
        }

        public Builder setOpsGenieAutoClose(boolean opsGenieAutoClose)
        {
            this.opsGenieAutoClose = opsGenieAutoClose;
            return this;
        }

        public Builder setOpsGenieTags(List<String> opsGenieTags)
        {
            this.opsGenieTags = opsGenieTags;
            return this;
        }

        public Builder setRunbookId(String runbookId)
        {
            this.runbookId = runbookId;
            return this;
        }

        public Builder setOcSeverity(OcSeverity ocSeverity)
        {
            this.ocSeverity = ocSeverity;
            return this;
        }

        public Builder setOcTier(OcTier ocTier)
        {
            this.ocTier = ocTier;
            return this;
        }

        public Builder setPagerDutyAutoClose(boolean pagerDutyAutoClose)
        {
            this.pagerDutyAutoClose = pagerDutyAutoClose;
            return this;
        }

        public Notification build()
        {
            return new Notification(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
