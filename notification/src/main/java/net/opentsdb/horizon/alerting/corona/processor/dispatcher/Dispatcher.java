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

package net.opentsdb.horizon.alerting.corona.processor.dispatcher;

import java.util.Objects;
import java.util.function.Consumer;

import net.opentsdb.horizon.alerting.corona.model.messagekit.PrePackedMessageKit;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.Processor;

public class Dispatcher implements Processor<PrePackedMessageKit> {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(Dispatcher.class);

    /* ------------ Fields ------------ */

    private final Processor<PrePackedMessageKit> emailHandler;

    private final Processor<PrePackedMessageKit> ocHandler;

    private final Processor<PrePackedMessageKit> opsGenieHandler;

    private final Processor<PrePackedMessageKit> slackHandler;

    private final Processor<PrePackedMessageKit> webhookHandler;

    private final Processor<PrePackedMessageKit> pagerDutyHandler;

    /* ------------ Constructor ------------ */

    // TODO: Try to make this type-safe.
    Dispatcher(final Builder builder)
    {
        Objects.requireNonNull(builder.emailHandler,
                "emailHandler cannot be null");
        Objects.requireNonNull(builder.ocHandler,
                "ocHandler cannot be null");
        Objects.requireNonNull(builder.opsGenieHandler,
                "opsGenieHandler cannot be null");
        Objects.requireNonNull(builder.slackHandler,
                "slackHandler cannot be null");
        Objects.requireNonNull(builder.webhookHandler,
                "webhookHandler cannot be null");
        Objects.requireNonNull(builder.pagerDutyHandler,
                "pagerDutyHandler cannot be null");

        this.emailHandler = builder.emailHandler;
        this.ocHandler = builder.ocHandler;
        this.opsGenieHandler = builder.opsGenieHandler;
        this.slackHandler = builder.slackHandler;
        this.webhookHandler = builder.webhookHandler;
        this.pagerDutyHandler = builder.pagerDutyHandler;
    }

    /* ------------ Methods ------------ */

    private void submit(final PrePackedMessageKit message,
                        final Consumer<PrePackedMessageKit> handler)
    {
        try {
            handler.accept(message);
        } catch (Exception e) {
            final String type = message.getType().name().toLowerCase();
            AppMonitor.get().countDispatchFailed(type);
            LOG.error("Failed to dispatch to handler: message = {}.",
                    message, e);
        }
    }

    @Override
    public void process(final PrePackedMessageKit message)
    {
        switch (message.getType()) {
            case EMAIL:
                submit(message, emailHandler::process);
                break;
            case OC:
                submit(message, ocHandler::process);
                break;
            case OPSGENIE:
                submit(message, opsGenieHandler::process);
                break;
            case SLACK:
                submit(message, slackHandler::process);
                break;
            case WEBHOOK:
                submit(message, webhookHandler::process);
                break;
            case PAGERDUTY:
                submit(message, pagerDutyHandler::process);
                break;
            default:
                LOG.error("Unknown messagekit type: messagekit = {}", message);
        }
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private Processor<PrePackedMessageKit> emailHandler;

        private Processor<PrePackedMessageKit> ocHandler;

        private Processor<PrePackedMessageKit> opsGenieHandler;

        private Processor<PrePackedMessageKit> slackHandler;

        private Processor<PrePackedMessageKit> webhookHandler;

        private Processor<PrePackedMessageKit> pagerDutyHandler;

        protected Builder() {}

        public Builder setEmailHandler(
                final Processor<PrePackedMessageKit> emailHandler)
        {
            this.emailHandler = emailHandler;
            return this;
        }

        public Builder setOcHandler(
                final Processor<PrePackedMessageKit> ocHandler)
        {
            this.ocHandler = ocHandler;
            return this;
        }

        public Builder setOpsGenieHandler(
                final Processor<PrePackedMessageKit> opsGenieHandler)
        {
            this.opsGenieHandler = opsGenieHandler;
            return this;
        }

        public Builder setSlackHandler(
                final Processor<PrePackedMessageKit> slackHandler)
        {
            this.slackHandler = slackHandler;
            return this;
        }

        public Builder setWebhookHandler(
                final Processor<PrePackedMessageKit> webhookHandler)
        {
            this.webhookHandler = webhookHandler;
            return this;
        }

        public Builder setPagerDutyHandler(
                final Processor<PrePackedMessageKit> pagerDutyHandler)
        {
            this.pagerDutyHandler = pagerDutyHandler;
            return this;
        }

        public Dispatcher build()
        {
            return new Dispatcher(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
