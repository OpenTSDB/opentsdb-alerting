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

package net.opentsdb.horizon.alerts.model.tsdb;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.ToString;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

@ToString
@Getter
public class Datum implements KryoSerializable {

    private String cluster = "";
    private String application = "";
    private Long timestamp = 0l;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    private Map<String, IMetric> metrics = new HashMap<>();
    private Tags tags = new Tags();
    private String status_msg = null;
    private String status_code = null;

    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Datum() {

    }

    public Datum(Builder builder) {
        this.cluster = builder.cluster;
        this.application = builder.application;
        this.additionalProperties = builder.additionalProperties;
        this.metrics = builder.metrics;
        this.tags = builder.tags;
        this.status_msg = builder.status_msg;
        this.status_code = builder.status_code;
        this.timestamp = builder.timestamp;
        this.timeUnit = builder.timeUnit;
    }


    @Override
    public boolean equals(Object o) {
        if (o instanceof Datum) {
            Datum d = (Datum) o;

            if (this.cluster.equals(d.cluster)
                    && this.application.equals(d.application)
                    && this.timestamp.equals(d.timestamp)
                    && this.metrics.equals(d.metrics)
                    && this.tags.equals(d.tags)
                    && this.additionalProperties.equals(d.additionalProperties)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void write(Kryo kryo, Output output) {
        output.writeString(cluster);
        output.writeString(application);
        output.writeLong(timestamp);
        output.writeString(status_code);
        output.writeString(status_msg);

        output.writeShort(metrics.size());
        for (Entry<String, IMetric> entry : metrics.entrySet()) {
            output.writeString(entry.getKey());
            kryoSerializeIMetric(kryo, entry.getValue(), output);
        }


        output.writeByte(tags.getDimensions().size());
        for (Entry<String, String> entry : tags.getDimensions().entrySet()) {
            output.writeString(entry.getKey());
            output.writeString(entry.getValue());
        }
    }

    public static void kryoSerializeIMetric(Kryo kryo, IMetric value, Output output) {
        if (value instanceof Metric) {
            output.writeByte(0);
        } else {
            throw new AssertionError("Unrecognized Metric type " + value.getClass());
        }

        value.write(kryo, output);
    }

    @Override
    public void read(Kryo kryo, Input input) {
        cluster = input.readString();
        application = input.readString();
        timestamp = input.readLong();
        status_code = input.readString();
        status_msg = input.readString();

        int size = input.readShort();

        metrics = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String metricName = input.readString();
            metrics.put(metricName, new Metric(input.readDouble()));
        }

        size = input.readByte();
        Map<String, String> tagMap = new HashMap<>(size);

        for (int i = 0; i < size; i++) {
            tagMap.put(input.readString(), input.readString());
        }
        tags = new Tags(tagMap);
    }


    public void readNew(Kryo kryo, Input input) {
        cluster = input.readString();
        application = input.readString();
        timestamp = input.readLong();
        status_code = input.readString();
        status_msg = input.readString();

        int size = input.readShort();
        metrics = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String metricName = input.readString();
            metrics.put(metricName, kryoDeserializeIMetric(kryo, input));
        }

        size = input.readByte();
        Map<String, String> tagMap = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            tagMap.put(input.readString(), input.readString());
        }
        tags = new Tags(tagMap);
    }

    public static IMetric kryoDeserializeIMetric(Kryo kryo, Input input) {
        IMetric metric;
        byte b = input.readByte();
        if (b == 0) {
            metric = new Metric();
        } else {
            throw new AssertionError("Unrecognized Metric type " + b);
        }
        metric.read(kryo, input);
        return metric;
    }

    public static class Builder {

        private String cluster = "";
        private String application = "";
        private Long timestamp = 0l;

        private Map<String, IMetric> metrics = new HashMap<>();
        private Tags tags = new Tags();
        private String status_msg = null;
        private String status_code = null;
        private TimeUnit timeUnit = TimeUnit.SECONDS;

        private Map<String, Object> additionalProperties = new HashMap<String, Object>();

        public Builder withCluster(String cluster) {
            this.cluster = cluster;
            return this;
        }

        public Builder withApplication(String application) {
            this.application = application;
            return this;
        }

        public Builder withTimestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withMetrics(Map<String, IMetric> metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder withTags(Tags tags) {
            this.tags = tags;
            return this;
        }

        public Builder withStatus_msg(String status_msg) {
            this.status_msg = status_msg;
            return this;
        }

        public Builder withStatus_code(String status_code) {
            this.status_code = status_code;
            return this;
        }

        public Builder withAdditionalProperties(Map<String, Object> additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }

        public Builder withTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        public Datum build() {
            return new Datum(this);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
}
