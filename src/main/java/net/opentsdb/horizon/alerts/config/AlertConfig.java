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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import net.opentsdb.horizon.alerts.state.purge.Purge;
import net.opentsdb.horizon.alerts.state.purge.impl.NoOpPurgePolicy;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import net.opentsdb.horizon.alerts.processor.ControlledAlertExecutor;
import net.opentsdb.horizon.alerts.processor.notification.NotificationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.enums.AlertType;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import net.opentsdb.horizon.alerts.state.AlertStateStores;

import com.fasterxml.jackson.databind.JsonNode;

@Getter
@Setter
@ToString
public abstract class AlertConfig {

    private AlertType alertType;

    private NotificationConfig notificationConfig;

    private final long alertId;

    private String alertName;

    private long last_modified;

    private String namespace;

    private boolean enabled = true;

    private List<String> labels = new ArrayList<>();

    private Map<String, String> miscFields = new HashMap<>();

    private long hash;

    private int queryIndex = 0;

    private JsonNode queryJson = null;

    private String md5BaseQueryString = null;

    private double badThreshold;
    /**
     * Only recovery and be inferred.
     */
    protected double recoveryThreshold;

    private double warnThreshold;

    private boolean hasWarnThreshold = false;

    private boolean hasBadThreshold = false;

    /**
     * Only recovery and be inferred.
     */
    protected boolean hasRecoveryThreshold = false;

    private int nagIntervalInSecs = AlertUtils.DO_NOT_NAG;

    private boolean isMissingEnabled = false;

    private TransitionConfig transitionConfig = null;

    private int evaluationDelayInMins = 0;

    private static final Logger LOG = LoggerFactory.getLogger(AlertConfig.class);

    protected AlertConfig(String namespace, AlertType alertType, long alertId, long last_modified) {
        this.namespace = namespace;
        this.alertId = alertId;
        this.alertType = alertType;
        this.last_modified = last_modified;
        hash = AlertUtils.getXXHash(alertId, last_modified);
    }

    //Queries or any threshold config
    public abstract void parseAlertSpecific(JsonNode jsonNode);

    public abstract <K extends AlertConfig> ControlledAlertExecutor<AlertEventBag, K> createAlertExecutor();

    public NotificationProcessor createNotificationProcessor() {

        return NotificationProcessorFactory.createChain(this);
    }

    public long getHash() {
        return hash;
    }

    public String getNamespace() {
        return namespace;
    }

    public int hashcode() {
        return (int) alertId;
    }

    @Override
    public boolean equals(Object that) {
        if (that != null) {
            if (that.getClass() == this.getClass()) {
                AlertConfig thatC = (AlertConfig) that;
                if (alertId == thatC.alertId) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isValid() throws AlertException {

        if ((alertId != 0) &&
                (alertName != null) &&
                (namespace != null) &&
                !AlertUtils.isEmpty(queryJson.toString()) &&
                (hasBadThreshold || hasWarnThreshold || isMissingEnabled)) {
            if (validateConfig()) {
                // TODO: isValid() is called from a ConfigFetcher, which is
                //       used when we need to bootstrap state, hence circular
                //       dependency. In general, configs should be validated
                //       before they are even fed to the executor.
                //       Moreover, a config should not create an executor,
                //       there should be some i.nstance which takes a config,
                //       maybe bootstrapped state, and then creates an executor
                return true; // createAlertExecutor().validateConfigs();
            }
        }
        return false;
    }

    public AlertStateStore createAlertStateStore() {

        if (isMissingEnabled) {
            return AlertStateStores.withTransitionsAndMissing(String.valueOf(getAlertId()),
                    nagIntervalInSecs, transitionConfig);
        } else {
            return AlertStateStores.withTransitions(String.valueOf(getAlertId()),
                    nagIntervalInSecs, transitionConfig);
        }
    }

    /**
     * Whether a config can be updated without reset
     *
     * @param alertConfig
     * @return
     */
    public boolean updatable(AlertConfig alertConfig) {
        if (alertConfig.getClass() == this.getClass()) {
            if (alertConfig.getAlertId() == this.alertId) {
                if (alertConfig.namespace.equals(this.namespace)) {
                    if (md5BaseQueryString != null && alertConfig.md5BaseQueryString != null) {
                        if (md5BaseQueryString.equals(alertConfig.md5BaseQueryString)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected abstract String getDefaultQueryType();

    /**
     * Is a valid config
     *
     * @return
     * @throws AlertException
     */
    protected abstract boolean validateConfig() throws AlertException;

    public abstract AlertEvent createAlertEvent(final long hash,
                                                final String tsField,
                                                final SortedMap<String, String> tags,
                                                final AlertState alertType);

    public boolean storeIdentity() {
        return isMissingEnabled;
    }

    public Purge createPurge() {

        return Purge.PurgeBuilder.create()
                .addPolicy(new NoOpPurgePolicy())
                .build();

    }

}
