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

package net.opentsdb.horizon.alerting.corona.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;

public class AlertProcessorApp {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(AlertProcessorApp.class);

    private static final String DEFAULT_PATH = "/etc/alert-processor.conf";

    private static final String ENV_VARIABLE = "ALERT_PROCESSOR_CONFIG";

    private static final String SYSTEM_PROPERTY =
            ENV_VARIABLE.toLowerCase().replace("_", ".");

    /* ------------ Static Methods ------------ */

    public static void main(String[] argv)
    {
        // Try tell CertRefresher to wait for certs only up to a minute.
        if (!AppUtils.trySetAthenzKeyCertWaitTimeMin(1)) {
            LOG.info("Did not override CertRefresher configuration.");
        }

        final AlertProcessorConfig config = AlertProcessorConfig.builder()
                .setVerbose(true)
                .setDefaultPath(DEFAULT_PATH)
                .trySystemProperty(SYSTEM_PROPERTY)
                .tryEnvVariable(ENV_VARIABLE)
                .tryFromArgs(argv, 0)
                .build();

        // TODO: toString() is used to validate the config. Makes
        //       sense to do it somewhere else.
        LOG.info("Configuration: \n{}", config.toString());

        // TODO: container-id tag is based on knowledge that we run in a
        //       container, and by default it is a short-hash.
        AppMonitor.initialize(AppMonitor.config()
                .setNamespace(config.getMonitoringNamespace())
                .setApplication(config.getMonitoringApplication())
                .setHost(config.getMonitoringHost())
                .addStaticTag(
                        "container-name", config.getMonitoringContainerName()
                )
        );

        try {
            final AlertProcessor alertProcessor = new AlertProcessor(config);
            LOG.info("AlertProcessor starting");
            alertProcessor.start();
            LOG.info("AlertProcessor finished");
        } catch (Exception e) {
            LOG.error("Failed AlertProcessor creation/run", e);
        }

        // Force shutdown to make sure Kafka's reader is dead dead.
        System.exit(0);
    }
}
