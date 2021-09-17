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

import net.opentsdb.horizon.alerting.corona.model.AbstractSerializer;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;

public class ContactSerializerFactory {

    private final EmailContactSerializer emailContactSerializer;

    private final WebhookContactSerializer webhookContactSerializer;

    private final OcContactSerializer ocContactSerializer;

    private final OpsGenieContactSerializer opsGenieContactSerializer;

    private final SlackContactSerializer slackContactSerializer;

    public ContactSerializerFactory()
    {
        this.emailContactSerializer = new EmailContactSerializer();
        this.webhookContactSerializer = new WebhookContactSerializer();
        this.ocContactSerializer = new OcContactSerializer();
        this.opsGenieContactSerializer = new OpsGenieContactSerializer();
        this.slackContactSerializer = new SlackContactSerializer();
    }

    public AbstractSerializer<? extends Contact> get(Contact.Type type)
    {
        switch (type) {
            case EMAIL:
                return emailContactSerializer;
            case WEBHOOK:
                return webhookContactSerializer;
            case OC:
                return ocContactSerializer;
            case OPSGENIE:
                return opsGenieContactSerializer;
            case SLACK:
                return slackContactSerializer;
            default:
                throw new IllegalArgumentException(
                        "Unknown contact type: " + type.name());
        }
    }
}
