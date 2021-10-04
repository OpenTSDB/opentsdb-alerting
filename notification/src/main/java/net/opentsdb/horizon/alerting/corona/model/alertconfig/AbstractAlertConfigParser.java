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

import com.fasterxml.jackson.databind.JsonNode;

import net.opentsdb.horizon.alerting.corona.model.AbstractParser;
import net.opentsdb.horizon.alerting.corona.model.Factory;

public abstract class AbstractAlertConfigParser<
        C extends AbstractAlertConfig,
        B extends AbstractAlertConfig.Builder<C, ?>
        >
        extends AbstractParser<C>
{
    /* ------------ Constants ------------ */

    private static final String F_ID = "id";

    private static final String F_NAME = "name";

    private static final String F_NAMESPACE = "namespace";

    private static final String F_TYPE = "type";

    private static final String F_ENABLED = "enabled";

    private static final String F_LABELS = "labels";

    private static final String F_ALERT_GROUPING_RULES = "alertgroupingrules";

    private static final boolean QUIET = true;

    /* ------------ Fields ------------ */

    private final Factory<B> builderFactory;

    /* ------------ Constructor ------------ */

    public AbstractAlertConfigParser(final Factory<B> builderFactory)
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
                    builder.setId(val.asLong());
                    break;
                case F_NAME:
                    builder.setName(val.asText());
                    break;
                case F_NAMESPACE:
                    builder.setNamespace(val.asText());
                    break;
                case F_TYPE:
                    final String typeName = val.asText().toUpperCase();
                    builder.setType(AbstractAlertConfig.Type.valueOf(typeName));
                    break;
                case F_ENABLED:
                    builder.setEnabled(val.asBoolean());
                    break;
                case F_LABELS:
                    builder.setLabels(
                            parseList(val, JsonNode::asText, QUIET)
                    );
                    break;
                case F_ALERT_GROUPING_RULES:
                    builder.setGroupingRules(
                            parseList(val, JsonNode::asText, QUIET)
                    );
                    break;
                default:
                    doParse(builder, key, val);
            }
        });

        return builder.build();
    }
}
