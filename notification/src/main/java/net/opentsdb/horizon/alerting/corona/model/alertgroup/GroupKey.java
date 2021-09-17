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

package net.opentsdb.horizon.alerting.corona.model.alertgroup;

import java.util.Arrays;
import java.util.Objects;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;

public class GroupKey {

    @Getter
    private final String namespace;

    @Getter
    private final long alertId;

    @Getter
    private final AlertType alertType;

    @Getter
    private final String[] keys;

    @Getter
    private final String[] values;

    GroupKey(final Builder builder)
    {
        Objects.requireNonNull(builder.namespace, "namespace cannot be null");
        Objects.requireNonNull(builder.alertType, "alertType cannot be null");
        this.namespace = builder.namespace;
        this.alertId = builder.alertId;
        this.alertType = builder.alertType;
        this.keys = builder.keys != null ?
                builder.keys : new String[]{};
        this.values = builder.values != null ?
                builder.values : new String[]{};
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
        GroupKey groupKey = (GroupKey) o;
        return alertId == groupKey.alertId &&
                Objects.equals(namespace, groupKey.namespace) &&
                Arrays.equals(keys, groupKey.keys) &&
                Arrays.equals(values, groupKey.values);
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash(namespace, alertId);
        result = 31 * result + Arrays.hashCode(keys);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }

    @Override
    public String toString()
    {
        return "GroupKey{" +
                "namespace='" + namespace + '\'' +
                ", alertId=" + alertId +
                ", alertType=" + alertType +
                ", keys=" + Arrays.toString(keys) +
                ", values=" + Arrays.toString(values) +
                '}';
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private String namespace;

        private long alertId;

        private AlertType alertType;

        private String[] keys;

        private String[] values;

        public Builder setNamespace(String namespace)
        {
            this.namespace = namespace;
            return this;
        }

        public Builder setAlertId(long alertId)
        {
            this.alertId = alertId;
            return this;
        }

        public Builder setAlertType(AlertType alertType)
        {
            this.alertType = alertType;
            return this;
        }

        public Builder setKeys(String... keys)
        {
            this.keys = keys;
            return this;
        }

        public Builder setValues(String... values)
        {
            this.values = values;
            return this;
        }

        public GroupKey build()
        {
            return new GroupKey(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
