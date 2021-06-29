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

package net.opentsdb.horizon.alerts;

import net.opentsdb.horizon.alerts.monitor.Monitor;
import net.opentsdb.horizon.alerts.monitor.impl.SimpleRunnableMonitor;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.AlertConfigFetcher;
import net.opentsdb.horizon.alerts.heartbeat.HeartbeatReadable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class AlertDaemon implements HeartbeatReadable {
    
    private int daemonIndex = 0;

    private static final Logger LOG = LoggerFactory.getLogger(AlertDaemon.class);

    private ExecutorService executor = null;

    //Thread local set
    private Map<Long, Monitor> monitors = new ConcurrentHashMap<>();

    //Thread local futures
    private Map<Long,Future<Monitor>> monitorFutures = new ConcurrentHashMap<>();

    private ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    private AlertConfigFetcher alertConfigFetcher;

    private EnvironmentConfig environmentConfig = new EnvironmentConfig();

    public AlertDaemon(int daemonIndex, AlertConfigFetcher alertConfigFetcher) {
        this.daemonIndex = daemonIndex;
        executor = Executors.newWorkStealingPool(environmentConfig.getNumThreadsInAlertPool());
        this.alertConfigFetcher = alertConfigFetcher;

    }

    public ScheduledFuture<?> startDaemon() {
        LOG.info("Starting alert daemon: {} with {} threads",
                daemonIndex,
                environmentConfig.getNumThreadsInAlertPool());
        return service.scheduleAtFixedRate(this::doAlertRun,environmentConfig.getAlertDaemonInitialDelaySecs()
                ,environmentConfig.getAlertDaemonRunFreqSecs(),TimeUnit.SECONDS);
    }


    private void doAlertRun() {
            try {
                LOG.info("Starting a new Alert Run");
                //TODO: Refresh config - periodically can be moved to another thread.

                //TODO: This object should be cached

                Map<Long, AlertConfig> longAlertConfigMap = alertConfigFetcher.getAlertConfig();
                //Disable == delete since it makes no sense to keep state for disabled around.
                Map<Long,Long> monitorsToBeDeleted = new HashMap<>();

                Map<Long,Long> monitorsToBeDisabled = new HashMap<>();

                //LOG.info("Fetched config: " + longAlertConfigMap.toString());

                longAlertConfigMap.entrySet().stream()
                        .forEach(entry -> {

                            Long key = entry.getKey();
                            AlertConfig newAlertConfig = entry.getValue();

                            LOG.info("Fetched config for id: {} {}",key, newAlertConfig);

                            if(!newAlertConfig.isEnabled()) {
                                if(monitors.containsKey(key) ||
                                        monitorFutures.containsKey(key)) {
                                    //Clean up disabled monitors
                                    monitorsToBeDisabled.put(key, key);
                                } else {
                                    //New monitor, but disabled. Do nothing.
                                    return;
                                }
                            }

                            Monitor existingMonitor = monitors.get(key);

                            if (existingMonitor != null) {
                                AlertConfig existingAlertConfig = existingMonitor.getAlertConfig();
                                if (existingAlertConfig.getHash() == newAlertConfig.getHash()) {
                                    return;
                                } else if (existingAlertConfig.updatable(newAlertConfig)) {
                                    existingMonitor.updateAlertConfig(newAlertConfig);
                                    return;
                                } else {
                                    // Create new Alert Config
                                    // Check if execution is in progress
                                    final Future<Monitor> monitorFuture = monitorFutures.get(key);

                                    if (monitorFuture == null) {
                                        LOG.error("Monitor future is null for id," +
                                                " let it be scheduled: {}", key);
                                        existingMonitor.finish(false);
                                    } else {

                                        Monitor monitor = null;
                                        try {
                                            monitor = monitorFuture.get(environmentConfig.getMonitorCheckTimeoutMs(), TimeUnit.MILLISECONDS);

                                            monitor.finish(true);
                                        } catch (InterruptedException e) {
                                            LOG.error("Interrupted when checking future", e);
                                            cancelMonitorExecution(monitorFuture, key);
                                            existingMonitor.finish(false);
                                        } catch (ExecutionException e) {
                                            LOG.error("Monitor execution failed for {} isDone {}", key,
                                                    monitorFuture.isDone(), e);
                                            cancelMonitorExecution(monitorFuture, key);
                                            existingMonitor.finish(false);

                                        } catch (TimeoutException e) {
                                            LOG.error("Monitor still in progress");
                                            cancelMonitorExecution(monitorFuture, key);
                                            existingMonitor.finish(false);
                                        }
                                    }
                                    final long defaultLastRunTimeSecs =
                                            AlertUtils.getDefaultLastRunTimeSecs();
                                    // Pick up from where we left off.
                                    LOG.info("Update with new " +
                                            "monitor for id: {} " +
                                            "with time: {}", key,
                                            defaultLastRunTimeSecs);
                                    monitors.put(key,
                                            new SimpleRunnableMonitor(newAlertConfig.createAlertExecutor(),
                                                    newAlertConfig.createNotificationProcessor(),
                                                    defaultLastRunTimeSecs));
                                    //Remove old monitor future
                                    monitorFutures.remove(key);
                                    return;
                                }

                            }
                            final long defaultLastRunTimeSecs =
                                    AlertUtils.getDefaultLastRunTimeSecs();
                            LOG.info("Create new " +
                                            "monitor for id: {} " +
                                            "with time: {}", key,
                                    defaultLastRunTimeSecs);
                            monitors.put(key,
                                        new SimpleRunnableMonitor(newAlertConfig.createAlertExecutor(),
                                                newAlertConfig.createNotificationProcessor(),
                                                defaultLastRunTimeSecs));
                            //Remove old monitor future
                            monitorFutures.remove(key);

                        });
                

                LOG.debug("Monitors: {}", monitors.toString());
                LOG.debug("Monitor Futures: {}", monitorFutures.toString());

                //Remove disabled monitors
                //disable is same as delete as it doesnt make sense to store state yet
                purgeMonitors(monitorsToBeDisabled);

                monitors.entrySet().stream()
                        .forEach(longMonitorEntry -> {

                            final Long key = longMonitorEntry.getKey();

                            final Monitor existingMonitor = longMonitorEntry.getValue();

                            final Future<Monitor> future = monitorFutures.get(key);

                            if (future != null) {
                                Monitor monitor = null;
                                try {
                                    monitor = future.get(10, TimeUnit.MILLISECONDS);

                                    monitor.finish(true);
                                } catch (InterruptedException e) {
                                    LOG.error("Interrupted when checking future.. doing nothing " + key, e);
                                    Monitoring.get().reportDelay(existingMonitor);
                                    return;
                                } catch (ExecutionException e) {
                                    LOG.error("Monitor execution failed: " + key, e);

                                    final AlertConfig alertConfig;
                                    if(Objects.nonNull(existingMonitor)) {
                                        alertConfig = existingMonitor.getAlertConfig();
                                    } else {
                                        alertConfig = null;
                                    }
                                    if(Objects.nonNull(alertConfig) &&
                                                Objects.nonNull(alertConfig.getAlertType())
                                                    && Objects.nonNull(alertConfig.getAlertType().getString())) {
                                        Monitoring.get().
                                                incMonitorExecutionFailures(
                                                        alertConfig.getNamespace(),
                                                        alertConfig.getAlertId(),
                                                        alertConfig.getAlertType().getString());
                                    }
                                    cancelMonitorExecution(future,key);
                                    //Retry
                                    existingMonitor.finish(false);
                                } catch (TimeoutException e) {
                                    LOG.error("Monitor still in progress.. will wait till it completes " + key);
                                    Monitoring.get().reportDelay(existingMonitor);
                                    return;
                                } catch (Throwable t) {
                                    LOG.error("Monitor error " + key, t);
                                }
                            }
                            LOG.info("Checking Time to run for id: {}",
                                    existingMonitor.getAlertConfig().getAlertId());
                            Monitoring.get().reportDelay(existingMonitor);

                            if(!longAlertConfigMap.containsKey(key)) {
                                // Delete monitor, dont submit a new run
                                monitorsToBeDeleted.put(key,key);
                            } else if (existingMonitor.isTimeToRun()) {
                                LOG.info("Time to run for id: {} {}",
                                        existingMonitor.getAlertConfig().getAlertId(),
                                        existingMonitor.getLastRuntimeInSecs());
                                existingMonitor.prep();
                                Callable<Monitor> callableMonitor = new RunMonitorCallable(existingMonitor);
                                final Future<Monitor> monitorFuture = executor.submit(callableMonitor);
                                monitorFutures.put(key, monitorFuture);
                            } else {
                                LOG.info("Not time to run for id: {} {}",
                                        existingMonitor.getAlertConfig().getAlertId(),
                                        existingMonitor.getLastRuntimeInSecs());
                            }

                        });
                //delete
                purgeMonitors(monitorsToBeDeleted);
                monitors.entrySet()
                        .stream()
                        .forEach(entry -> Monitoring.get().gaugeNumberOfMonitors(entry.getKey(),entry.getValue().getAlertConfig().getNamespace()));

            } catch (Throwable t) {
                LOG.error("Daemon thread almost died",t);
                Monitoring.get().countThreadAlmostDied();
            }



    }

    private void purgeMonitors(Map<Long, Long> monitorsToBePurged) {
        monitorsToBePurged.keySet().stream().forEach(key -> {
            if(monitorFutures.containsKey(key)) {
                cancelMonitorExecution(monitorFutures.get(key),key);
                monitorFutures.remove(key);
            }
            if(monitors.containsKey(key)) {
                monitors.remove(key);
            }
        });
    }

    private void cancelMonitorExecution(Future<Monitor> future, Long key) {
        if(!future.isDone()) {
            try {
                future.cancel(true);
            } catch (Exception e1) {
                LOG.error("Trying to cancel monitor failed: " + key, e1);
            }
        }
    }

    @Override
    public Map<Long, String> getIdToName() {

        final Map<Long,String> map = new HashMap<>();

        for(long id : monitors.keySet()) {
            final Monitor monitor = monitors.get(id);
            final String name;
            final String namespace;
            if(monitor != null) {
                final AlertConfig alertConfig = monitor.getAlertConfig();
                if(alertConfig != null) {
                    name = alertConfig.getAlertName();
                    namespace = alertConfig.getNamespace();
                } else {
                    name = null;
                    namespace = null;
                }

            } else {
                name = null;
                namespace = null;
            }
            if(name != null && namespace != null) {
                map.put(id, String.format("Namespace : %s  , name : %s",namespace,name));
            }

        }
        return map;
    }


    private class RunMonitorCallable implements Callable<Monitor> {

        private Monitor monitor;

        public RunMonitorCallable(Monitor monitor) {

            this.monitor = monitor;
        }

        @Override
        public Monitor call() throws Exception {
            try {
                this.monitor.execute();
                return this.monitor;
            } catch (Throwable t) {
                LOG.error("Error running the monitor",t);
                throw t;
            }
        }
    }
}
