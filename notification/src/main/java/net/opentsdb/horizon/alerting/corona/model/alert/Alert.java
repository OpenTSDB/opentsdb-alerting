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

package net.opentsdb.horizon.alerting.corona.model.alert;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;

public abstract class Alert {

    @Getter
    private final AlertType type;

    @Getter
    private final long id;

    @Getter
    private final String namespace;

    @Getter
    private final long timestampSec;

    @Getter
    private final Map<String, String> tags;

    @Getter
    private final Map<String, String> properties;

    @Getter
    private final State stateFrom;

    @Getter
    private final State state;

    @Getter
    private final String details;

    @Getter
    private final boolean isNag;

    @Getter
    private final boolean isSnoozed;

    protected Alert(final Builder<?, ?> builder)
    {
        Objects.requireNonNull(builder.type, "type cannot be null");
        Objects.requireNonNull(builder.namespace, "namespace cannot be null");
        Objects.requireNonNull(builder.stateFrom, "stateFrom cannot be null");
        Objects.requireNonNull(builder.state, "state cannot be null");
        if (builder.timestampSec <= 0) {
            throw new IllegalArgumentException("timestampSec cannot be <= 0");
        }

        this.type = builder.type;
        this.id = builder.id;
        this.namespace = builder.namespace;
        this.timestampSec = builder.timestampSec;
        this.tags = builder.tags == null ?
                Collections.emptyMap() : builder.tags;
        this.properties = builder.properties == null ?
                Collections.emptyMap() : builder.properties;
        this.stateFrom = builder.stateFrom;
        this.state = builder.state;
        this.details = builder.details;
        this.isNag = builder.isNag;
        this.isSnoozed = builder.isSnoozed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Alert that = (Alert) o;
        return id == that.id &&
                timestampSec == that.timestampSec &&
                type == that.type &&
                Objects.equals(namespace, that.namespace) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(properties, that.properties) &&
                stateFrom == that.stateFrom &&
                state == that.state &&
                Objects.equals(details, that.details) &&
                isNag == that.isNag &&
                isSnoozed == that.isSnoozed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(
                type,
                id,
                namespace,
                timestampSec,
                tags,
                properties,
                stateFrom,
                state,
                details,
                isNag,
                isSnoozed
        );
    }

    /**
     * Subclasses should embed call to this function in their implementations.
     *
     * <pre>{@code
     *     public String toString()
     *     {
     *         return "SingleMetricSimpleAlert{" +
     *                 super.toString() +
     *                 ", valuesInWindow=" + Arrays.toString(valuesInWindow) +
     *                 ", timestamps=" + Arrays.toString(timestamps) +
     *                 ", threshold=" + threshold +
     *                 ", comparator=" + comparator +
     *                 ", metric=" + metric +
     *                 '}';
     *     }
     * }</pre>
     *
     * @return non standard formatted string
     */
    @Override
    public String toString()
    {
        return "type=" + type +
                ", id=" + id +
                ", namespace='" + namespace + '\'' +
                ", timestampSec=" + timestampSec +
                ", tags=" + tags +
                ", properties=" + properties +
                ", stateFrom=" + stateFrom +
                ", state=" + state +
                ", details='" + details + '\'' +
                ", isNag=" + isNag +
                ", isSnoozed=" + isSnoozed;
    }

    /* ------------ Builder ------------ */

    protected abstract static class Builder<A extends Alert, B extends Builder<A, B>> {

        private final AlertType type;

        private long id;

        private String namespace;

        private long timestampSec;

        private Map<String, String> tags;

        private Map<String, String> properties;

        private State stateFrom;

        private State state;

        private String details;

        private boolean isNag;

        private boolean isSnoozed;

        public Builder(final AlertType type)
        {
            Objects.requireNonNull(type, "type cannot be null");
            this.type = type;
        }

        protected abstract B self();

        public abstract A build();

        public B setId(long id)
        {
            this.id = id;
            return self();
        }

        public B setNamespace(String namespace)
        {
            this.namespace = namespace;
            return self();
        }

        public B setTimestampSec(long timestampSec)
        {
            this.timestampSec = timestampSec;
            return self();
        }

        public B setTags(Map<String, String> tags)
        {
            if (this.tags != null) {
                throw new IllegalStateException("tags are already set");
            }
            this.tags = tags;
            return self();
        }

        public B addTag(String key, String val)
        {
            if (this.tags == null) {
                this.tags = new HashMap<>();
            }
            this.tags.put(key, val);
            return self();
        }

        public B setProperties(Map<String, String> properties)
        {
            if (this.properties != null) {
                throw new IllegalStateException("properties are already set");
            }
            this.properties = properties;
            return self();
        }

        public B addProperty(String key, String val)
        {
            if (this.properties == null) {
                this.properties = new HashMap<>();
            }
            this.properties.put(key, val);
            return self();
        }

        public B setStateFrom(State stateFrom)
        {
            this.stateFrom = stateFrom;
            return self();
        }

        public B setState(State state)
        {
            this.state = state;
            return self();
        }

        public B setDetails(String details)
        {
            this.details = details;
            return self();
        }

        public B setIsNag(boolean isNag)
        {
            this.isNag = isNag;
            return self();
        }

        public B setIsSnoozed(boolean isSnoozed)
        {
            this.isSnoozed = isSnoozed;
            return self();
        }
    }
}
