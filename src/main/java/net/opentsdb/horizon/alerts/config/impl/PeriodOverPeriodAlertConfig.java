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

import java.util.SortedMap;

import com.fasterxml.jackson.databind.JsonNode;

import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.enums.AlertType;
import net.opentsdb.horizon.alerts.enums.ThresholdUnit;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.processor.impl.UpdatableExecutorWrapper;

public class PeriodOverPeriodAlertConfig extends AlertConfig {

    private static final String KEY_THRESHOLD = "threshold";
    private static final String KEY_PERIOD_OVER_PERIOD = "periodOverPeriod";
    private static final String KEY_WARN_UPPER_THRESHOLD = "warnUpperThreshold";
    private static final String KEY_BAD_UPPER_THRESHOLD = "badUpperThreshold";
    private static final String KEY_UPPER_THRESHOLD_TYPE = "upperThresholdType";
    private static final String KEY_WARN_LOWER_THRESHOLD = "warnLowerThreshold";
    private static final String KEY_BAD_LOWER_THRESHOLD = "badLowerThreshold";
    private static final String KEY_LOWER_THRESHOLD_TYPE = "lowerThresholdType";
    private static final String KEY_PERIOD = "period";
    private static final String KEY_METRIC_ID = "metricId";
    private static final String EGADS_PREFIX = "egads-";

    private double upperWarnThreshold = Double.NaN;
    private double upperBadThreshold = Double.NaN;
    private ThresholdUnit upperThresholdUnit;
    private double lowerWarnThreshold = Double.NaN;
    private double lowerBadThreshold = Double.NaN;
    private ThresholdUnit lowerThresholdUnit;

    private long baselinePeriodSec;
    private String egadsNodeId;

    public PeriodOverPeriodAlertConfig(String namespace,
                                       AlertType alertType,
                                       long alertId,
                                       long last_modified) {
        super(namespace, alertType, alertId, last_modified);
    }

    public double getUpperWarnThreshold() {
        return upperWarnThreshold;
    }

    public double getUpperBadThreshold() {
        return upperBadThreshold;
    }

    public ThresholdUnit getUpperThresholdUnit() {
        return upperThresholdUnit;
    }

    public double getLowerWarnThreshold() {
        return lowerWarnThreshold;
    }

    public double getLowerBadThreshold() {
        return lowerBadThreshold;
    }

    public ThresholdUnit getLowerThresholdUnit() {
        return lowerThresholdUnit;
    }

    public long getBaselinePeriodSec() {
        return baselinePeriodSec;
    }

    public String getEgadsNodeId() {
        return egadsNodeId;
    }

    @Override
    public void parseAlertSpecific(final JsonNode root) {
        JsonNode cfgNode = root.get(KEY_THRESHOLD).get(KEY_PERIOD_OVER_PERIOD);

        upperWarnThreshold = cfgNode.get(KEY_WARN_UPPER_THRESHOLD).asDouble(Double.NaN);
        upperBadThreshold = cfgNode.get(KEY_BAD_UPPER_THRESHOLD).asDouble(Double.NaN);
        upperThresholdUnit = ThresholdUnit.valueOf(
                cfgNode.get(KEY_UPPER_THRESHOLD_TYPE).asText().toUpperCase()
        );
        lowerWarnThreshold = cfgNode.get(KEY_WARN_LOWER_THRESHOLD).asDouble(Double.NaN);
        lowerBadThreshold = cfgNode.get(KEY_BAD_LOWER_THRESHOLD).asDouble(Double.NaN);
        lowerThresholdUnit = ThresholdUnit.valueOf(
                cfgNode.get(KEY_LOWER_THRESHOLD_TYPE).asText().toUpperCase()
        );

        baselinePeriodSec = cfgNode.get(KEY_PERIOD).asLong();
        egadsNodeId = EGADS_PREFIX + cfgNode.get(KEY_METRIC_ID).asText();
    }

    @Override
    public UpdatableExecutorWrapper<PeriodOverPeriodAlertConfig> createAlertExecutor() {
        return new UpdatableExecutorWrapper<>(this);
    }

    @Override
    protected String getDefaultQueryType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValid() throws AlertException {
        if (getAlertId() != 0
                && getAlertName() != null
                && getAlertType() == AlertType.PERIOD_OVER_PERIOD
                && getNamespace() != null
                && getQueryJson() != null
                && !(Double.isNaN(upperBadThreshold) &&
                    Double.isNaN(upperWarnThreshold) &&
                    Double.isNaN(lowerBadThreshold) &&
                    Double.isNaN(lowerWarnThreshold))) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean validateConfig() {
        // This method is called in the super.isValid(). Since we override the
        // later above, this one should not be called.
        throw new UnsupportedOperationException();
    }

    @Override
    public AlertEvent createAlertEvent(long hash,
                                       String tsField,
                                       SortedMap<String, String> tags,
                                       AlertState alertType) {
        throw new UnsupportedOperationException();
    }
}
