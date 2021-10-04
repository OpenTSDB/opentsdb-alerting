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

package net.opentsdb.horizon.alerting.corona.processor.emitter.oc.formatter;

import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricMessageKitView;

public class SingleMetricAlertOcFormatter
        extends AbstractAlertOcFormatter<
        SingleMetricAlert,
        SingleMetricAlertView,
        SingleMetricMessageKitView
        >
{

    public SingleMetricAlertOcFormatter(final String colo, final String host)
    {
        super(colo, host);
    }

    /* ------------ Methods ------------ */

    @Override
    protected StringBuilder buildAppDescriptionPrefix(
            final StringBuilder sb,
            final SingleMetricAlertView view)
    {
        return sb.append(view.getMetric());
    }

    @Override
    protected String buildMessageSnippet(
            final SingleMetricMessageKitView messageKitView,
            final SingleMetricAlertView view)
    {
        return String.format(
                "%s -> %s. Value: %f",
                view.getMetric(),
                view.getDescription(),
                view.getMetricValue()
        );
    }
}