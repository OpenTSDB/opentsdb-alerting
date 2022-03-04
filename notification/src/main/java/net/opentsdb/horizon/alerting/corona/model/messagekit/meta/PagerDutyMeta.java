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

@ThreadSafe
public class PagerDutyMeta extends Meta {

    /* ------------ Static Methods ------------ */

    public static PagerDutyMeta from(final Metadata metadata)
    {
        return builder()
                .setSubject(metadata.getSubject())
                .setBody(metadata.getBody())
                .setLabels(metadata.getLabels())
                .setPagerDutyAutoClose(metadata.isPagerDutyAutoClose())
                .build();
    }

    /* ------------ Fields ------------ */

    @Getter
    private final boolean pagerDutyAutoClose;

    /* ------------ Constructor ------------ */

    PagerDutyMeta(final Builder<?> builder)
    {
        super(builder);
        this.pagerDutyAutoClose = builder.pagerDutyAutoClose;
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean equals(final Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        final PagerDutyMeta that = (PagerDutyMeta) o;
        return Objects.equals(pagerDutyAutoClose, that.pagerDutyAutoClose);
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hash(pagerDutyAutoClose);
    }

    @Override
    public String toString()
    {
        return "PagerdutyMeta{" +
                "pagerDutyPriority=" + pagerDutyAutoClose +
                '}';
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends Meta.Builder<PagerDutyMeta, B>
    {
        private boolean pagerDutyAutoClose;

        public B setPagerDutyAutoClose(final boolean pagerDutyAutoClose)
        {
            this.pagerDutyAutoClose = pagerDutyAutoClose;
            return self();
        }

        @Override
        public PagerDutyMeta build() { return new PagerDutyMeta(this); }
    }

    private static class BuilderImpl extends Builder<BuilderImpl> {
        @Override
        protected BuilderImpl self() { return this; }
    }

    public static Builder<?> builder() { return new BuilderImpl(); }
}
