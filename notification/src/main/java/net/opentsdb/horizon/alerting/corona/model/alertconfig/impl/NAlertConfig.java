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

package net.opentsdb.horizon.alerting.corona.model.alertconfig.impl;

import java.util.Objects;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alertconfig.AbstractAlertConfig;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.Notification;

/**
 * Alert configuration with notification field parsed.
 */
public class NAlertConfig extends AbstractAlertConfig {

    /* ------------ Fields ------------ */

    @Getter
    private final Notification notification;

    /* ------------ Constructors ------------ */

    public NAlertConfig(final AbstractBuilder<?, ?> builder)
    {
        super(builder);
        Objects.requireNonNull(builder.notification,
                "notification cannot be null");
        this.notification = builder.notification;
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        NAlertConfig that = (NAlertConfig) o;
        return Objects.equals(notification, that.notification);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), notification);
    }

    @Override
    public String toString()
    {
        return "NAlertConfig{" +
                super.toString() +
                ", notification=" + notification +
                '}';
    }

    /* ------------ Builder ------------ */

    protected abstract static
    class AbstractBuilder<C extends NAlertConfig, B extends AbstractBuilder<C, B>>
            extends AbstractAlertConfig.Builder<C, B>
    {

        private Notification notification;

        public B setNotification(final Notification notification)
        {
            this.notification = notification;
            return self();
        }
    }

    public abstract static class Builder<B extends Builder<B>>
            extends AbstractBuilder<NAlertConfig, B>
    {
        @Override
        public NAlertConfig build()
        {
            return new NAlertConfig(this);
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
