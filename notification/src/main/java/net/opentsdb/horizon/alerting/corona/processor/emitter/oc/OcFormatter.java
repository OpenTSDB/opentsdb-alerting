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

package net.opentsdb.horizon.alerting.corona.processor.emitter.oc;

import java.util.List;

import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.processor.emitter.oc.formatter.PeriodOverPeriodAlertOcFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.oc.formatter.EventAlertOcFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.oc.formatter.HealthCheckAlertOcFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.oc.formatter.SingleMetricAlertOcFormatter;

public class OcFormatter implements Formatter<MessageKit, List<OcCommand>> {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(OcFormatter.class);

    /* ------------ Fields ------------ */

    private final SingleMetricAlertOcFormatter singleMetricAlertFormatter;

    private final HealthCheckAlertOcFormatter healthCheckAlertFormatter;

    private final EventAlertOcFormatter eventAlertFormatter;

    private final PeriodOverPeriodAlertOcFormatter periodOverPeriodFormatter;

    /* ------------ Constructors ------------ */

    public OcFormatter(final String colo, final String host) {
        this.singleMetricAlertFormatter =
                new SingleMetricAlertOcFormatter(colo, host);
        this.healthCheckAlertFormatter =
                new HealthCheckAlertOcFormatter(colo, host);
        this.eventAlertFormatter =
                new EventAlertOcFormatter(colo, host);
        this.periodOverPeriodFormatter =
                new PeriodOverPeriodAlertOcFormatter(colo, host);
    }

    /* ------------ Methods ------------ */

    @Override
    public List<OcCommand> format(final MessageKit messageKit) {
        final AlertGroup alertGroup = messageKit.getAlertGroup();
        final GroupKey groupKey = alertGroup.getGroupKey();

        switch (groupKey.getAlertType()) {
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
                "Unsupported alert class: " + messageKit.getClass());
    }
}
