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

import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import net.opentsdb.horizon.alerting.corona.model.AbstractParser;
import net.opentsdb.horizon.alerting.corona.model.Parser;

public class ContactsParser extends AbstractParser<Contacts> {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(ContactsParser.class);

    private static final Parser<EmailContact> emailContactParser =
            new EmailContactParser();

    private static final Parser<WebhookContact> webhookContactParser =
            new WebhookContactParser();

    private static final Parser<OcContact> ocContactParser =
            new OcContactParser();

    private static final Parser<OpsGenieContact> opsGenieContactParser =
            new OpsGenieContactParser();

    private static final Parser<SlackContact> slackContactParser =
            new SlackContactParser();

    private static final String F_EMAIL = "email";

    private static final String F_HTTP = "http";

    private static final String F_WEBHOOK = "webhook";

    private static final String F_SLACK = "slack";

    private static final String F_OC = "oc";

    private static final String F_OPSGENIE = "opsgenie";

    private static final boolean SILENT_FAIL = true;

    /* ------------ Methods ------------ */

    @Override
    protected Contacts doParse(JsonNode root)
    {
        LOG.trace("Parse contacts: content={}", root);

        final Contacts.Builder builder = Contacts.builder();

        root.fields().forEachRemaining(e -> {
            final String typeName = e.getKey().toLowerCase();
            final JsonNode val = e.getValue();

            switch (typeName) {
                case F_EMAIL:
                    builder.setEmailContacts(
                            parseList(val, emailContactParser, SILENT_FAIL)
                    );
                    break;
                case F_HTTP:
                case F_WEBHOOK:
                    builder.setWebhookContacts(
                            parseList(val, webhookContactParser, SILENT_FAIL)
                    );
                    break;
                case F_OC:
                    builder.setOcContacts(
                            parseList(val, ocContactParser, SILENT_FAIL)
                    );
                    break;
                case F_OPSGENIE:
                    builder.setOpsGenieContacts(
                            parseList(val, opsGenieContactParser, SILENT_FAIL)
                    );
                    break;
                case F_SLACK:
                    builder.setSlackContacts(
                            parseList(val, slackContactParser, SILENT_FAIL)
                    );
                    break;
                default:
                    LOG.trace("Unknown type: name={}, value={}", typeName, val);
            }
        });

        return builder.build();
    }
}
