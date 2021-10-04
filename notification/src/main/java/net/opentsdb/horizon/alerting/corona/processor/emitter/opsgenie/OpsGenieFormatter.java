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

package net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie;

import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie.formatter.EventAlertOpsGenieFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie.formatter.HealthCheckAlertOpsGenieFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie.formatter.OpsGenieSingleMetricAlertFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie.formatter.PeriodOverPeriodAlertOpsGenieFormatter;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;

public class OpsGenieFormatter
        implements Formatter<MessageKit, OpsGenieAlert>
{

    /* ------------ Fields ------------ */
    private final OpsGenieSingleMetricAlertFormatter singleMetricAlertFormatter;

    private final HealthCheckAlertOpsGenieFormatter healthCheckAlertFormatter;

    private final EventAlertOpsGenieFormatter eventAlertFormatter;

    private final PeriodOverPeriodAlertOpsGenieFormatter periodOverPeriodFormatter;

    /* ------------ Constructor ------------ */

    private OpsGenieFormatter(final String user, final String source)
    {
        this.singleMetricAlertFormatter =
                new OpsGenieSingleMetricAlertFormatter(user, source);
        this.healthCheckAlertFormatter =
                new HealthCheckAlertOpsGenieFormatter(user, source);
        this.eventAlertFormatter =
                new EventAlertOpsGenieFormatter(user, source);
        this.periodOverPeriodFormatter =
                new PeriodOverPeriodAlertOpsGenieFormatter(user, source);
    }

    /* ------------ Methods ------------ */

    @Override
    public OpsGenieAlert format(final MessageKit messageKit)
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
                return periodOverPeriodFormatter.format(messageKit);
        }

        throw new IllegalArgumentException(
                "Unsupported alert type: " + alertType);
    }

    /* ------------ Builder ------------ */

    public static final class Builder {

        private String user;

        private String source;

        public Builder setUser(final String user)
        {
            this.user = user;
            return this;
        }

        public Builder setSource(final String source)
        {
            this.source = source;
            return this;
        }

        public OpsGenieFormatter build()
        {
            return new OpsGenieFormatter(user, source);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
