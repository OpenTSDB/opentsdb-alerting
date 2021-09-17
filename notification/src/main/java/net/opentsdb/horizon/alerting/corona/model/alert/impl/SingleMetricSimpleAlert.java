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

public final class SingleMetricSimpleAlert extends SingleMetricAlert {

    /* ------------ Constructor ------------ */

    protected SingleMetricSimpleAlert(Builder<?> builder)
    {
        super(builder);
    }

    /* ------------ Methods ------------ */

    @Override
    public String toString()
    {
        return "SingleMetricSimpleAlert{" +
                super.toString() +
                "}";
    }

    @Override
    public boolean equals(Object o)
    {
        return super.equals(o);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /* ------------ Builder ------------ */

    public static abstract class Builder<B extends Builder<B>>
            extends SingleMetricAlert.Builder<SingleMetricSimpleAlert, B>
    {
        protected Builder()
        {
            super();
        }
    }

    private static class BuilderImpl extends Builder<BuilderImpl> {

        @Override
        protected BuilderImpl self()
        {
            return this;
        }

        @Override
        public SingleMetricSimpleAlert build()
        {
            return new SingleMetricSimpleAlert(this);
        }
    }

    public static Builder<?> builder()
    {
        return new BuilderImpl();
    }
}
