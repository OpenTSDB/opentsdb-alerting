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

package net.opentsdb.horizon.alerting.corona.processor.emitter.slack;

import java.util.Collections;
import java.util.List;

import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackRequest;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.impl.SlackBuildersImpl;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.formatter.PeriodOverPeriodAlertSlackFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.formatter.SlackEventAlertFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.formatter.SlackHealthCheckAlertFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.formatter.SlackSingleMetricAlertFormatter;

public class SlackFormatter
        implements Formatter<MessageKit, List<SlackRequest>>
{

    private static final SlackSingleMetricAlertFormatter singleMetricAlertFormatter =
            new SlackSingleMetricAlertFormatter(SlackBuildersImpl.instance());

    private static final SlackHealthCheckAlertFormatter
            healthCheckAlertFormatter = new SlackHealthCheckAlertFormatter(SlackBuildersImpl.instance());

    private static final SlackEventAlertFormatter
            eventAlertFormatter = new SlackEventAlertFormatter(SlackBuildersImpl.instance());

    private static final PeriodOverPeriodAlertSlackFormatter
            periodOverPeriodFormatter = new PeriodOverPeriodAlertSlackFormatter();

    @Override
    public List<SlackRequest> format(final MessageKit messageKit)
    {
        final AlertType alertType =
                messageKit
                        .getAlertGroup()
                        .getGroupKey()
                        .getAlertType();

        switch (alertType) {
            case SINGLE_METRIC:
                return singleMetricAlertFormatter.format(messageKit);
            case HEALTH_CHECK:
                return healthCheckAlertFormatter.format(messageKit);
            case EVENT:
                return eventAlertFormatter.format(messageKit);
            case PERIOD_OVER_PERIOD:
                return Collections.singletonList(
                        periodOverPeriodFormatter.format(messageKit)
                );
        }

        throw new IllegalArgumentException("Unsupported alert type: " + alertType);
    }
}
