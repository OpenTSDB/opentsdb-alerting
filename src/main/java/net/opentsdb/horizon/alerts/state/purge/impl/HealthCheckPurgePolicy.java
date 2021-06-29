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
import net.opentsdb.horizon.alerts.config.impl.HealthCheckConfig;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import net.opentsdb.horizon.alerts.state.purge.PurgePolicy;
import net.opentsdb.horizon.alerts.state.purge.PurgeUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HealthCheckPurgePolicy implements PurgePolicy {

    private final HealthCheckConfig healthCheckConfig;

    private final EnvironmentConfig environmentConfig;

    private final String[] purgeTags;

    public HealthCheckPurgePolicy(final HealthCheckConfig healthCheckConfig) {

        this.healthCheckConfig = healthCheckConfig;
        this.environmentConfig = new EnvironmentConfig();
        this.purgeTags = Monitoring.getTagsPurgeType(healthCheckConfig, "old");
    }


    @Override
    public void purge(final AlertStateStore alertStateStore,
                      final boolean firstPurge) {

        final long purgeInterval = healthCheckConfig.isMissingEnabled() ?
                Math.min(
                        Math.max(healthCheckConfig.getMissingIntervalInSec() * 4,
                                environmentConfig.getHealthCheckPurgeIntervalMissing()),
                        healthCheckConfig.getMissingDataPurgeIntervalSec())
                :
                 environmentConfig.getHealthCheckPurgeIntervalNonMissing();

        final long purgeDate = AlertUtils.getPurgeDate(purgeInterval);

        log.info("id: {} Starting purge " +
                        " missing: {}" +
                        " missing purge interval: {}" +
                        " purgeIntervalCalc: {}" +
                        " purgeDate: {}" +
                        " first purge: {}",
                healthCheckConfig.getAlertId(),
                healthCheckConfig.isMissingEnabled(),
                healthCheckConfig.getMissingDataPurgeIntervalSec(),
                purgeInterval,
                purgeDate,
                firstPurge);

        PurgeUtil.doPurge(
                alertStateStore,
                firstPurge,
                purgeDate,
                purgeTags,
                healthCheckConfig.getAlertId());

    }
}
