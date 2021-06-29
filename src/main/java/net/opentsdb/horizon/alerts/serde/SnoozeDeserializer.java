/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import net.opentsdb.horizon.alerts.query.tsdb.TsdbV3QueryBuilder;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.query.filter.QueryFilter;

import java.io.IOException;

public class SnoozeDeserializer extends JsonDeserializer<QueryFilter> {

    @Override
    public QueryFilter deserialize(JsonParser parser, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        final JsonNode treeNode = AlertUtils.jsonMapper.readTree(parser);

        final String s = AlertUtils.jsonMapper.writeValueAsString(treeNode);
        return TsdbV3QueryBuilder.fromFilterStringForSnooze(s);
    }
}
