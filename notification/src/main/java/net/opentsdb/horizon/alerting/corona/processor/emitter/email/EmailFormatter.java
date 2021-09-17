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

package net.opentsdb.horizon.alerting.corona.processor.emitter.email;

import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.email.formatter.EventAlertEmailFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.email.formatter.HealthCheckAlertEmailFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.email.formatter.PeriodOverPeriodAlertEmailFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.email.formatter.SingleMetricAlertEmailFormatter;

public class EmailFormatter implements Formatter<MessageKit, EmailMessage> {

    private final SingleMetricAlertEmailFormatter singleMetricAlertFormatter;

    private final HealthCheckAlertEmailFormatter healthCheckAlertFormatter;

    private final EventAlertEmailFormatter eventAlertEmailFormatter;

    private final PeriodOverPeriodAlertEmailFormatter periodOverPeriodAlertFormatter;

    public EmailFormatter(final String debugPrefix)
    {
        this.singleMetricAlertFormatter =
                new SingleMetricAlertEmailFormatter(debugPrefix);
        this.healthCheckAlertFormatter =
                new HealthCheckAlertEmailFormatter(debugPrefix);
        this.eventAlertEmailFormatter =
                new EventAlertEmailFormatter(debugPrefix);
        this.periodOverPeriodAlertFormatter =
                new PeriodOverPeriodAlertEmailFormatter(debugPrefix);
    }

    @Override
    public EmailMessage format(final MessageKit messageKit)
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
                return eventAlertEmailFormatter.format(messageKit);
            case PERIOD_OVER_PERIOD:
                return periodOverPeriodAlertFormatter.format(messageKit);
        }

        throw new IllegalArgumentException(
                "Unsupported alert type: " + alertType);
    }
}
