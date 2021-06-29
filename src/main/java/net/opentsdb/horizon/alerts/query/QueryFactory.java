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

package net.opentsdb.horizon.alerts.query;

import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.impl.EventAlertConfig;
import net.opentsdb.horizon.alerts.config.impl.HealthCheckConfig;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.config.impl.PeriodOverPeriodAlertConfig;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.query.auradb.AuraDBProcessor;
import net.opentsdb.horizon.alerts.query.egads.PeriodOverPeriodProcessor;
import net.opentsdb.horizon.alerts.query.eventdb.EventProcessor;
import net.opentsdb.horizon.alerts.query.tsdb.TSDBV3SlidingWindowQuery;

/**
 * TODO: Maybe rename to ExecutorFactory.
 */
public final class QueryFactory {

    @SuppressWarnings("unchecked")
    public static <T extends AlertConfig> TimeBasedExecutor<AlertEventBag, T>
    getSlidingWindow(T alertConfig) {
        switch (alertConfig.getAlertType()) {
            case SIMPLE:
                return (TimeBasedExecutor<AlertEventBag, T>)
                        getSlidingWindow((MetricAlertConfig) alertConfig);
            case HEALTH_CHECK:
                return (TimeBasedExecutor<AlertEventBag, T>)
                        getSlidingWindow((HealthCheckConfig) alertConfig);
            case EVENT:
                return (TimeBasedExecutor<AlertEventBag, T>)
                        getSlidingWindow((EventAlertConfig) alertConfig);
            case PERIOD_OVER_PERIOD:
                return (TimeBasedExecutor<AlertEventBag, T>)
                        getSlidingWindow((PeriodOverPeriodAlertConfig) alertConfig);
        }
        throw new IllegalArgumentException(
                "Unknown type: alert_type=" + alertConfig.getAlertType()
        );
    }

    private static TimeBasedExecutor<AlertEventBag, MetricAlertConfig>
    getSlidingWindow(MetricAlertConfig metricAlertConfig) {
        return new TSDBV3SlidingWindowQuery(metricAlertConfig);
    }

    private static TimeBasedExecutor<AlertEventBag, HealthCheckConfig>
    getSlidingWindow(HealthCheckConfig healthCheckConfig) {
        return new AuraDBProcessor(healthCheckConfig);
    }

    private static TimeBasedExecutor<AlertEventBag, EventAlertConfig>
    getSlidingWindow(EventAlertConfig eventAlertConfig) {
        return new EventProcessor(eventAlertConfig);
    }

    private static TimeBasedExecutor<AlertEventBag, PeriodOverPeriodAlertConfig>
    getSlidingWindow(PeriodOverPeriodAlertConfig eventAlertConfig) {
        return new PeriodOverPeriodProcessor(eventAlertConfig);
    }

}
