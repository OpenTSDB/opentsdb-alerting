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

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.contact.AbstractContact;

public class OcContact extends AbstractContact {

    @Getter
    private final String displayCount;

    @Getter
    private final String customer;

    @Getter
    private final String context;

    @Getter
    private final String opsdbProperty;

    public OcContact(final Builder<?> builder)
    {
        super(Type.OC, builder);
        this.displayCount = builder.displayCount;
        this.customer = builder.customer;
        this.context = builder.context;
        this.opsdbProperty = builder.opsdbProperty;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        OcContact that = (OcContact) o;
        return Objects.equals(displayCount, that.displayCount) &&
                Objects.equals(context, that.context) &&
                Objects.equals(opsdbProperty, that.opsdbProperty);

    }

    @Override
    public int hashCode()
    {
        return Objects.hash(
                super.hashCode(),
                displayCount,
                context,
                opsdbProperty
        );
    }

    @Override
    public String toString()
    {
        return "OcContact{" +
                super.toString() +
                ", displayCount='" + displayCount + '\'' +
                ", customer='" + customer + '\'' +
                ", context='" + context + '\'' +
                ", opsdbProperty='" + opsdbProperty + '\'' +
                '}';
    }

    public abstract static class Builder<B extends Builder<B>>
            extends AbstractContact.Builder<OcContact, B>
    {

        private String displayCount;

        private String customer;

        private String context;

        private String opsdbProperty;

        public B setDisplayCount(final String displayCount)
        {
            this.displayCount = displayCount;
            return self();
        }

        public B setCustomer(final String customer)
        {
            this.customer = customer;
            return self();
        }

        public B setContext(final String context)
        {
            this.context = context;
            return self();
        }

        public B setOpsdbProperty(final String opsdbProperty)
        {
            this.opsdbProperty = opsdbProperty;
            return self();
        }

        public OcContact build()
        {
            return new OcContact(this);
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
