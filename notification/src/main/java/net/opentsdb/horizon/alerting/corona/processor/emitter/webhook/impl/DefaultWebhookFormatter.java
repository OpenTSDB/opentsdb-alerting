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

package net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.impl;

import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.WebhookMeta;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventMessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthCheckAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthCheckMessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodMessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricMessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.WebhookEvent;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.WebhookFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.formatter.WebhookEventAlertFacade;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.formatter.WebhookHealthCheckAlertFacade;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.formatter.WebhookPeriodOverPeriodAlertFacade;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.formatter.WebhookSingleMetricAlertFacade;

public class DefaultWebhookFormatter implements WebhookFormatter {
    @Override
    public WebhookEvent format(final MessageKitView messageKit,
                               final AlertView alertView,
                               final WebhookMeta meta) {
        if (alertView instanceof SingleMetricAlertView) {
            return new WebhookSingleMetricAlertFacade(
                    (SingleMetricMessageKitView) messageKit,
                    (SingleMetricAlertView) alertView,
                    meta
            );
        } else if (alertView instanceof HealthCheckAlertView) {
            return new WebhookHealthCheckAlertFacade(
                    (HealthCheckMessageKitView) messageKit,
                    (HealthCheckAlertView) alertView,
                    meta
            );
        } else if (alertView instanceof EventAlertView) {
            return new WebhookEventAlertFacade(
                    (EventMessageKitView) messageKit,
                    (EventAlertView) alertView,
                    meta
            );
        } else if (alertView instanceof PeriodOverPeriodAlertView) {
            return new WebhookPeriodOverPeriodAlertFacade(
                    (PeriodOverPeriodMessageKitView) messageKit,
                    (PeriodOverPeriodAlertView) alertView,
                    meta
            );
        }

        throw new IllegalArgumentException("Unexpected alert view: class=" +
                alertView.getClass().getCanonicalName());
    }

    public interface Builder extends WebhookFormatter.Builder<Builder> {
    }

    private static final class DefaultBuilder implements Builder {
        @Override
        public WebhookFormatter build() {
            return new DefaultWebhookFormatter();
        }
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }
}
