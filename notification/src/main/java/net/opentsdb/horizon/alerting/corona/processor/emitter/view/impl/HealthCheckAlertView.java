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

package net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl;

import java.util.Arrays;
import java.util.Objects;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;

@Getter
public class HealthCheckAlertView extends AlertView {

    /* ------------ Fields ------------ */

    private final String dataNamespace;

    private final String application;

    private final State[] states;

    private final int threshold;

    private final boolean isMissingRecovery;

    private final int missingIntervalSec;

    private final String statusMessage;

    /* ------------ Constructors ------------ */

    public HealthCheckAlertView(final Builder<?> builder)
    {
        super(builder);
        this.dataNamespace = builder.dataNamespace;
        this.application = builder.application;
        this.states = builder.states;
        this.threshold = builder.threshold;
        this.isMissingRecovery = builder.isMissingRecovery;
        this.missingIntervalSec = builder.missingIntervalSec;
        this.statusMessage = builder.statusMessage;
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean showGraph()
    {
        return super.showGraph() && !isMissingRecovery;
    }

    @Override
    public String getDescription(final String emphasisStart,
                                 final String emphasisStop)
    {
        if (Views.of(State.MISSING).equals(getStateTo())) {
            return String.format(
                    "%s%s: %s%s. No data received for at least %s%d minutes.%s",
                    emphasisStart,
                    dataNamespace,
                    application,
                    emphasisStop,
                    emphasisStart,
                    missingIntervalSec / 60,
                    emphasisStop
            );
        } else if (isMissingRecovery) {
            return String.format(
                    "%s%s: %s%s has recovered from missing. " +
                            "Data had been missing for at least %s%d minutes.%s",
                    emphasisStart,
                    dataNamespace,
                    application,
                    emphasisStop,
                    emphasisStart,
                    missingIntervalSec / 60,
                    emphasisStop
            );
        } else {
            return String.format(
                    "%s%s: %s%s was in the %s%s%s state for at least %s%d times.%s",
                    emphasisStart,
                    dataNamespace,
                    application,
                    emphasisStop,
                    emphasisStart,
                    getStateTo(),
                    emphasisStop,
                    emphasisStart,
                    threshold,
                    emphasisStop
            );
        }
    }

    @Override
    public boolean equals(final Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        HealthCheckAlertView that = (HealthCheckAlertView) o;
        return threshold == that.threshold &&
                isMissingRecovery == that.isMissingRecovery &&
                missingIntervalSec == that.missingIntervalSec &&
                Objects.equals(dataNamespace, that.dataNamespace) &&
                Objects.equals(application, that.application) &&
                Arrays.equals(states, that.states) &&
                Objects.equals(statusMessage, that.statusMessage);
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash(
                super.hashCode(),
                dataNamespace,
                application,
                threshold,
                isMissingRecovery,
                missingIntervalSec,
                statusMessage
        );
        result = 31 * result + Arrays.hashCode(states);
        return result;
    }

    @Override
    public String toString()
    {
        return "HealthCheckAlertView{" +
                super.toString() +
                "dataNamespace='" + dataNamespace + '\'' +
                ", application='" + application + '\'' +
                ", states=" + Arrays.toString(states) +
                ", threshold=" + threshold +
                ", isMissingRecovery=" + isMissingRecovery +
                ", missingIntervalSec=" + missingIntervalSec +
                ", statusMessage='" + statusMessage + '\'' +
                '}';
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends AlertView.Builder<B>
    {

        private String dataNamespace;

        private String application;

        private State[] states;

        private int threshold;

        private boolean isMissingRecovery;

        private int missingIntervalSec;

        private String statusMessage;

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

        public B setStates(final State[] states)
        {
            this.states = states;
            return self();
        }

        public B setThreshold(final int threshold)
        {
            this.threshold = threshold;
            return self();
        }

        public B setIsMissingRecovery(final boolean isAutoRecovery)
        {
            this.isMissingRecovery = isAutoRecovery;
            return self();
        }

        public B setMissingIntervalSec(final int missingIntervalSec)
        {
            this.missingIntervalSec = missingIntervalSec;
            return self();
        }

        public B setStatusMessage(final String statusMessage)
        {
            this.statusMessage = statusMessage;
            return self();
        }

        @Override
        public B reset()
        {
            super.reset();
            dataNamespace = null;
            application = null;
            states = null;
            threshold = 0;
            isMissingRecovery = false;
            missingIntervalSec = 0;
            return self();
        }

        public HealthCheckAlertView build()
        {
            return new HealthCheckAlertView(this);
        }
    }

    private static final class BuilderImpl extends Builder<BuilderImpl> {

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
