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

package net.opentsdb.horizon.alerting.corona.model.contact.impl;

import com.fasterxml.jackson.databind.JsonNode;

import net.opentsdb.horizon.alerting.corona.model.contact.AbstractContactParser;

public class OcContactParser
        extends AbstractContactParser<OcContact, OcContact.Builder<?>>
{

    private static final String F_DISPLAY_COUNT = "displaycount";

    private static final String F_CUSTOMER = "customer";

    private static final String F_CONTEXT = "context";

    private static final String F_OPSDB_PROPERTY = "opsdbproperty";


    public OcContactParser()
    {
        super(OcContact::builder);
    }

    @Override
    protected void doParse(final OcContact.Builder<?> builder,
                           final String key,
                           final JsonNode node)
    {
        switch (key) {
            case F_DISPLAY_COUNT:
                builder.setDisplayCount(node.asText());
                break;
            case F_CUSTOMER:
                builder.setCustomer(node.asText());
                break;
            case F_CONTEXT:
                builder.setContext(node.asText());
                break;
            case F_OPSDB_PROPERTY:
                builder.setOpsdbProperty(node.asText());
                break;
        }
    }
}
