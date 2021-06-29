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

package net.opentsdb.horizon.alerts.config.impl;


import com.fasterxml.jackson.databind.JsonNode;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.AlertConfigFields;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.enums.AlertType;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.processor.impl.UpdatableExecutorWrapper;
import net.opentsdb.horizon.alerts.state.purge.Purge;
import net.opentsdb.horizon.alerts.state.purge.impl.HealthCheckPurgePolicy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.SortedMap;

import static net.opentsdb.horizon.alerts.config.impl.HealthCheckConfigFields.UNKNOWN_THRESHOLD;


@Getter
@Slf4j
public class HealthCheckConfig extends AlertConfig {

    private double unknownThreshold;

    private boolean hasUnknownThreshold;

    private int missingIntervalInSec;

    private int missingDataPurgeIntervalSec;

    private final EnvironmentConfig environmentConfig = new EnvironmentConfig();

    public HealthCheckConfig(String namespace, AlertType alertType, long alertId, long last_modified) {
        super(namespace, alertType, alertId, last_modified);
    }

    //Move to builder
    @Override
    public void parseAlertSpecific(JsonNode root) {

        final JsonNode threshold = root.get(AlertConfigFields.THRESHOLD);

        if(super.isMissingEnabled()) {
            missingIntervalInSec = threshold.get(HealthCheckConfigFields.MISSING_DATA_INTERVAL)
                    .asInt(30)*60 ;
        }

        if(threshold.hasNonNull(HealthCheckConfigFields.MISSING_DATA_PURGE_INTERVAL)) {
            missingDataPurgeIntervalSec = threshold.get(HealthCheckConfigFields.MISSING_DATA_PURGE_INTERVAL).asInt();
        } else {
            missingDataPurgeIntervalSec = environmentConfig.getHealthCheckPurgeIntervalMissing();
        }

        final JsonNode healthCheck = threshold.get(HealthCheckConfigFields.HEALTHCHECK);

        if(healthCheck.has(UNKNOWN_THRESHOLD)) {
            final JsonNode badNode = healthCheck.get(UNKNOWN_THRESHOLD);
            final String s = badNode.asText();

            if(!AlertUtils.isEmpty(s) && !badNode.isNull()) {
                hasUnknownThreshold = true;
                unknownThreshold = healthCheck.get(UNKNOWN_THRESHOLD).asDouble();
            }
        }

        //LOG.debug("Recovery threshold: {} Bad threshold: {} Warn threshold: {} Comparator: {} S: {} s: {}",
        //      recoveryThreshold,badThreshold,warnThreshold,comparisonString,(s != null && !s.isEmpty()),s);

    }

    @Override
    protected String getDefaultQueryType() {
        return HealthCheckConfigFields.QUERY_TYPE_AURA;
    }

    @Override
    public UpdatableExecutorWrapper<HealthCheckConfig> createAlertExecutor() {
        return new UpdatableExecutorWrapper<>(this);
    }


    @Override
    protected boolean validateConfig() throws AlertException {
        if(hasRecoveryThreshold) {
            return true;
        }
        return false;
    }

    @Override
    public AlertEvent createAlertEvent(long hash, String tsField,
                                       SortedMap<String, String> tags, AlertState signal) {
        return AlertUtils.createHealthCheckAlertEvent(tsField,tags,signal,this);
    }

    @Override
    public Purge createPurge() {

        return Purge.PurgeBuilder.create()
                .addPolicy(new HealthCheckPurgePolicy(this))
                .build();

    }

    @Override
    public String toString() {
        return (super.toString() + String.format(" missingIntervalInSec : %s ," +
                        " missingDataPurgeIntervalSec : %s, ",
                missingIntervalInSec,missingDataPurgeIntervalSec));
    }

}
