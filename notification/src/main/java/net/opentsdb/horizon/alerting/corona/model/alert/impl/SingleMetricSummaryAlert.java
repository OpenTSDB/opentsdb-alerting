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

import net.opentsdb.horizon.alerting.corona.model.alert.Summary;

public final class SingleMetricSummaryAlert extends SingleMetricAlert {

    /* ------------ Fields ------------ */

    @Getter
    private final double[] summaryValues;

    @Getter
    private final Summary summary;

    /* ------------ Constructors ------------ */

    public SingleMetricSummaryAlert(Builder<?> builder)
    {
        super(builder);
        this.summaryValues = builder.summaryValues;
        this.summary = builder.summary;
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        SingleMetricSummaryAlert that = (SingleMetricSummaryAlert) o;
        return Arrays.equals(summaryValues, that.summaryValues) &&
                summary == that.summary;
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash(super.hashCode(), summary);
        result = 31 * result + Arrays.hashCode(summaryValues);
        return result;
    }

    @Override
    public String toString()
    {
        return "SingleMetricSummaryAlert{" +
                super.toString() +
                ", summaryValues=" + Arrays.toString(summaryValues) +
                ", summary=" + summary +
                '}';
    }

    /* ------------ Builder ------------ */

    public static abstract class Builder<B extends Builder<B>>
            extends SingleMetricAlert.Builder<SingleMetricSummaryAlert, B>
    {

        private double[] summaryValues;

        private Summary summary;

        private Builder()
        {
            super();
        }

        public B setSummaryValues(final double[] summaryValues)
        {
            this.summaryValues = summaryValues;
            return self();
        }

        public B setSummary(final Summary summary)
        {
            this.summary = summary;
            return self();
        }

        @Override
        public SingleMetricSummaryAlert build()
        {
            return new SingleMetricSummaryAlert(this);
        }
    }

    private static class BuilderImpl extends Builder<BuilderImpl> {

        private BuilderImpl()
        {
            super();
        }

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
