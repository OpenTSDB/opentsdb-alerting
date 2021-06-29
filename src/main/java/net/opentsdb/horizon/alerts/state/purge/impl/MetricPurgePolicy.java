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

package net.opentsdb.horizon.alerts.state.purge.impl;

import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.Monitoring;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import net.opentsdb.horizon.alerts.state.purge.PurgePolicy;
import net.opentsdb.horizon.alerts.state.purge.PurgeUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MetricPurgePolicy implements PurgePolicy {

    private final MetricAlertConfig metricAlertConfig;

    private final EnvironmentConfig environmentConfig;

    private final String[] purgeTags;

    public MetricPurgePolicy(final MetricAlertConfig metricAlertConfig) {
        this.metricAlertConfig = metricAlertConfig;
        this.environmentConfig = new EnvironmentConfig();
        this.purgeTags = Monitoring.getTagsPurgeType(metricAlertConfig, "old");

    }


    @Override
    public void purge(AlertStateStore alertStateStore, boolean firstPurge) {

        final long purgeInterval = Math.max(
                metricAlertConfig.getSlidingWindowInSecs() * 4,
                metricAlertConfig.isMissingEnabled() ?
                        environmentConfig.getMetricPurgeIntervalMissing() :
                        (metricAlertConfig.isAutoRecover() ?
                            Math.max(metricAlertConfig.getAutoRecoveryInterval() * 2,
                                    environmentConfig.getMetricPurgeIntervalNonMissing()) :
                                environmentConfig.getMetricPurgeIntervalNonMissing())
        );

        final long purgeDate = AlertUtils.getPurgeDate(purgeInterval);

        log.info("id: {} Starting purge " +
                " window: {}" +
                " missing: {}" +
                " auto recovery: {}" +
                " purgeIntervalCalc: {}" +
                " purgeDate: {}" +
                " first purge: {}",
                metricAlertConfig.getAlertId(),
                metricAlertConfig.getSlidingWindowInSecs(),
                metricAlertConfig.isMissingEnabled(),
                metricAlertConfig.isAutoRecover(),
                purgeInterval,
                purgeDate,
                firstPurge);

        PurgeUtil.doPurge(
                alertStateStore,
                firstPurge,
                purgeDate,
                purgeTags,
                metricAlertConfig.getAlertId());

    }
}
