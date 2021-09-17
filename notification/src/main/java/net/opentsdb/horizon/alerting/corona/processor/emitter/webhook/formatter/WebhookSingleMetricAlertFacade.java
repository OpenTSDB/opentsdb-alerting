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

package net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.formatter;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricAlert;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.WebhookMeta;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricMessageKitView;

public class WebhookSingleMetricAlertFacade
        extends WebhookAbstractAlertFacade<
        SingleMetricAlert,
                        SingleMetricAlertView,
                        SingleMetricMessageKitView
                        > {

    public WebhookSingleMetricAlertFacade(
            final SingleMetricMessageKitView messageKitView,
            final SingleMetricAlertView alertView,
            final WebhookMeta meta)
    {
        super(messageKitView, alertView, meta);
    }

    @Override
    public String getType()
    {
        return "single_metric";
    }

    @JsonProperty("metric")
    public String getMetric() {
        return alertView.getMetric();
    }

    @JsonProperty("value")
    public double getValue()
    {
        return alertView.getMetricValue();
    }
}

