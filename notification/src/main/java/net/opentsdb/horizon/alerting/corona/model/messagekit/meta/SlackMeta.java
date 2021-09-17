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

import javax.annotation.concurrent.ThreadSafe;

import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;

@ThreadSafe
public class SlackMeta extends Meta {

    /* ------------ Static Methods ------------ */

    public static SlackMeta from(final Metadata metadata)
    {
        return builder()
                .setSubject(metadata.getSubject())
                .setBody(metadata.getBody())
                .setLabels(metadata.getLabels())
                .build();
    }

    /* ------------ Constructor ------------ */

    SlackMeta(final Builder<?> builder)
    {
        super(builder);
    }

    @Override
    public boolean equals(final Object o)
    {
        return super.equals(o);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public String toString()
    {
        return "SlackMeta{" +
                super.toString() +
                '}';
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends Meta.Builder<SlackMeta, B>
    {
        @Override
        public SlackMeta build()
        {
            return new SlackMeta(this);
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
