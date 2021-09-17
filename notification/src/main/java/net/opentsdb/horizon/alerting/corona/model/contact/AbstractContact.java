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

package net.opentsdb.horizon.alerting.corona.model.contact;

import java.util.Objects;

import lombok.Getter;

public abstract class AbstractContact implements Contact {

    @Getter
    protected final Type type;

    @Getter
    protected final int id;

    @Getter
    protected final String name;

    protected AbstractContact(final Type type, final Builder<?, ?> builder)
    {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(builder, "builder cannot be null");
        Objects.requireNonNull(builder.name, "name cannot be null");
        this.type = type;
        this.id = builder.id;
        this.name = builder.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractContact that = (AbstractContact) o;
        return id == that.id &&
                type == that.type &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id, name);
    }

    @Override
    public String toString() {
        return "type=" + type +
                ", id=" + id +
                ", name='" + name + '\'';
    }

    /* ------------ Builder ------------ */

    protected abstract static class Builder<C, B extends Builder<C, B>> {

        private int id;

        private String name;

        protected abstract B self();

        protected abstract C build();

        public B setId(final int id)
        {
            this.id = id;
            return self();
        }

        public B setName(final String name)
        {
            this.name = name;
            return self();
        }
    }
}
