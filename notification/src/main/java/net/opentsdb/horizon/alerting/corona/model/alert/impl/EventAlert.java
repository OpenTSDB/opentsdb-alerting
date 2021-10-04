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

package net.opentsdb.horizon.alerting.corona.model.alert.impl;

import java.util.Objects;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alert.Event;

@Getter
public class EventAlert extends Alert {

    /* ------------ Fields ------------ */

    private String dataNamespace;

    private String filterQuery;

    private int threshold;

    private int windowSizeSec;

    private int count;

    private Event event;

    /* ------------ Constructors ------------ */

    private EventAlert(final Builder<?> builder)
    {
        super(builder);
        Objects.requireNonNull(builder.dataNamespace,
                "dataNamespace cannot be null");
        Objects.requireNonNull(builder.filterQuery,
                "filterQuery cannot be null");
        this.dataNamespace = builder.dataNamespace;
        this.filterQuery = builder.filterQuery;
        this.threshold = builder.threshold;
        this.windowSizeSec = builder.windowSizeSec;
        this.count = builder.count;
        this.event = builder.event;
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean equals(final Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        EventAlert that = (EventAlert) o;
        return threshold == that.threshold &&
                windowSizeSec == that.windowSizeSec &&
                count == that.count &&
                Objects.equals(dataNamespace, that.dataNamespace) &&
                Objects.equals(filterQuery, that.filterQuery) &&
                Objects.equals(event, that.event);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(
                super.hashCode(),
                dataNamespace,
                filterQuery,
                threshold,
                windowSizeSec,
                count,
                event
        );
    }

    @Override
    public String toString()
    {
        return "EventAlert{" +
                super.toString() +
                ", dataNamespace='" + dataNamespace + '\'' +
                ", filterQuery='" + filterQuery + '\'' +
                ", threshold=" + threshold +
                ", windowSizeSec=" + windowSizeSec +
                ", count=" + count +
                ", event=" + event +
                '}';
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends Alert.Builder<EventAlert, B>
    {

        private String dataNamespace;

        private String filterQuery;

        private int threshold;

        private int windowSizeSec;

        private int count;

        private Event event;

        public Builder()
        {
            super(AlertType.EVENT);
        }

        public B setDataNamespace(final String dataNamespace)
        {
            this.dataNamespace = dataNamespace;
            return self();
        }

        public B setFilterQuery(final String filterQuery)
        {
            this.filterQuery = filterQuery;
            return self();
        }

        public B setThreshold(final int threshold)
        {
            this.threshold = threshold;
            return self();
        }

        public B setWindowSizeSec(final int windowSizeSec)
        {
            this.windowSizeSec = windowSizeSec;
            return self();
        }

        public B setCount(final int count)
        {
            this.count = count;
            return self();
        }

        public B setEvent(final Event event)
        {
            this.event = event;
            return self();
        }
    }

    private static class BuilderImpl extends Builder<BuilderImpl> {

        @Override
        protected BuilderImpl self()
        {
            return this;
        }

        @Override
        public EventAlert build()
        {
            return new EventAlert(this);
        }
    }

    public static Builder<?> builder()
    {
        return new BuilderImpl();
    }
}
