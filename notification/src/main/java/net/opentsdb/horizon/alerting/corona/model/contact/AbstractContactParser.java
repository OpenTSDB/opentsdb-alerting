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

package net.opentsdb.horizon.alerting.corona.model.contact;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import net.opentsdb.horizon.alerting.corona.model.AbstractParser;
import net.opentsdb.horizon.alerting.corona.model.Factory;

public abstract class AbstractContactParser<
        C extends AbstractContact,
        B extends AbstractContact.Builder<C, ?>
        >
        extends AbstractParser<C>
{

    /* ------------ Constants ------------ */

    private static final String F_ID = "id";
    private static final String F_NAME = "name";

    /* ------------ Fields ------------ */

    private final Factory<B> builderFactory;

    /* ------------ Constructors ------------ */

    protected AbstractContactParser(final Factory<B> builderFactory)
    {
        Objects.requireNonNull(builderFactory, "builderFactory cannot be null");
        this.builderFactory = builderFactory;
    }

    /* ------------ Abstract Methods ------------ */

    /**
     * Parse the given node and update the builder.
     *
     * @param builder builder of the concrete type
     * @param key     field key
     * @param node    field value
     */
    protected abstract void doParse(B builder, String key, JsonNode node);

    /* ------------ Methods ------------ */

    @Override
    public C doParse(final JsonNode root)
    {
        final B builder = builderFactory.create();

        root.fields().forEachRemaining(e -> {
            final String key = e.getKey().toLowerCase();
            final JsonNode val = e.getValue();

            switch (key) {
                case F_ID:
                    builder.setId(val.asInt());
                    break;
                case F_NAME:
                    builder.setName(val.asText());
                    break;
                default:
                    doParse(builder, key, val);
            }
        });

        return builder.build();
    }
}
