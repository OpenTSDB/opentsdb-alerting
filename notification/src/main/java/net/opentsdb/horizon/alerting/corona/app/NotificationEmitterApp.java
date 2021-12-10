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

import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;

public class NotificationEmitterApp {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(NotificationEmitterApp.class);

    private static final String DEFAULT_PATH =
            "/etc/alert-processor-emitter.conf";

    private static final String ENV_VARIABLE =
            "ALERT_PROCESSOR_EMITTER_CONFIG";

    private static final String SYSTEM_PROPERTY =
            ENV_VARIABLE.toLowerCase().replace("_", ".");

    /* ------------ Static Methods ------------ */

    public static void main(String[] argv)
    {
        final NotificationEmitterConfig config =
                NotificationEmitterConfig.builder()
                        .setDefaultPath(DEFAULT_PATH)
                        .trySystemProperty(SYSTEM_PROPERTY)
                        .tryEnvVariable(ENV_VARIABLE)
                        .tryFromArgs(argv, 0)
                        .setVerbose(true)
                        .build();

        // TODO: toString() is used to validate the config. Makes
        //       sense to do it somewhere else.
        LOG.info("Configuration: \n{}", config.toString());

        // TODO: container-id tag is based on knowledge that we run in a
        //       container, and by default it is a short-hash.
        AppMonitor.initialize(AppMonitor.config()
                .setNamespace(config.getMonitoringNamespace())
                .setApplication(
                        config.getEmitterType() + "." + config.getMonitoringApplication()
                )
                .setHost(config.getMonitoringHost())
                .addStaticTag(
                        "container-name",
                        config.getMonitoringContainerName()
                )
        );

        Views.initialize(Views.config()
                .setHorizonUrl(config.getViewsHorizonUrl())
                .setSplunkUrl(config.getViewsSplunkUrl())
                .setSplunkIndex(config.getViewsSplunkIndex())
                .setSplunkLocale(config.getViewsSplunkLocale())
        );

        try {
            final NotificationEmitter emitter = new NotificationEmitter(config);
            LOG.info("NotificationEmitter starting");
            emitter.start();
            LOG.info("NotificationEmitter finished");
        } catch (Exception e) {
            LOG.error("Exception starting NotificationEmitter", e);
        }

        // Force shutdown to make sure Kafka's reader is dead dead.
        System.exit(0);
    }
}
