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

public class PagerDutyContact extends AbstractContact {

    private final String routingKey;

    public PagerDutyContact(final Builder<?> builder)
    {
        super(Type.PAGERDUTY, builder);
        this.routingKey = builder.routingKey;
    }

    public String getRoutingKey()
    {
        return routingKey;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        PagerDutyContact that = (PagerDutyContact) o;
        return Objects.equals(routingKey, that.routingKey);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), routingKey);
    }

    @Override
    public String toString()
    {
        final String routingKeySnip = routingKey == null
                ? "null"
                : ("..." + routingKey.substring(Math.max(0, routingKey.length() - 6)));

        return "PagerDutyContact{" +
                super.toString() +
                ", routingKey='" + routingKeySnip + '\'' +
                '}';
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends AbstractContact.Builder<PagerDutyContact, B>
    {

        private String routingKey;

        public B setRoutingKey(final String routingKey)
        {
            this.routingKey = routingKey;
            return self();
        }

        public PagerDutyContact build() { return new PagerDutyContact(this); }
    }

    private static class BuilderImpl extends Builder<BuilderImpl> {

        @Override
        protected BuilderImpl self() { return this; }
    }

    public static Builder<?> builder() { return new BuilderImpl(); }
}
