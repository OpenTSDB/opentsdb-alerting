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

package net.opentsdb.horizon.alerts.processor.impl;


import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.enums.AlertType;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.processor.ControlledAlertExecutor;
import net.opentsdb.horizon.alerts.query.QueryFactory;
import net.opentsdb.horizon.alerts.query.TimeBasedExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class UpdatableExecutorWrapper <T extends AlertConfig>
        implements ControlledAlertExecutor<AlertEventBag,T> {

    private volatile T alertConfig;

    private boolean loadConfig = true;

    private final TimeBasedExecutor<AlertEventBag,T> slidingWindowQuery;

    public UpdatableExecutorWrapper(T alertConfig) {
        this(alertConfig, QueryFactory.getSlidingWindow(alertConfig));
    }

    public UpdatableExecutorWrapper(T alertConfig, TimeBasedExecutor<AlertEventBag,T> slidingWindowQuery) {
        this.alertConfig = alertConfig;
        this.slidingWindowQuery = slidingWindowQuery;
    }

    @Override
    public AlertEventBag evaluate(long endTime, TimeUnit timeUnit) throws AlertException {

        if(loadConfig) {
            try {
                log.info("alert id: {} Prepping slidinngWindow processor",
                        alertConfig.getAlertId());
                slidingWindowQuery.prepAndValidate(alertConfig);
                loadConfig = false;
            } catch (Exception e) {
                log.error("Error prepping query: ",e);
            }
        }
        log.info("alert id: {} running the processopr",alertConfig.getAlertId());
        return slidingWindowQuery.execute(endTime, TimeUnit.SECONDS);
    }

    @Override
    public boolean validateConfigs() {
        try {
            return QueryFactory.getSlidingWindow(alertConfig).prepAndValidate(alertConfig);
        } catch (AlertException e) {
            log.error("Error validating config",e);
            return false;
        }
    }

    @Override
    public AlertType getExecutorType() {
        return alertConfig.getAlertType();
    }

    @Override
    public T getAlertConfig() {
        return alertConfig;
    }

    @Override
    public boolean update(T config) {
        this.alertConfig = config;
        this.loadConfig = true;
        return false;
    }
}
