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

package net.opentsdb.horizon.alerting.corona.model.messagekit.meta;

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;

public class MetaSerializers {

    /* ------------ Constants ------------ */

    private final EmailMetaSerializer emailSerializer;

    private final WebhookMetaSerializer webhookSerializer;

    private final OcMetaSerializer ocSerializer;

    private final OpsGenieMetaSerializer opsGenieSerializer;

    private final SlackMetaSerializer slackSerializer;

    private final PagerDutyMetaSerializer pagerDutySerializer;

    /* ------------ Constructor ------------ */

    public MetaSerializers()
    {
        this.emailSerializer = new EmailMetaSerializer();
        this.webhookSerializer = new WebhookMetaSerializer();
        this.ocSerializer = new OcMetaSerializer();
        this.opsGenieSerializer = new OpsGenieMetaSerializer();
        this.slackSerializer = new SlackMetaSerializer();
        this.pagerDutySerializer = new PagerDutyMetaSerializer();
    }

    /* ------------ Methods ------------ */

    public AbstractSerializer<? extends Meta> get(final Contact.Type type)
    {
        switch (type) {
            case EMAIL:
                return emailSerializer;
            case WEBHOOK:
                return webhookSerializer;
            case OC:
                return ocSerializer;
            case OPSGENIE:
                return opsGenieSerializer;
            case SLACK:
                return slackSerializer;
            case PAGERDUTY:
                return pagerDutySerializer;
            default:
                throw new RuntimeException(
                        "Unknown contact type: " + type.name());
        }
    }
}
