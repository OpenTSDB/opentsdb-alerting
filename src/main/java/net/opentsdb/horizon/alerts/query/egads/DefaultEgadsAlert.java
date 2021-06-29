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

import java.util.Objects;

import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.enums.ThresholdType;

public final class DefaultEgadsAlert implements EgadsAlert {

    private final long timestampSec;
    private final AlertState alertState;
    private final String message;
    private final double observedValue;
    private final double thresholdValue;
    private final ThresholdType thresholdType;

    public DefaultEgadsAlert(DefaultBuilder builder) {
        this.timestampSec = builder.timestampSec;
        this.alertState = builder.alertState;
        this.message = builder.message;
        this.observedValue = builder.observedValue;
        this.thresholdValue = builder.thresholdValue;
        this.thresholdType = builder.thresholdType;
    }

    @Override
    public long getTimestampSec() {
        return timestampSec;
    }

    @Override
    public AlertState getAlertState() {
        return alertState;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public double getObservedValue() {
        return observedValue;
    }

    @Override
    public double getThresholdValue() {
        return thresholdValue;
    }

    @Override
    public ThresholdType getThresholdType() {
        return thresholdType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultEgadsAlert that = (DefaultEgadsAlert) o;
        return timestampSec == that.timestampSec &&
                Double.compare(that.observedValue, observedValue) == 0 &&
                Double.compare(that.thresholdValue, thresholdValue) == 0 &&
                alertState == that.alertState &&
                thresholdType == that.thresholdType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                timestampSec,
                alertState,
                observedValue,
                thresholdValue,
                thresholdType
        );
    }

    @Override
    public String toString() {
        return "DefaultEgadsAlert{" +
                "timestampSec=" + timestampSec +
                ", alertState=" + alertState +
                ", observedValue=" + observedValue +
                ", thresholdValue=" + thresholdValue +
                ", thresholdType=" + thresholdType +
                '}';
    }

    public interface Builder extends EgadsAlert.Builder<Builder> {

    }

    private static final class DefaultBuilder implements Builder {

        private long timestampSec;
        private AlertState alertState;
        private String message;
        private double observedValue;
        private double thresholdValue;
        private ThresholdType thresholdType;

        @Override
        public Builder setTimestampSec(long timestampSec) {
            this.timestampSec = timestampSec;
            return this;
        }

        @Override
        public Builder setAlertState(AlertState alertState) {
            this.alertState = alertState;
            return this;
        }

        @Override
        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Builder setObservedValue(double value) {
            this.observedValue = value;
            return this;
        }

        @Override
        public Builder setThresholdValue(double value) {
            this.thresholdValue = value;
            return this;
        }

        @Override
        public Builder setThresholdType(ThresholdType type) {
            this.thresholdType = type;
            return this;
        }

        @Override
        public EgadsAlert build() {
            return new DefaultEgadsAlert(this);
        }
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }
}
