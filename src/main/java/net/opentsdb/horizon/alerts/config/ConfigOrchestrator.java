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

package net.opentsdb.horizon.alerts.config;

import com.google.common.annotations.VisibleForTesting;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.config.impl.FileConfigFetcher;
import net.opentsdb.horizon.alerts.config.impl.HorizonConfigFetcher;
import net.opentsdb.horizon.alerts.config.impl.NamespaceRejectFilter;
import net.opentsdb.horizon.alerts.config.impl.PartitionedConfigFetcher;
import net.opentsdb.horizon.alerts.http.AlertHttpsClient;
import net.opentsdb.horizon.alerts.http.AuthProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static net.opentsdb.horizon.alerts.EnvironmentConfig.ALL_NAMESPACES;

/**
 * Assumes initially that all mirrors are up.
 * Takes in new config after a few schedules.
 *
 *
 */
public class ConfigOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigOrchestrator.class);

    private static EnvironmentConfig environmentConfig = new EnvironmentConfig();

    public static AlertConfigFetcher getAlertConfigFetcher(int daemonid) {

        final AlertConfigFetcher seedAlertConfigFetcher;

        final NamespaceFetcher namespaceFetcher = getNamespaceFetcher();

        if(environmentConfig.getConfigSource().
                equalsIgnoreCase(EnvironmentConfig.DEFAULT_CONFIG_SOURCE)) {

            seedAlertConfigFetcher = new FileConfigFetcher(daemonid);
        } else {
            seedAlertConfigFetcher = HorizonConfigFetcher.builder()
                                        .withNamespaceFetcher(namespaceFetcher)
                                        .withClient(getConfigDBClient())
                                        .withEndpoint(
                                                environmentConfig
                                                .getConfigDbEndpoint()
                                        )
                                        .withMirrorId(
                                                environmentConfig
                                                .getMirrorId()
                                        )
                                        .build();

        }

        return PartitionedConfigFetcher.builder()
                .alertConfigFetcher(seedAlertConfigFetcher)
                .daemonid(daemonid)
                .totalNumberOfDaemons(environmentConfig.getNumberOfDaemons())
                .mirrorid(environmentConfig.getMirrorId())
                .totalNumberOfMirrors(environmentConfig.getNumberOfMirrors())
                .mirrorSetId(environmentConfig.getMirrorSetId())
                .totalNumberMirrorSets(environmentConfig.getNumberOfMirrorSets())
                .build();

    }

    static NamespaceFetcher getNamespaceFetcher() {

        if (environmentConfig.getNamespacesToReject().isPresent()) {
            logger.info("Building namespace reject filter with reject list: {}", environmentConfig.getNamespacesToReject().get());
        }
        return NamespaceRejectFilter.Builder.create()
                .seedNamespaceFetcher(getConfigBasedNamespaceFetcher())
                .withNamsespaceRejectList(() -> environmentConfig.getNamespacesToReject())
                .build();
    }

    @VisibleForTesting
    static NamespaceFetcher getConfigBasedNamespaceFetcher() {

        Optional<List<String>> namespacesToRunOpt = environmentConfig.getNamespacesToRun();
        if (namespacesToRunOpt.isPresent() &&
                namespacesToRunOpt.get().size() == 1 &&
                namespacesToRunOpt.get().get(0).equals(ALL_NAMESPACES)) {
            return NamespaceFetchers.getHorizonNamespaceFetcher(
                    environmentConfig.getConfigDbEndpoint(),
                    getConfigDBClient());
        } else if (namespacesToRunOpt.isPresent()) {
            return NamespaceFetchers.getStaticNamespaceFetcher(namespacesToRunOpt.get());
        } else {
            throw new RuntimeException("Error with config (namespaces_to_run). Need at least one namespace to run!");
        }
    }

    private static AlertHttpsClient getConfigDBClient() {
        return AlertHttpsClient.create(
                    AuthProviders.getAuthProvider(
                            environmentConfig.getConfigDbAuthProvider()

                    )
        );
    }


}
