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


import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.enums.ComparatorType;
import net.opentsdb.horizon.alerts.enums.SummaryType;
import net.opentsdb.horizon.alerts.enums.WindowSampler;

import java.util.Set;

public class SuppressMetricConfig {

    private String metricId;
    
    private int reportingInterval;
    
    private ComparatorType comparatorType;
    
    private WindowSampler sampler;

    private Double threshold;
    
    private SummaryType summaryType;
    
    private boolean isRequiredFullWindow;
    
    private long suppressMetricTemporalThreshold = 1;
    
    private Set<String> keySet;

    public SuppressMetricConfig(String metricId,
                                int reportingInterval,
                                ComparatorType comparatorType,
                                Double threshold,
                                WindowSampler sampler,
                                SummaryType summaryType,
                                boolean isRequiredFullWindow,
                                long suppressMetricTemporalThreshold,
                                Set<String> keySet) {
        this.metricId = metricId;
        this.reportingInterval = reportingInterval;
        this.comparatorType = comparatorType;
        this.threshold = threshold;
        this.sampler = sampler;
        this.summaryType = summaryType;
        this.isRequiredFullWindow = isRequiredFullWindow;
        this.suppressMetricTemporalThreshold = suppressMetricTemporalThreshold;
        this.keySet = keySet;
    }

    public String getMetricId() {
        return metricId;
    }

    public void setMetricId(String metricId) {
        this.metricId = metricId;
    }

    public int getReportingInterval() {
        return reportingInterval;
    }

    public void setReportingInterval(int reportingInterval) {
        this.reportingInterval = reportingInterval;
    }

    public ComparatorType getComparatorType() {
        return comparatorType;
    }

    public void setSuppressMetricType(ComparatorType suppressMetricType) {
        this.comparatorType = suppressMetricType;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public WindowSampler getSampler() {
        return sampler;
    }
    
    public SummaryType getSummaryType() {
        return summaryType;
    }

    public boolean getIsRequiredFullWindow() {
        return isRequiredFullWindow;
    }

    public long getSuppressMetricTemporalThreshold() {
        return suppressMetricTemporalThreshold;
    }

    public Set<String> getKeySet() {
        return keySet;
    }

    public static long getTemporalThreshold(String heartbeat, long slidingWindowInSecs) {
        switch (heartbeat) {
            case "at_least_once":
            case "all_of_the_times":
                return 1;
            case "in_total":
            case "in_avg":
                return slidingWindowInSecs / AlertUtils.dataFrequencyInSecs;
        }
        throw new AssertionError("Unsupported string for TemporalThreshold :" + heartbeat);
    }

    @Override
    public String toString() {
        return "SuppressMetricConfig{" +
                "metricId='" + metricId + '\'' +
                ", reportingInterval=" + reportingInterval +
                ", comparatorType=" + comparatorType +
                ", sampler=" + sampler +
                ", threshold=" + threshold +
                ", summaryType=" + summaryType +
                ", isRequiredFullWindow=" + isRequiredFullWindow +
                ", suppressMetricTemporalThreshold=" + suppressMetricTemporalThreshold +
                ", keySet=" + keySet +
                '}';
    }
}
