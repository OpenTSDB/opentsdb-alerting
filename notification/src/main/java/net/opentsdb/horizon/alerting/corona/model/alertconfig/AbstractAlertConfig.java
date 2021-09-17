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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

public abstract class AbstractAlertConfig {

    public enum Type {
        SIMPLE,
        HEALTHCHECK,
        EVENT
    }

    @Getter
    private final long id;

    @Getter
    private final String name;

    @Getter
    private final String namespace;

    @Getter
    private final Type type;

    @Getter
    private final boolean enabled;

    @Getter
    private final List<String> labels;

    @Getter
    private final List<String> groupingRules;

    protected AbstractAlertConfig(final Builder<?, ?> builder)
    {
        Objects.requireNonNull(builder, "builder cannot be bull");
        // TODO: Decide what to check.
        this.id = builder.id;
        this.name = builder.name;
        this.namespace = builder.namespace;
        this.type = builder.type;
        this.enabled = builder.enabled;
        this.labels = builder.labels == null ?
                Collections.emptyList() : builder.labels;
        this.groupingRules = builder.groupingRules == null ?
                Collections.emptyList() : builder.groupingRules;
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
        AbstractAlertConfig that = (AbstractAlertConfig) o;
        return id == that.id &&
                enabled == that.enabled &&
                Objects.equals(name, that.name) &&
                Objects.equals(namespace, that.namespace) &&
                type == that.type &&
                Objects.equals(labels, that.labels) &&
                Objects.equals(groupingRules, that.groupingRules);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(
                id,
                name,
                namespace,
                type,
                enabled,
                labels,
                groupingRules
        );
    }

    /**
     * Subclasses should override this method and call it as part of the
     * final representation construction.
     *
     * @return partial object representation
     */
    @Override
    public String toString()
    {
        return "id=" + id +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", type=" + type +
                ", enabled=" + enabled +
                ", labels=" + labels +
                ", groupingRules=" + groupingRules;
    }

    /* ------------ Builder ------------ */

    protected abstract static
    class Builder<C extends AbstractAlertConfig, B extends Builder<C, B>> {

        /* ------------ Fields ------------ */

        private long id;

        private String name;

        private String namespace;

        private Type type;

        private boolean enabled;

        private List<String> labels;

        private List<String> groupingRules;

        /* ------------ Abstract Methods ------------ */

        protected abstract B self();

        protected abstract C build();

        /* ------------ Methods ------------ */

        public B setId(long id)
        {
            this.id = id;
            return self();
        }

        public B setName(String name)
        {
            this.name = name;
            return self();
        }

        public B setNamespace(String namespace)
        {
            this.namespace = namespace;
            return self();
        }

        public B setType(Type type)
        {
            this.type = type;
            return self();
        }

        public B setEnabled(boolean enabled)
        {
            this.enabled = enabled;
            return self();
        }

        public B setLabels(List<String> labels)
        {
            this.labels = labels;
            return self();
        }

        public B setGroupingRules(List<String> groupingRules)
        {
            this.groupingRules = groupingRules;
            return self();
        }
    }
}
