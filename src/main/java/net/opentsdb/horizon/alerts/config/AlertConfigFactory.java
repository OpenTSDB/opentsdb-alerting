/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.config;

import static net.opentsdb.horizon.alerts.config.impl.HealthCheckConfigFields.HEALTHCHECK;
import static net.opentsdb.horizon.alerts.config.impl.MetricAlertConfigFields.SINGLE_METRIC;

import net.opentsdb.horizon.alerts.config.impl.EventAlertConfig;
import net.opentsdb.horizon.alerts.config.impl.HealthCheckConfig;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.config.impl.PeriodOverPeriodAlertConfig;
import net.opentsdb.horizon.alerts.enums.AlertType;
import net.opentsdb.horizon.alerts.enums.MetricAlertType;

public class AlertConfigFactory {

    private static final String EVENT = "event";
    private static final String PERIOD_OVER_PERIOD = "periodOverPeriod";
    private static final String SIMPLE = "simple";

    public static AlertConfig getAlertConfig(String namespace,
                                             String alertType,
                                             String thresholdType,
                                             long alertId,
                                             long lastModified) {
        if (SIMPLE.equalsIgnoreCase(alertType)) {
            if (SINGLE_METRIC.equalsIgnoreCase(thresholdType)) {
                return new MetricAlertConfig(
                        namespace,
                        AlertType.SIMPLE,
                        MetricAlertType.SINGLE_METRIC,
                        alertId,
                        lastModified
                );
            } else if (PERIOD_OVER_PERIOD.equalsIgnoreCase(thresholdType)) {
                return new PeriodOverPeriodAlertConfig(
                        namespace,
                        AlertType.PERIOD_OVER_PERIOD,
                        alertId,
                        lastModified
                );
            }
        } else if (HEALTHCHECK.equalsIgnoreCase(alertType)) {
            return new HealthCheckConfig(
                    namespace,
                    AlertType.HEALTH_CHECK,
                    alertId,
                    lastModified
            );
        } else if (EVENT.equalsIgnoreCase(alertType)) {
            return new EventAlertConfig(
                    namespace,
                    AlertType.EVENT,
                    alertId,
                    lastModified
            );
        }

        throw new AssertionError("Failed to find corresponding config: " +
                "alert_type=" + alertType + " threshold_type=" + thresholdType);
    }
}
