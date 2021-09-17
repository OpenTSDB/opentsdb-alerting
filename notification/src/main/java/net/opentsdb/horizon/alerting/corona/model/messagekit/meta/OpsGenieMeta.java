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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.concurrent.ThreadSafe;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;

@ThreadSafe
public class OpsGenieMeta extends Meta {

    /* ------------ Static Methods ------------ */

    public static OpsGenieMeta from(final Metadata metadata)
    {
        return builder()
                .setSubject(metadata.getSubject())
                .setBody(metadata.getBody())
                .setLabels(metadata.getLabels())
                .setOpsGeniePriority(metadata.getOpsGeniePriority())
                .setOpsGenieAutoClose(metadata.isOpsGenieAutoClose())
                .setOpsGenieTags(metadata.getOpsGenieTags())
                .build();
    }

    /* ------------ Fields ------------ */

    @Getter
    private final String opsGeniePriority;

    @Getter
    private final boolean opsGenieAutoClose;

    @Getter
    private final List<String> opsGenieTags;

    /* ------------ Constructor ------------ */

    OpsGenieMeta(final Builder<?> builder)
    {
        super(builder);
        this.opsGeniePriority = builder.opsGeniePriority;
        this.opsGenieAutoClose = builder.opsGenieAutoClose;
        this.opsGenieTags = builder.opsGenieTags == null
                ? Collections.emptyList()
                : builder.opsGenieTags;
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean equals(final Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        final OpsGenieMeta that = (OpsGenieMeta) o;
        return Objects.equals(opsGeniePriority, that.opsGeniePriority)
                && Objects.equals(opsGenieAutoClose, that.opsGenieAutoClose)
                && Objects.equals(opsGenieTags, that.opsGenieTags);
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hash(opsGeniePriority, opsGenieAutoClose, opsGenieTags);
    }

    @Override
    public String toString()
    {
        return "OpsGenieMeta{" +
                "opsGeniePriority='" + opsGeniePriority + '\'' +
                ", opsGenieAutoClose=" + opsGenieAutoClose +
                ", opsGenieTags=" + opsGenieTags +
                '}';
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends Meta.Builder<OpsGenieMeta, B>
    {

        private String opsGeniePriority;

        private boolean opsGenieAutoClose;

        private List<String> opsGenieTags;

        public B setOpsGeniePriority(final String opsGeniePriority)
        {
            this.opsGeniePriority = opsGeniePriority;
            return self();
        }

        public B setOpsGenieAutoClose(final boolean opsGenieAutoClose)
        {
            this.opsGenieAutoClose = opsGenieAutoClose;
            return self();
        }

        public B setOpsGenieTags(final List<String> opsGenieTags)
        {
            this.opsGenieTags = opsGenieTags;
            return self();
        }

        @Override
        public OpsGenieMeta build()
        {
            return new OpsGenieMeta(this);
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
