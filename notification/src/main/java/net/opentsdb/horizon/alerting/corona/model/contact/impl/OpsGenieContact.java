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

public class OpsGenieContact extends AbstractContact {

    private final String apiKey;

    public OpsGenieContact(final Builder<?> builder)
    {
        super(Type.OPSGENIE, builder);
        this.apiKey = builder.apiKey;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        OpsGenieContact that = (OpsGenieContact) o;
        return Objects.equals(apiKey, that.apiKey);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), apiKey);
    }

    @Override
    public String toString()
    {
        final String apiKeySnip = apiKey == null
                ? "null"
                : ("..." + apiKey.substring(Math.max(0, apiKey.length() - 6)));

        return "OpsGenieContact{" +
                super.toString() +
                ", apiKey='" + apiKeySnip + '\'' +
                '}';
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends AbstractContact.Builder<OpsGenieContact, B>
    {

        private String apiKey;

        public B setApiKey(final String apiKey)
        {
            this.apiKey = apiKey;
            return self();
        }

        public OpsGenieContact build()
        {
            return new OpsGenieContact(this);
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
