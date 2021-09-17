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

public class EmailContact extends AbstractContact {

    @Getter
    private final String email;

    public EmailContact(final Builder<?> builder)
    {
        super(Type.EMAIL, builder);
        Objects.requireNonNull(builder.email, "email cannot be null");
        this.email = builder.email;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        EmailContact that = (EmailContact) o;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), email);
    }

    @Override
    public String toString()
    {
        return "EmailContact{" +
                super.toString() +
                ", email='" + email + '\'' +
                '}';
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends AbstractContact.Builder<EmailContact, B>
    {

        private String email;

        public B setEmail(final String email)
        {
            this.email = email;
            return self();
        }

        public EmailContact build()
        {
            return new EmailContact(this);
        }
    }

    private static class BuilderImpl extends Builder<BuilderImpl> {

        @Override
        protected BuilderImpl self()
        {
            return this;
        }
    }

    public static Builder<?> builder() {
        return new BuilderImpl();
    }
}
