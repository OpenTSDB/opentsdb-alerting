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

package net.opentsdb.horizon.alerting.corona.processor.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.opentsdb.horizon.alerting.corona.processor.Processor;

public class Fork<T> implements Processor<T> {

    private final List<Processor<T>> processors;

    private Fork(final Builder<T> builder)
    {
        Objects.requireNonNull(builder, "builder cannot be null");
        Objects.requireNonNull(builder.processors, "processor cannot be null");
        if (builder.processors.size() == 0) {
            throw new IllegalArgumentException(
                    "At least one processor is required");
        }
        this.processors = builder.processors;
    }

    @Override
    public void process(T item)
    {
        processors.forEach(p -> p.process(item));
    }

    /* ------------ Builder ------------ */

    public static class Builder<T> {

        private final List<Processor<T>> processors;

        private Builder()
        {
            this.processors = new ArrayList<>();
        }

        public Builder<T> addProcessor(Processor<T> processor)
        {
            processors.add(processor);
            return this;
        }

        public Fork<T> build()
        {
            return new Fork<>(this);
        }
    }

    public static <T> Builder<T> builder()
    {
        return new Builder<>();
    }
}
