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

package net.opentsdb.horizon.alerting.corona.model.alertconfig.impl;

import net.opentsdb.horizon.alerting.corona.model.Parser;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.AbstractAlertConfigParser;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.Notification;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.NotificationParser;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.Recipient;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.RecipientParser;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.impl.NAlertConfig.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class NAlertConfigParser
        extends AbstractAlertConfigParser<NAlertConfig, Builder<?>>
{

    private static final Logger LOG =
            LoggerFactory.getLogger(NAlertConfigParser.class);

    private final Parser<Notification> notificationParser;

    public NAlertConfigParser()
    {
        super(NAlertConfig::builder);

        this.notificationParser =
                new NotificationParser(
                        Notification::builder,
                        new RecipientParser(Recipient::builder)
                );
    }

    @Override
    protected void doParse(
            final NAlertConfig.Builder<?> builder,
            final String key,
            final JsonNode node)
    {
        if ("notification".equals(key)) {
            builder.setNotification(notificationParser.parse(node));
        } else {
            LOG.trace("Unknown key={}", key);
        }
    }
}
