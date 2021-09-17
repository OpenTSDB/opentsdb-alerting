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

package net.opentsdb.horizon.alerting.corona.model.messagekit;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.Meta;

public abstract class AbstractMessageKit {

    /* ------------ Fields ------------ */

    @Getter
    protected final Contact.Type type;

    @Getter
    protected final List<? extends Contact> contacts;

    @Getter
    protected final Meta meta;

    /* ------------ Constructor ------------ */

    protected AbstractMessageKit(final Builder<?> builder)
    {
        Objects.requireNonNull(builder.type, "type cannot be null");
        Objects.requireNonNull(builder.contacts, "contacts cannot be null");
        Objects.requireNonNull(builder.meta, "meta cannot be null");
        this.type = builder.type;
        this.contacts = builder.contacts;
        this.meta = builder.meta;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractMessageKit that = (AbstractMessageKit) o;
        return type == that.type &&
                contacts.equals(that.contacts) &&
                meta.equals(that.meta);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, contacts, meta);
    }

    @Override
    public String toString()
    {
        return "type=" + type +
                ", contacts=" + contacts +
                ", meta=" + meta;
    }

    /* ------------ Builder ------------ */

    abstract static class Builder<B extends Builder<B>> {

        private Contact.Type type;

        private List<? extends Contact> contacts;

        private Meta meta;

        protected abstract B self();

        public B setType(Contact.Type type)
        {
            this.type = type;
            return self();
        }

        public B setContacts(List<? extends Contact> contacts)
        {
            this.contacts = contacts;
            return self();
        }

        public B setContacts(Contact... contacts)
        {
            this.contacts = Arrays.asList(contacts);
            return self();
        }

        public B setMeta(Meta meta)
        {
            this.meta = meta;
            return self();
        }
    }
}
