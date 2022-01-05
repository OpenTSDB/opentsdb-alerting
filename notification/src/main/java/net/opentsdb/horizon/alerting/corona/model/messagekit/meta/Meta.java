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

package net.opentsdb.horizon.alerting.corona.model.messagekit.meta;

import java.util.Arrays;
import java.util.Objects;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;

public abstract class Meta {

    /* ------------ Constants ------------ */

    private static final String[] EMPTY_LABELS = new String[0];

    /* ------------ Static Methods ------------ */

    public static Meta from(final Contact.Type type,
                            final Metadata metadata)
    {
        Objects.requireNonNull(metadata, "metadata cannot be null");
        switch (type) {
            case EMAIL:
                return EmailMeta.from(metadata);
            case WEBHOOK:
                return WebhookMeta.from(metadata);
            case OC:
                return OcMeta.from(metadata);
            case OPSGENIE:
                return OpsGenieMeta.from(metadata);
            case SLACK:
                return SlackMeta.from(metadata);
            case PAGERDUTY:
                return PagerDutyMeta.from(metadata);
            default:
                throw new IllegalArgumentException("Unknown type=" + type);
        }
    }

    /* ------------ Fields ------------ */

    @Getter
    private final String subject;

    @Getter
    private final String body;

    @Getter
    private final String[] labels;

    /* ------------ Constructor ------------ */

    protected Meta(final Builder<?, ?> builder)
    {
        this.subject = builder.subject;
        this.body = builder.body;
        this.labels = builder.labels == null ? EMPTY_LABELS : builder.labels;
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
        Meta that = (Meta) o;
        return Objects.equals(subject, that.subject) &&
                Objects.equals(body, that.body) &&
                Arrays.equals(labels, that.labels);
    }

    @Override
    public int hashCode()
    {
        return 31 * Objects.hash(subject, body) + Arrays.hashCode(labels);
    }

    @Override
    public String toString()
    {
        return "subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", labels=" + Arrays.toString(labels);
    }

    /* ------------ Builder ------------ */

    protected abstract static class Builder<
            O extends Meta,
            B extends Builder<O, B>
            >
    {

        private String subject;

        private String body;

        private String[] labels;

        protected abstract B self();

        protected abstract O build();

        public B setSubject(final String subject)
        {
            this.subject = subject;
            return self();
        }

        public B setBody(final String body)
        {
            this.body = body;
            return self();
        }

        public B setLabels(final String... labels)
        {
            this.labels = labels;
            return self();
        }
    }
}
