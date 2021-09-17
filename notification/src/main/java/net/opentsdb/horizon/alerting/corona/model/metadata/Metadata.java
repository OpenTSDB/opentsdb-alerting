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

package net.opentsdb.horizon.alerting.corona.model.metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

public class Metadata {

    /* ------------ Constants ------------ */

    private static final String[] EMPTY_LABELS = new String[0];

    /* ------------ Fields ------------ */

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
    private final String[] labels;

    /* ------------ Constructor ------------ */

    private Metadata(final Builder builder)
    {
        this.subject = builder.subject;
        this.body = builder.body;
        this.opsGeniePriority = builder.opsGeniePriority;
        this.opsGenieAutoClose = builder.opsGenieAutoClose;
        this.opsGenieTags = builder.opsGenieTags == null
                ? Collections.emptyList()
                : builder.opsGenieTags;
        this.runbookId = builder.runbookId;
        this.ocSeverity = builder.ocSeverity;
        this.ocTier = builder.ocTier;
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
        Metadata other = (Metadata) o;
        return Objects.equals(subject, other.subject) &&
                Objects.equals(body, other.body) &&
                Objects.equals(opsGeniePriority, other.opsGeniePriority) &&
                Objects.equals(opsGenieAutoClose, other.opsGenieAutoClose) &&
                Objects.equals(opsGenieTags, other.opsGenieTags) &&
                Objects.equals(runbookId, other.runbookId) &&
                Objects.equals(ocSeverity, other.ocSeverity) &&
                Objects.equals(ocTier, other.ocTier) &&
                Arrays.equals(labels, other.labels);
    }

    @Override
    public int hashCode()
    {
        return 31 * Arrays.hashCode(labels) +
                Objects.hash(
                        subject,
                        body,
                        opsGeniePriority,
                        opsGenieAutoClose,
                        opsGenieTags,
                        runbookId,
                        ocSeverity,
                        ocTier
                );
    }

    @Override
    public String toString()
    {
        return "Metadata{" +
                "subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", opsGeniePriority='" + opsGeniePriority + '\'' +
                ", opsGenieAutoClose=" + opsGenieAutoClose +
                ", opsGenieTags=" + opsGenieTags +
                ", runbookId='" + runbookId + '\'' +
                ", ocSeverity=" + ocSeverity +
                ", ocTier=" + ocTier +
                ", labels=" + Arrays.toString(labels) +
                '}';
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private String subject;

        private String body;

        private String opsGeniePriority;

        private boolean opsGenieAutoClose;

        private List<String> opsGenieTags;

        private String runbookId;

        private OcSeverity ocSeverity;

        private OcTier ocTier;

        private String[] labels;

        private Builder() {}

        public Builder setSubject(final String subject)
        {
            this.subject = subject;
            return this;
        }

        public Builder setBody(final String body)
        {
            this.body = body;
            return this;
        }

        public Builder setOpsGeniePriority(final String opsGeniePriority)
        {
            this.opsGeniePriority = opsGeniePriority;
            return this;
        }

        public Builder setOpsGenieAutoClose(final boolean opsGenieAutoClose)
        {
            this.opsGenieAutoClose = opsGenieAutoClose;
            return this;
        }

        public Builder setOpsGenieTags(final List<String> opsGenieTags)
        {
            this.opsGenieTags = opsGenieTags;
            return this;
        }

        public Builder setRunbookId(final String runbookId)
        {
            this.runbookId = runbookId;
            return this;
        }

        public Builder setOcSeverity(final OcSeverity ocSeverity)
        {
            this.ocSeverity = ocSeverity;
            return this;
        }

        public Builder setOcTier(final OcTier ocTier)
        {
            this.ocTier = ocTier;
            return this;
        }

        public Builder setLabels(final String... labels)
        {
            this.labels = labels;
            return this;
        }

        public Metadata build()
        {
            return new Metadata(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
