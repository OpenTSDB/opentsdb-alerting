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

import java.util.Objects;

import javax.annotation.concurrent.ThreadSafe;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;

@ThreadSafe
public class OcMeta extends Meta {

    /* ------------ Static Methods ------------ */

    public static OcMeta from(final Metadata metadata)
    {
        return OcMeta.builder()
                .setSubject(metadata.getSubject())
                .setBody(metadata.getBody())
                .setLabels(metadata.getLabels())
                .setOcSeverity(metadata.getOcSeverity())
                .setOcTier(metadata.getOcTier())
                .setRunbookId(metadata.getRunbookId())
                .build();
    }

    /* ------------ Fields ------------ */

    @Getter
    private final OcSeverity ocSeverity;

    @Getter
    private final OcTier ocTier;

    @Getter
    private final String runbookId;

    /* ------------ Constructor ------------ */

    OcMeta(final Builder<?> builder)
    {
        super(builder);
        Objects.requireNonNull(builder.ocSeverity, "ocSeverity cannot be null");
        Objects.requireNonNull(builder.ocTier, "ocTier cannot be null");
        this.ocSeverity = builder.ocSeverity;
        this.ocTier = builder.ocTier;
        this.runbookId = builder.runbookId;
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean equals(final Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        final OcMeta that = (OcMeta) o;
        return Objects.equals(ocSeverity, that.ocSeverity) &&
                Objects.equals(ocTier, that.ocTier) &&
                Objects.equals(runbookId, that.runbookId);
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() +
                Objects.hash(ocSeverity, ocTier, runbookId);
    }

    @Override
    public String toString()
    {
        return "OcMeta{" +
                super.toString() +
                ", ocSeverity='" + ocSeverity + '\'' +
                ", ocTier='" + ocTier + '\'' +
                ", runbookId='" + runbookId + '\'' +
                '}';
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends Meta.Builder<OcMeta, B>
    {

        private OcSeverity ocSeverity;

        private OcTier ocTier;

        private String runbookId;

        public B setOcSeverity(final OcSeverity ocSeverity)
        {
            this.ocSeverity = ocSeverity;
            return self();
        }

        public B setOcTier(final OcTier ocTier)
        {
            this.ocTier = ocTier;
            return self();
        }

        public B setRunbookId(final String runbookId)
        {
            this.runbookId = runbookId;
            return self();
        }

        @Override
        public OcMeta build()
        {
            return new OcMeta(this);
        }
    }

    private static class BuilderImpl extends Builder<BuilderImpl> {
        @Override
        protected BuilderImpl self()
        {
            return this;
        }
    }

    public static Builder<?> builder()
    {
        return new BuilderImpl();
    }
}
