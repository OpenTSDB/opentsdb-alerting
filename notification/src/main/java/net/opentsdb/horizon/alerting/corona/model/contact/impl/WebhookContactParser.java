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
import net.opentsdb.horizon.alerting.corona.model.contact.impl.WebhookContact.Builder;

public class WebhookContactParser
        extends AbstractContactParser<WebhookContact, Builder<?>>
{

    private static final String F_ENDPOINT = "endpoint";

    public WebhookContactParser()
    {
        super(WebhookContact::builder);
    }

    @Override
    protected void doParse(final WebhookContact.Builder<?> builder,
                           final String key,
                           final JsonNode node)
    {
        switch (key) {
            case F_ENDPOINT:
                builder.setEndpoint(node.asText());
                break;
        }
    }
}
