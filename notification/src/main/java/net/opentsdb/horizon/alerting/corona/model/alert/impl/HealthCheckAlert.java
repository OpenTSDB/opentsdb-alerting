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

import java.util.Arrays;
import java.util.Objects;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;

@Getter
public class HealthCheckAlert extends Alert {

    /* ------------ Constants ------------ */

    private static final State[] EMPTY_STATES = new State[]{};

    private static final long[] EMPTY_TIMESTAMPS = new long[]{};

    /* ------------ Fields ------------ */

    private final String dataNamespace;

    private final String application;

    private final State[] states;

    private final long[] timestampsSec;

    private final int threshold;

    private final boolean isMissingRecovery;

    private final int missingIntervalSec;

    /* ------------ Constructors ------------ */

    public HealthCheckAlert(final Builder<?> builder)
    {
        super(builder);
        this.dataNamespace = builder.dataNamespace;
        this.application = builder.application;
        this.states = builder.states == null ?
                EMPTY_STATES : builder.states;
        this.timestampsSec = builder.timestampsSec == null ?
                EMPTY_TIMESTAMPS : builder.timestampsSec;
        if (states.length != timestampsSec.length) {
            throw new IllegalArgumentException(
                    "states.length != timestampsSec.length");
        }
        this.threshold = builder.threshold;
        this.isMissingRecovery = builder.isMissingRecovery;
        this.missingIntervalSec = builder.missingIntervalSec;
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean equals(final Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        HealthCheckAlert that = (HealthCheckAlert) o;
        return threshold == that.threshold &&
                isMissingRecovery == that.isMissingRecovery &&
                Objects.equals(dataNamespace, that.dataNamespace) &&
                Objects.equals(application, that.application) &&
                Arrays.equals(states, that.states) &&
                Arrays.equals(timestampsSec, that.timestampsSec) &&
                missingIntervalSec == that.missingIntervalSec;
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash(super.hashCode(),
                dataNamespace,
                application,
                threshold,
                isMissingRecovery,
                missingIntervalSec);
        result = 31 * result + Arrays.hashCode(states);
        result = 31 * result + Arrays.hashCode(timestampsSec);
        return result;
    }

    @Override
    public String toString()
    {
        return "HealthCheckAlert{" +
                super.toString() +
                ", dataNamespace='" + dataNamespace + '\'' +
                ", application='" + application + '\'' +
                ", states=" + Arrays.toString(states) +
                ", timestampsSec=" + Arrays.toString(timestampsSec) +
                ", threshold=" + threshold +
                ", isMissingRecovery=" + isMissingRecovery +
                ", missingIntervalSec=" + missingIntervalSec +
                '}';
    }

    /* ------------ Builder ------------ */

    public static abstract class Builder<B extends Builder<B>>
            extends Alert.Builder<HealthCheckAlert, B>
    {

        private String dataNamespace;

        private String application;

        private State[] states;

        private long[] timestampsSec;

        private int threshold;

        private boolean isMissingRecovery;

        private int missingIntervalSec;

        public Builder()
        {
            super(AlertType.HEALTH_CHECK);
        }

        public B setDataNamespace(final String dataNamespace)
        {
            this.dataNamespace = dataNamespace;
            return self();
        }

        public B setApplication(final String application)
        {
            this.application = application;
            return self();
        }

        public B setStates(final State... states)
        {
            this.states = states;
            return self();
        }

        public B setTimestampsSec(final long[] timestampsSec)
        {
            this.timestampsSec = timestampsSec;
            return self();
        }

        public B setThreshold(final int threshold)
        {
            this.threshold = threshold;
            return self();
        }

        public B setMissingRecovery(final boolean missingRecovery)
        {
            this.isMissingRecovery = missingRecovery;
            return self();
        }

        public B setMissingIntervalSec(final int missingIntervalSec)
        {
            this.missingIntervalSec = missingIntervalSec;
            return self();
        }

        @Override
        public HealthCheckAlert build()
        {
            return new HealthCheckAlert(this);
        }
    }

    private static class BuilderImpl extends Builder<BuilderImpl> {

        @Override
        protected BuilderImpl self()
        {
            return this;
        }
    }

    public static Builder<?> builder()
    {
        return new BuilderImpl();
    }

}
