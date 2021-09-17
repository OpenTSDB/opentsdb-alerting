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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.model.AbstractParser;
import net.opentsdb.horizon.alerting.corona.model.Factory;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;

import com.fasterxml.jackson.databind.JsonNode;

public class NotificationParser extends AbstractParser<Notification> {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(NotificationParser.class);

    private static final String F_TRANSITIONS_TO_NOTIFY = "transitionstonotify";

    private static final String F_RECIPIENTS = "recipients";

    private static final String F_SUBJECT = "subject";

    private static final String F_BODY = "body";

    private static final String F_OPSGENIE_PRIORITY = "opsgeniepriority";

    private static final String F_OPSGENIE_AUTOCLOSE = "opsgenieautoclose";

    private static final String F_OPSGENIE_TAGS = "opsgenietags";

    private static final String F_RUNBOOK_ID = "runbookid";

    private static final String F_OC_SEVERITY = "ocseverity";

    private static final String F_OC_TIER = "octier";

    private static final boolean QUIET = true;

    /* ------------ Fields ------------ */

    private final Factory<Notification.Builder> notificationBuilderFactory;

    private final RecipientParser recipientParser;

    /* ------------ Constructors ------------ */

    public NotificationParser(
            final Factory<Notification.Builder> notificationBuilderFactory,
            final RecipientParser recipientParser)
    {
        Objects.requireNonNull(notificationBuilderFactory,
                "notificationBuilderFactory cannot be null");
        this.notificationBuilderFactory = notificationBuilderFactory;
        this.recipientParser = recipientParser;
    }

    /* ------------ Methods ------------ */

    private Map<Contact.Type, List<Recipient>> parseRecipients(JsonNode node)
    {
        final Map<Contact.Type, List<Recipient>> recipients = new HashMap<>(8);
        node.fields().forEachRemaining(
                e -> {
                    final Contact.Type contactType;
                    switch (e.getKey().toUpperCase()) {
                        case "EMAIL":
                            contactType = Contact.Type.EMAIL;
                            break;
                        case "HTTP":
                        case "WEBHOOK":
                            contactType = Contact.Type.WEBHOOK;
                            break;
                        case "OC":
                            contactType = Contact.Type.OC;
                            break;
                        case "OPSGENIE":
                            contactType = Contact.Type.OPSGENIE;
                            break;
                        case "SLACK":
                            contactType = Contact.Type.SLACK;
                            break;
                        default:
                            LOG.warn("Unknown contact type '{}'", e.getKey());
                            return;

                    }
                    recipients.put(
                            contactType,
                            parseList(e.getValue(), recipientParser, QUIET)
                    );
                }
        );
        return recipients;
    }

    @Override
    public Notification doParse(JsonNode root)
    {
        final Notification.Builder builder =
                notificationBuilderFactory.create();

        root.fields().forEachRemaining(e -> {
            final String key = e.getKey().toLowerCase();
            final JsonNode val = e.getValue();
            switch (key) {
                case F_TRANSITIONS_TO_NOTIFY:
                    builder.setTransitionsToNotify(
                            parseList(val, JsonNode::asText, QUIET)
                    );
                    break;
                case F_RECIPIENTS:
                    builder.setRecipients(parseRecipients(val));
                    break;
                case F_SUBJECT:
                    builder.setSubject(val.asText());
                    break;
                case F_BODY:
                    builder.setBody(val.asText());
                    break;
                case F_OPSGENIE_PRIORITY:
                    builder.setOpsGeniePriority(val.asText());
                    break;
                case F_OPSGENIE_AUTOCLOSE:
                    final boolean opsGenieAutoClose;
                    if (val.isBoolean()) {
                        opsGenieAutoClose = val.asBoolean();
                    } else {
                        opsGenieAutoClose = Boolean.parseBoolean(val.asText());
                    }
                    builder.setOpsGenieAutoClose(opsGenieAutoClose);
                    break;
                case F_OPSGENIE_TAGS:
                    builder.setOpsGenieTags(parseList(val, JsonNode::asText, QUIET));
                    break;
                case F_RUNBOOK_ID:
                    builder.setRunbookId(val.asText());
                    break;
                case F_OC_SEVERITY:
                    final String severityValue = val.asText();
                    if (severityValue == null || severityValue.trim().isEmpty()) {
                        builder.setOcSeverity(OcSeverity.NOT_SET);
                        break;
                    }

                    try {
                        final byte id = Byte.parseByte(severityValue);
                        builder.setOcSeverity(OcSeverity.fromId(id));
                    } catch (IllegalArgumentException ex) {
                        LOG.warn("Failed to parse ocSeverity: value='{}', reason={}",
                                val.asText(), ex.getMessage());
                        builder.setOcSeverity(OcSeverity.NOT_SET);
                    }
                    break;
                case F_OC_TIER:
                    final String tierValue = val.asText();
                    if (tierValue == null || tierValue.trim().isEmpty()) {
                        builder.setOcTier(OcTier.NOT_SET);
                        break;
                    }

                    try {
                        final byte id = Byte.parseByte(tierValue);
                        builder.setOcTier(OcTier.fromId(id));
                    } catch (IllegalArgumentException ex) {
                        LOG.warn("Failed to parse ocTier: value='{}', reason={}",
                                val.asText(), ex.getMessage());
                        builder.setOcTier(OcTier.NOT_SET);
                    }
                    break;
                default:
                    LOG.trace("Unknown entry: key='{}', value='{}'",
                            e.getKey(), e.getValue().asText());
            }
        });

        return builder.build();
    }
}
