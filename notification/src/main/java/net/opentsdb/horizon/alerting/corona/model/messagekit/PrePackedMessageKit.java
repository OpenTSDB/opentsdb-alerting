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

import java.util.Arrays;

import lombok.Getter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class PrePackedMessageKit extends AbstractMessageKit {

    @Getter
    private final byte[] alertGroupBytes;

    private PrePackedMessageKit(final Builder<?> builder)
    {
        super(builder);
        this.alertGroupBytes = builder.serializedAlertGroup;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        PrePackedMessageKit that = (PrePackedMessageKit) o;
        return Arrays.equals(alertGroupBytes, that.alertGroupBytes);
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Arrays.hashCode(alertGroupBytes);
    }

    @Override
    public String toString()
    {
        return "PrePackedMessageKit{" +
                super.toString() +
                ", alertGroupBytes(length)=" + alertGroupBytes.length +
                '}';
    }

    public abstract static class Builder<B extends Builder<B>>
            extends AbstractMessageKit.Builder<B>
    {

        private byte[] serializedAlertGroup;


        @SuppressFBWarnings(
                value = {"EI2"},
                justification = "Copying is redundant"
        )
        public B setSerializedAlertGroup(final byte[] serializedAlertGroup)
        {
            this.serializedAlertGroup = serializedAlertGroup;
            return self();
        }

        public PrePackedMessageKit build()
        {
            return new PrePackedMessageKit(this);
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
