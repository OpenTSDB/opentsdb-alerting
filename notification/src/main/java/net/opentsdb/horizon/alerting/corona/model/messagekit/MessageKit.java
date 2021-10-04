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

package net.opentsdb.horizon.alerting.corona.model.messagekit;

import java.util.Objects;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;

public class MessageKit extends AbstractMessageKit {

    /* ------------ Fields ------------ */

    @Getter
    private final AlertGroup alertGroup;

    /* ------------ Constructor ------------ */

    protected MessageKit(final Builder<?> builder)
    {
        super(builder);
        Objects.requireNonNull(builder.alertGroup, "alertGroup cannot be null");
        this.alertGroup = builder.alertGroup;
    }

    /* ------------ Methods ------------ */

    public String getNamespace()
    {
        return alertGroup.getGroupKey().getNamespace();
    }

    public long getAlertId()
    {
        return alertGroup.getGroupKey().getAlertId();
    }

    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        MessageKit messageKit = (MessageKit) o;
        return Objects.equals(alertGroup, messageKit.alertGroup);
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode()
                + (alertGroup == null ? 0 : alertGroup.hashCode());
    }

    @Override
    public String toString()
    {
        return "MessageKit{" +
                super.toString() +
                ", alertGroup=" + alertGroup +
                '}';
    }

    /* ------------ Builder ------------ */

    // TODO: The `type` and `meta` should become type-safe.
    // It should not be possible to use EmailMeta `meta` with
    // Contact.ViewType.SLACK `type`.
    public abstract static class Builder<B extends Builder<B>>
            extends AbstractMessageKit.Builder<B>
    {

        private AlertGroup alertGroup;

        public B setAlertGroup(final AlertGroup alertGroup)
        {
            this.alertGroup = alertGroup;
            return self();
        }

        public MessageKit build()
        {
            return new MessageKit(this);
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
