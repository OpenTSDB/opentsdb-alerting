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

import java.util.concurrent.TimeUnit;

import net.opentsdb.horizon.alerts.Monitoring;
import net.opentsdb.horizon.alerts.state.purge.Purge;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.state.persistence.StatePersistor;
import net.opentsdb.horizon.alerts.state.persistence.StatePersistors;
import net.opentsdb.horizon.alerts.state.persistence.StateProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.state.AlertStateStore;

public abstract class StateTimeBasedExecutor<T extends AlertConfig>
        extends TimeBasedExecutor<AlertEventBag, T> {

    private static final Logger LOG =
            LoggerFactory.getLogger(StateTimeBasedExecutor.class);

    private volatile AlertStateStore alertStateStore = null;

    private Purge purge;

    private int slack;

    private final StatePersistor statePersistor;

    public StateTimeBasedExecutor(T alertConfig) {
        super(alertConfig);
        this.statePersistor = StatePersistors.getDefault(alertConfig.getAlertId());
    }

    @Override
    public boolean prepAndValidate(T alertConfig) throws AlertException {
        this.purge = alertConfig.createPurge();
        if (alertStateStore == null) {
            // TODO: It is hard to inject state from above. Changes to the
            //       AlertDaemon -> Monitor -> StateTimeBasedExecutor are
            //       to be extensive. This is a POC.
            this.alertStateStore =
                    StateProviders.getDefault()
                            .get(alertConfig)
                            .orElseGet(alertConfig::createAlertStateStore);
            this.purge.purge(this.alertStateStore, true);
        } else {
            alertStateStore.setTransitionConfig(alertConfig.getTransitionConfig());
            alertStateStore.setNagIntervalInSecs(alertConfig.getNagIntervalInSecs());
            alertStateStore.setStoreAlertIdentity(alertConfig.storeIdentity());
        }
        this.purge = alertConfig.createPurge();

        LOG.info("id: {} Updated with : {} {} {}", alertConfig.getAlertId(),
                alertStateStore.getStoreAlertIdentity(),
                alertConfig.getNagIntervalInSecs(), alertConfig.getNotificationConfig());
        if (alertConfig.getEvaluationDelayInMins() == 0) {
            slack = AlertUtils.defaultEvaluationDelayInMins*60;
        } else {
            slack = alertConfig.getEvaluationDelayInMins()*60;
        }

        return prepAndValidate(alertConfig, alertStateStore);
    }

    public abstract boolean prepAndValidate(T alertConfig,
                                            AlertStateStore alertStateStore)
            throws AlertException;

    @Override
    public AlertEventBag execute(long endTime, TimeUnit timeUnit)
            throws AlertException {
        alertStateStore.newRun();
        final AlertEventBag alertBag =
                execute(endTime - slack, timeUnit, alertStateStore);

        final long runStampSec = TimeUnit.SECONDS.convert(endTime, timeUnit);

        this.purge.purge(this.alertStateStore, false);

        try {
            persistState(alertStateStore, runStampSec);
        } catch (Exception e) {
            LOG.error("Failed to persist state: alert_id={}, run_stamp_sec={}",
                    getAlertConfig().getAlertId(), runStampSec, e);
            Monitoring.get().reportStatePersistenceFailure(getAlertConfig());
        }
        return alertBag;
    }

    private void persistState(final AlertStateStore stateStore,
                              final long runStampSec) {
        LOG.info("id: {} Starting persisting of state", getAlertConfig().getAlertId());
        statePersistor.persist(getAlertConfig(), stateStore, runStampSec);
        LOG.info("id: {} Finished persisting of state", getAlertConfig().getAlertId());
    }

    public abstract AlertEventBag execute(
            final long endTime,
            final TimeUnit timeUnit,
            final AlertStateStore alertStateStore)
            throws AlertException;
}
