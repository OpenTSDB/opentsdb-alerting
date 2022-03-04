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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.opentsdb.horizon.alerting.corona.app.NotificationEmitterConfig;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.EmailContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OpsGenieContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.PagerDutyContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.SlackContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.WebhookContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;

public class ContactOverrider
        extends ChainableProcessor<MessageKit, MessageKit>
{

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(ContactOverrider.class);

    private static final String ANALYSIS_CONTEXT = "analysis";

    private static final List<EmailContact> EMAIL_CONTACTS =
            Contacts.of(
                    EmailContact.builder()
                            .setName("test@opentsdb.net")
                            .setEmail("test@opentsdb.net")
                            .build()
            );

    /* ------------ Fields ------------ */

    private final NotificationEmitterConfig config;

    private final List<OpsGenieContact> opsGenieContacts;

    private final List<SlackContact> slackContacts;

    private final List<WebhookContact> webhookContacts;

    private final List<PagerDutyContact> pagerDutyContacts;

    /* ------------ Constructors ------------ */

    public ContactOverrider(final Processor<MessageKit> next,
                            final NotificationEmitterConfig config)
    {
        super(next);
        Objects.requireNonNull(config, "config cannot be null");
        this.config = config;
        this.opsGenieContacts =
                Contacts.of(OpsGenieContact.builder()
                        .setName("OpenTSDB-Test")
                        .setApiKey(config.getDebugOpsgenieApiKey())
                        .build()
                );
        this.slackContacts =
                Contacts.of(SlackContact.builder()
                        .setName("OpenTSDB-Test")
                        .setEndpoint(config.getDebugSlackEndpoint())
                        .build()
                );

        this.webhookContacts =
                Contacts.of(WebhookContact.builder()
                        .setName("OpenTSDB-Test")
                        .setEndpoint(config.getDebugWebhookEndpoint())
                        .build()
                );
        this.pagerDutyContacts =
                Contacts.of(PagerDutyContact.builder()
                        .setName("OpenTSDB-Test")
                        .setRoutingKey(config.getDebugPagerdutyRoutingKey())
                        .build()
                );

    }

    /* ------------ Methods ------------ */

    private MessageKit override(final MessageKit old,
                                final List<? extends Contact> newContacts)
    {
        LOG.debug("Overriding {} with {}.", old.getContacts(), newContacts);
        return MessageKit.builder()
                .setAlertGroup(old.getAlertGroup())
                .setMeta(old.getMeta())
                .setType(old.getType())
                .setContacts(newContacts)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<OcContact> forceAnalyticsContext(
            final List<? extends Contact> oldContacts)
    {
        return ((List<OcContact>) oldContacts).stream()
                .map(c -> OcContact.builder()
                        .setName(c.getName())
                        .setOpsdbProperty(c.getOpsdbProperty())
                        .setDisplayCount(c.getDisplayCount())
                        .setContext(ANALYSIS_CONTEXT)
                        .build()
                )
                .collect(Collectors.toList());
    }

    private MessageKit overrideOcContext(final MessageKit old)
    {
        final List<? extends Contact> newContacts =
                forceAnalyticsContext(old.getContacts());

        LOG.debug("Overriding {} with {}.", old.getContacts(), newContacts);
        return MessageKit.builder()
                .setAlertGroup(old.getAlertGroup())
                .setMeta(old.getMeta())
                .setType(old.getType())
                .setContacts(newContacts)
                .build();
    }

    @Override
    public void process(final MessageKit oldKit)
    {
        final MessageKit newKit;
        switch (oldKit.getType()) {
            case EMAIL:
                newKit = override(oldKit, EMAIL_CONTACTS);
                break;
            case OPSGENIE:
                newKit = override(oldKit, opsGenieContacts);
                break;
            case SLACK:
                newKit = override(oldKit, slackContacts);
                break;
            case OC:
                newKit = overrideOcContext(oldKit);
                break;
            case WEBHOOK:
                newKit = override(oldKit, webhookContacts);
                break;
            case PAGERDUTY:
                newKit = override(oldKit, pagerDutyContacts);
                break;
            default:
                throw new RuntimeException(
                        "Unsupported type: " + oldKit.getType());
        }

        submit(newKit);
    }
}
