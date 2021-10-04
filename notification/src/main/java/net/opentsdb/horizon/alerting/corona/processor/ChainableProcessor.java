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

package net.opentsdb.horizon.alerting.corona.processor;

import java.util.Objects;

public abstract class ChainableProcessor<I, O>
        implements Processor<I>
{

    private final Processor<O> next;

    protected ChainableProcessor(final Processor<O> next)
    {
        Objects.requireNonNull(next, "next cannot be null");
        this.next = next;
    }

    protected ChainableProcessor(final Builder<O, ?> builder)
    {
        Objects.requireNonNull(builder, "builder cannot be null");
        Objects.requireNonNull(builder.next, "next cannot be null");
        this.next = builder.next;
    }

    protected void submit(O inputForNext)
    {
        next.process(inputForNext);
    }

    /* ------------ Builder ------------ */

    protected abstract static class Builder<O, B extends Builder<O, B>> {

        private Processor<O> next;

        protected abstract B self();

        public B setNext(final Processor<O> next)
        {
            this.next = next;
            return self();
        }
    }
}
