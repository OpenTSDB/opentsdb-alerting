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

package net.opentsdb.horizon.alerting.corona.processor.emitter.slack.formatter;

import com.google.inject.Inject;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackBuilders;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricMessageKitView;

public final class SlackSingleMetricAlertFormatter
        extends SlackAbstractFormatter<
        SingleMetricAlert,
        SingleMetricAlertView,
        SingleMetricMessageKitView
        >
{

    @Inject
    public SlackSingleMetricAlertFormatter(final SlackBuilders buildersFactory)
    {
        super(buildersFactory);
    }

    @Override
    protected String generateAttachmentText(final SingleMetricAlertView view)
    {
        return "Value: *" + view.getMetricValue() + "*  " +
                "_Tags:_ " + formatTags(view.getSortedTags());
    }
}
