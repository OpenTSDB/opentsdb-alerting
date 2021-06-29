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

package net.opentsdb.horizon.alerts.monitor.impl;

import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.monitor.Monitor;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.processor.ControlledAlertExecutor;
import net.opentsdb.horizon.alerts.processor.Overseer;
import net.opentsdb.horizon.alerts.processor.notification.NotificationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class SimpleRunnableMonitor implements Monitor {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleRunnableMonitor.class);

    private ControlledAlertExecutor<AlertEventBag, AlertConfig> controlledAlertExecutor;

    private Overseer overseer;

    private NotificationProcessor enrichmentProcessor;

    private volatile boolean inProgress;

    public SimpleRunnableMonitor(ControlledAlertExecutor<AlertEventBag,AlertConfig> controlledAlertExecutor,
                                 NotificationProcessor enrichmentProcessor) {
        this(controlledAlertExecutor,enrichmentProcessor,AlertUtils.getDefaultLastRunTimeSecs());
    }

    public SimpleRunnableMonitor(ControlledAlertExecutor<AlertEventBag,AlertConfig> controlledAlertExecutor,
                                 NotificationProcessor enrichmentProcessor,long lastRunTime) {
        this.controlledAlertExecutor = controlledAlertExecutor;
        this.enrichmentProcessor = enrichmentProcessor;
        this.overseer = new Overseer(AlertUtils.dataFrequencyInSecs,lastRunTime);
    }

    @Override
    public boolean inProgress() {
        return this.inProgress;
    }

    @Override
    public boolean isTimeToRun() {

        LOG.info("");

        return overseer.isTimeToRun();
    }

    @Override
    public void prep() {
        this.inProgress = true;
    }

    @Override
    public void execute() throws AlertException {
        overseer.startNewRun();
        final long alertId = controlledAlertExecutor.getAlertConfig().getAlertId();
        final long currentRunTimeSecs = overseer.getCurrentRunTimeSecs();
        LOG.info("Starting monitor run for {} {}",alertId,currentRunTimeSecs);
        //LOG.info("m: "+ controlledAlertExecutor + " e: "+ enrichmentProcessor);
        final AlertEventBag eventBag = controlledAlertExecutor.evaluate(overseer.getCurrentRunTimeSecs(),TimeUnit.SECONDS);

        LOG.info("Result of monitor run for {} {} : {} ",alertId,currentRunTimeSecs,eventBag);
        AlertUtils.reportAlertStats(eventBag,
                controlledAlertExecutor.getAlertConfig());
        enrichmentProcessor.process(eventBag);
        overseer.endRun();

    }

    @Override
    public void finish(boolean needRetry) {
        if(!needRetry) {
            overseer.endRun();
        }
        this.inProgress = false;
    }

    @Override
    public AlertConfig getAlertConfig() {
        return controlledAlertExecutor.getAlertConfig();
    }

    @Override
    public long getLastRuntimeInSecs() {
        return overseer.getLastRuntimeSecs();
    }

    @Override
    public void updateAlertConfig(AlertConfig alertConfig) {
        controlledAlertExecutor.update(alertConfig);
        enrichmentProcessor.setConfig(alertConfig.getNotificationConfig());
        enrichmentProcessor.setAlertConfig(alertConfig);
    }

    @Override
    public String toString() {

        return ("Monitor for: "+ getAlertConfig().toString());

    }

}
