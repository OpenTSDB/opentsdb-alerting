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

package net.opentsdb.horizon.alerting.corona.model.contact.impl;

import java.util.Objects;

import net.opentsdb.horizon.alerting.corona.model.contact.AbstractContact;

public class SlackContact extends AbstractContact {

    private final String endpoint;

    public SlackContact(final Builder<?> builder)
    {
        super(Type.SLACK, builder);
        this.endpoint = builder.endpoint;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        SlackContact that = (SlackContact) o;
        return Objects.equals(endpoint, that.endpoint);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), endpoint);
    }

    @Override
    public String toString()
    {
        final String endpointSnip = endpoint == null
                ? "null"
                : ("..." + endpoint.substring(Math.max(0, endpoint.length() - 15)));

        return "SlackContact{" +
                super.toString() +
                ", endpoint='" + endpointSnip + '\'' +
                '}';
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends AbstractContact.Builder<SlackContact, B>
    {

        private String endpoint;

        public B setEndpoint(final String endpoint)
        {
            this.endpoint = endpoint;
            return self();
        }

        public SlackContact build()
        {
            return new SlackContact(this);
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

