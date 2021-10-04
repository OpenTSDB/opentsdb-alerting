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

package net.opentsdb.horizon.alerting.corona.model.alertconfig;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import net.opentsdb.horizon.alerting.corona.model.Factory;
import net.opentsdb.horizon.alerting.corona.model.AbstractParser;

public class RecipientParser extends AbstractParser<Recipient> {

    private static final Logger LOG =
            LoggerFactory.getLogger(RecipientParser.class);

    private static final String F_ID = "id";
    private static final String F_NAME = "name";

    private final Factory<Recipient.Builder> builderFactory;

    public RecipientParser(Factory<Recipient.Builder> builderFactory) {
        Objects.requireNonNull(builderFactory, "builderFactory cannot be null");
        this.builderFactory = builderFactory;
    }

    @Override
    public Recipient doParse(JsonNode root) {
        LOG.trace("Parse recipient: content={}", root);

        final Recipient.Builder builder = builderFactory.create();

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
                    LOG.trace("Unknown field: name={}, value={}", key, val);
            }
        });

        return builder.build();
    }
}
