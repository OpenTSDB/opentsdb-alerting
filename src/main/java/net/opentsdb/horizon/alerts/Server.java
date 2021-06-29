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

import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.AlertConfigFetcher;
import net.opentsdb.horizon.alerts.config.ConfigOrchestrator;
import net.opentsdb.horizon.alerts.heartbeat.HeartbeatServer;
import net.opentsdb.horizon.alerts.state.persistence.PulsarClientSingleton;
import net.opentsdb.horizon.alerts.state.persistence.PulsarStatePersistor;
import net.opentsdb.horizon.alerts.state.persistence.PulsarStateProvider;
import net.opentsdb.horizon.alerts.state.persistence.StatePersistor;
import net.opentsdb.horizon.alerts.state.persistence.StatePersistors;
import net.opentsdb.horizon.alerts.state.persistence.StateProviders;

import org.apache.pulsar.client.api.PulsarClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class Server {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private static Map<AlertDaemon, ScheduledFuture> daemonList = new HashMap<>();

    private static ScheduledFuture alertDaemon;

    /**
     * Start the heartbeat server
     * Start the daemon
     *
     * @param args
     */
    public static void main(String args[]) throws InterruptedException {
        loadConfigFile(args);
        EnvironmentConfig environmentConfig = new EnvironmentConfig();
        LOG.info("Starting Alert evaluation server with mirror set id : {} " +
                "number of mirror sets: {} mirror id: {} " +
                "number of mirrors: {} ",
                environmentConfig.getMirrorSetId(),
                environmentConfig.getNumberOfMirrorSets(),
                environmentConfig.getMirrorId(),
                environmentConfig.getNumberOfMirrors());

        final AlertConfigFetcher alertConfigFetcher
                = ConfigOrchestrator.getAlertConfigFetcher(0);

        tryInitializeStatePersistence(
                    environmentConfig,
                    alertConfigFetcher);

            AlertDaemon daemon = new AlertDaemon(0, alertConfigFetcher);
            alertDaemon = daemon.startDaemon();
    }

    private static void tryInitializeStatePersistence(
            final EnvironmentConfig config,
            final AlertConfigFetcher alertConfigFetcher) {
        tryBootstrapAlertsState(config, alertConfigFetcher);
    }

    private static void tryBootstrapAlertsState(
            final EnvironmentConfig config,
            final AlertConfigFetcher alertConfigFetcher) {

        boolean failed = false;

        if (config.isPulsarStatePersistenceEnabled()) {
            for (int i = 0; i < 2; i++) {
                try {
                    doCmsBootstrap(config, alertConfigFetcher);
                } catch (Exception e) {
                    LOG.warn("Failed to bootstrap attempt from CMS: attempt={}", i, e);
                    failed = true;
                    continue;
                }

                failed = false;
                break;
            }
            if(failed) {
                LOG.error("CMS bootstrap failed: state operations are set to noop.");
                return;
            }

            try {
                doInitializePersistors(config);
            } catch (Exception e) {
                LOG.error("CMS persistors initialization failed: using noop.", e);
            }
        }
    }

    // TODO: There is no way to stop the config fetcher.
    private static void doCmsBootstrap(
            final EnvironmentConfig config,
            final AlertConfigFetcher alertConfigFetcher) {
        final PulsarClient pulsarClient = PulsarClientSingleton.get();

        // Bootstrap state provider.
        final Collection<AlertConfig> configs =
                alertConfigFetcher.getAlertConfig().values();

        final PulsarStateProvider provider =
                PulsarStateProvider.create(
                        pulsarClient,
                        config.getPulsarTopicName(),
                        config.getPulsarConsumerName(),
                        // TODO: Just a reminder where the magical cutoff time comes from.
                        Instant.now().getEpochSecond()
                );

        final long startTimeMs = System.currentTimeMillis();
        try {
            provider.bootstrap(new ArrayList<>(configs));
        } finally {
            try {
                provider.close();
            } catch (Exception e) {
                LOG.warn("Failed to close state provider.", e);
            }
        }
        final long delta = System.currentTimeMillis() - startTimeMs;

        LOG.info("Bootstrap: delta_ms={}", delta);

        // Set state provider.
        StateProviders.initialize(provider);
    }

    private static void doInitializePersistors(final EnvironmentConfig config) {
        final int numberOfPersistors = 10;
        final PulsarClient pulsarClient = PulsarClientSingleton.get();

        // Create Pulsar persistors.
        final List<StatePersistor> persistors =
                new ArrayList<>(numberOfPersistors);
        for (int i = 0; i < numberOfPersistors; i++) {
            persistors.add(
                    PulsarStatePersistor.create(
                            pulsarClient,
                            config.getPulsarTopicName(),
                            config.getPulsarPersistorConfig()
                    )
            );
        }

        // Set state persistors.
        StatePersistors.initialize(persistors);
    }

    private static HeartbeatServer startHeartbeatServer(EnvironmentConfig environmentConfig) {
        LOG.info("Starting heartbeat server");
        final String hostname;

        if (EnvironmentConfig.IS_LOCAL) {
            hostname = "localhost";
        } else {
            try {
                hostname = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                throw new RuntimeException("Unable to start heartbeat server", e);
            }
        }
        HeartbeatServer heartbeatServer = new HeartbeatServer(hostname, environmentConfig.getPort(),
                environmentConfig.getHeartbeatServerPath());
        heartbeatServer.start();
        return heartbeatServer;
    }

    private static void loadConfigFile(String[] args) {

        final String file;

        if (args.length > 0 && Files.exists(Paths.get(args[0]))) {
            file = args[0];
        } else if (Files.exists(Paths.get(EnvironmentConfig.PROPERTIES_FILE))) {
            EnvironmentConfig.IS_LOCAL = false;
            file = EnvironmentConfig.PROPERTIES_FILE;
        } else if (Files.exists(Paths.get(EnvironmentConfig.LOCAL_PROPERTIES_FILE))) {
            EnvironmentConfig.IS_LOCAL = true;
            file = EnvironmentConfig.LOCAL_PROPERTIES_FILE;
        } else {
            throw new RuntimeException("Unable to find a config file.. so dying.");
        }

        EnvironmentConfig.seedFromFile(file);

        final EnvironmentConfig config = new EnvironmentConfig();

        if (config.isMirroringEnabled()) {
            if (config.getBaseHost() == null) {
                throw new AssertionError("Please fill the value for base host " +
                        "when mirroring is used.");
            }
        }

        LOG.info("Loaded config from file {}", file);
    }
}
