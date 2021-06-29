/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.query.egads;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.opentsdb.horizon.core.validate.Validate;

public final class DefaultEgadsResponse implements EgadsResponse {

    private final long startTimeSec;
    private final long endTimeSec;
    private final String metricName;
    private final List<EgadsDataItem> dataItems;

    private DefaultEgadsResponse(DefaultBuilder builder) {
        Validate.isTrue(builder.startTimeSec < builder.endTimeSec,
                "startTimeSec (%d) has to be before endTimeSec (%d)",
                builder.startTimeSec, builder.endTimeSec);
        Validate.paramNotNull(builder.dataItems, "dataItems");
        this.startTimeSec = builder.startTimeSec;
        this.endTimeSec = builder.endTimeSec;
        this.metricName = builder.metricName;
        this.dataItems = builder.dataItems;
    }

    @Override
    public long getStartTimeSec() {
        return startTimeSec;
    }

    @Override
    public long getEndTimeSec() {
        return endTimeSec;
    }

    @Override
    public Optional<String> getMetricName() {
        return Optional.ofNullable(metricName);
    }

    @Override
    public List<EgadsDataItem> getDataItems() {
        return dataItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultEgadsResponse that = (DefaultEgadsResponse) o;
        return startTimeSec == that.startTimeSec &&
                endTimeSec == that.endTimeSec &&
                Objects.equals(metricName, that.metricName) &&
                Objects.equals(dataItems, that.dataItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTimeSec, endTimeSec, metricName, dataItems);
    }

    @Override
    public String toString() {
        return "DefaultEgadsResponse{" +
                "startTimeSec=" + startTimeSec +
                ", endTimeSec=" + endTimeSec +
                ", metricName='" + metricName + '\'' +
                ", dataItems=" + dataItems +
                '}';
    }

    public interface Builder extends EgadsResponse.Builder<Builder> {

    }

    private static final class DefaultBuilder implements Builder {

        private long startTimeSec;
        private long endTimeSec;
        private String metricName;
        private List<EgadsDataItem> dataItems;

        private DefaultBuilder() {
        }

        @Override
        public Builder setStartTimeSec(long startTimeSec) {
            this.startTimeSec = startTimeSec;
            return this;
        }

        @Override
        public Builder setEndTimeSec(long endTimeSec) {
            this.endTimeSec = endTimeSec;
            return this;
        }

        @Override
        public Builder setMetricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        @Override
        public Builder setDataItems(List<EgadsDataItem> dataItems) {
            this.dataItems = dataItems;
            return this;
        }

        @Override
        public EgadsResponse build() {
            return new DefaultEgadsResponse(this);
        }
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }
}
