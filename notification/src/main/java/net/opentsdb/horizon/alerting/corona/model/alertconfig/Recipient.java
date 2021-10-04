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

package net.opentsdb.horizon.alerting.corona.model.alertconfig;

import java.util.Objects;

import lombok.Getter;

public class Recipient {

    @Getter
    private final int id;

    @Getter
    private final String name;

    public Recipient(int id, String name)
    {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipient recipient = (Recipient) o;
        return id == recipient.id &&
                Objects.equals(name, recipient.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, name);
    }

    @Override
    public String toString()
    {
        return "Recipient{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private int id;

        private String name;

        private Builder() {}

        public Builder setId(int id)
        {
            this.id = id;
            return this;
        }

        public Builder setName(String name)
        {
            this.name = name;
            return this;
        }

        public Recipient build()
        {
            return new Recipient(id, name);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}

