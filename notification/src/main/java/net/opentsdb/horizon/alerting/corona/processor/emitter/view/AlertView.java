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

package net.opentsdb.horizon.alerting.corona.processor.emitter.view;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alert.State;

@Getter
public abstract class AlertView {

    /* ------------ Constants ------------ */

    private static final long[] EMPTY_LONGS = new long[]{};

    protected static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("EEE MMM d, yyyy hh:mm:ss a z");

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /* ------------ Fields ------------ */

    /**
     * type is the end transition
     *
     * @return alert type
     */
    private final ViewType type;

    /**
     * Alert namespace (might differ from metric namespace).
     *
     * @return alert namespace.
     */
    private final String namespace;

    /**
     * TODO: Move this to the subclasses which need this field.
     *
     * Timestamps in seconds for display values.
     *
     * @return timestamps in seconds.
     */
    private final long[] timestampsSec;

    /**
     * Alert timestamp in milliseconds.
     *
     * @return alert timestamp in milliseconds.
     */
    private final long timestampMs;

    /**
     * State transition: from. E.g., "bad".
     *
     * @return `from` state representation.
     */
    private final String stateFrom;

    /**
     * State transition: to. E.g., "good".
     *
     * @return `to` state representation.
     */
    private final String stateTo;

    /**
     * Is alert snoozed?
     *
     * @return true if the alert is snoozed.
     */
    private final boolean isSnoozed;

    /**
     * Is nag alert?
     *
     * @return true if the alert is a nag.
     */
    private final boolean isNag;

    /**
     * Sorted alert tags.
     *
     * @return sorted tag map.
     */
    private final SortedMap<String, String> sortedTags;

    /**
     * Emitter specific properties.
     * <p>
     * E.g., for email that would be image "cid" -- content id.
     *
     * @return extra properties map. Can be modified.
     */
    private Map<String, Object> properties;

    /* ------------ Constructors ------------ */

    public AlertView(final Builder<?> builder)
    {
        Objects.requireNonNull(builder.type, "type cannot be null");
        this.type = builder.type;
        this.namespace = builder.namespace;
        this.timestampsSec = builder.timestampsSec == null ?
                EMPTY_LONGS : builder.timestampsSec;
        this.timestampMs = builder.timestampMs;
        this.stateFrom = builder.stateFrom;
        this.stateTo = builder.stateTo;
        this.isSnoozed = builder.isSnoozed;
        this.isNag = builder.isNag;
        this.sortedTags = Collections.unmodifiableSortedMap(builder.sortedTags);
        this.properties = builder.properties;
    }

    public AlertView() {
        // TODO: Many of the fields in this class (and subclasses) duplicate
        //       the fields of the original alert. It looks like it is pretty
        //       redundant and not needed. Most of the view methods are called
        //       only once, so the values can be computed at the call time.
        //       It makes sense to keep the interface, but switch to a lazy
        //       evaluation. EventAlertView is the first such approach.
        this.type = ViewType.BAD;
        this.namespace = null;
        this.timestampsSec = null;
        this.timestampMs = 0;
        this.stateFrom = null;
        this.stateTo = null;
        this.isSnoozed = false;
        this.isNag = false;
        this.sortedTags = null;
        this.properties = null;
    }

    /* ------------ Abstract Methods ------------ */

    public abstract String getDescription(final String emphasisStart,
                                          final String emphasisStop);

    /* ------------ Methods ------------ */

    public String getDescription()
    {
        return getDescription("", "");
    }

    public boolean showGraph()
    {
        return !Views.of(State.MISSING).equals(getStateTo());
    }

    public String getHumanTimestamp()
    {
        return DATE_FORMAT.format(new Date(getTimestampMs()));
    }

    public boolean isRecovery()
    {
        return Views.of(State.GOOD).equals(getStateTo());
    }

    public void addProperty(final String key,
                            final Object value)
    {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
    }

    public void addProperties(final Map<String, Object> properties)
    {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        this.properties.putAll(properties);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AlertView that = (AlertView) o;
        return timestampMs == that.timestampMs &&
                Objects.equals(namespace, that.namespace) &&
                Objects.equals(stateFrom, that.stateFrom) &&
                Objects.equals(stateTo, that.stateTo) &&
                isSnoozed == that.isSnoozed &&
                Objects.equals(sortedTags, that.sortedTags) &&
                Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode()
    {
        int hash = Arrays.hashCode(timestampsSec);
        return hash * 31 +
                Objects.hash(
                        namespace,
                        timestampMs,
                        stateFrom,
                        stateTo,
                        isSnoozed,
                        sortedTags,
                        properties
                );
    }

    @Override
    public String toString()
    {
        return "namespace='" + namespace + '\'' +
                ", timestampsSec=" + Arrays.toString(timestampsSec) +
                ", timestampMs=" + timestampMs +
                ", stateFrom='" + stateFrom + '\'' +
                ", stateTo='" + stateTo + '\'' +
                ", isSnoozed=" + isSnoozed +
                ", sortedTags=" + sortedTags +
                ", properties=" + properties;
    }

    /* ------------ Builder ------------ */

    protected abstract static class Builder<B extends Builder<B>> {

        private ViewType type;

        private String namespace;

        private long[] timestampsSec;

        private long timestampMs = Long.MIN_VALUE;

        private String stateFrom;

        private String stateTo;

        private boolean isSnoozed;

        private boolean isNag;

        private SortedMap<String, String> sortedTags;

        private Map<String, Object> properties;

        protected abstract B self();

        public B setType(final ViewType type)
        {
            this.type = type;
            return self();
        }

        public B setNamespace(final String namespace)
        {
            this.namespace = namespace;
            return self();
        }

        public B setTimestampsSec(final long... timestampsSec)
        {
            this.timestampsSec = timestampsSec;
            return self();
        }

        public B setTimestampMs(final long timestampMs)
        {
            this.timestampMs = timestampMs;
            return self();
        }

        public B setStateFrom(final String stateFrom)
        {
            this.stateFrom = stateFrom;
            return self();
        }

        public B setStateTo(final String stateTo)
        {
            this.stateTo = stateTo;
            return self();
        }

        public B setIsSnoozed(final boolean isSnoozed)
        {
            this.isSnoozed = isSnoozed;
            return self();
        }

        public B setIsNag(final boolean isNag)
        {
            this.isNag = isNag;
            return self();
        }

        public B setTagsAndSort(final Map<String, String> tags)
        {
            // TODO: This might be expensive.
            this.sortedTags = new TreeMap<>(tags);
            return self();
        }

        public B setProperties(final Map<String, Object> properties)
        {
            this.properties = properties;
            return self();
        }

        public B addProperty(final String key, final Object value)
        {
            if (properties == null) {
                properties = new HashMap<>();
            }
            properties.put(key, value);
            return self();
        }

        /**
         * Reset builder fields to defaults.
         *
         * @return builder.
         */
        public B reset()
        {
            namespace = null;
            timestampsSec = null;
            timestampMs = Long.MIN_VALUE;
            stateFrom = null;
            stateTo = null;
            isSnoozed = false;
            isNag = false;
            sortedTags = null;
            properties = null;
            type = ViewType.BAD;

            return self();
        }

    }
}
