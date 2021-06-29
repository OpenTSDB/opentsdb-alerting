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
import net.opentsdb.horizon.alerts.monitor.Monitor;
import net.opentsdb.horizon.alerts.enums.AlertState;
import io.ultrabrew.metrics.*;
import io.ultrabrew.metrics.data.*;
import joptsimple.internal.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * TODO: Do a better job and reduce duplication of tags
 *
 */
public class Monitoring {

    private static final String APPLICATION_NAME = "alertengine";

    private static final String DEFAULT_SELF_REPORT_NAMESPACE = "NS";

    private static final String CONTAINER_NAME = "container_name";

    private static final String HOST = "host";
    private static final int HASH_TABLE_CAPACITY = 4096;

    private static final Logger LOG =
            LoggerFactory.getLogger(Monitoring.class);

    private static final
    Map<Class<? extends Metric>, Function<Metric, ? extends Aggregator>>
            FUNCTION_MAP = Collections.unmodifiableMap(
            new HashMap<Class<? extends Metric>,
                    Function<Metric, ? extends Aggregator>>() {{
                this.put(Counter.class, metric -> new CustomCounterAggregator((Counter) metric));
                this.put(Gauge.class, metric -> new CustomGaugeAggregator((Gauge) metric));
                this.put(GaugeDouble.class, metric -> new CustomGaugeDoubleAggregator((GaugeDouble) metric));
                this.put(Timer.class, metric -> new CustomTimerAggregator((Timer) metric));
            }});


    public void gaugeCollectorQueueSize(int size) {
        gauge("collector.queue.size",size);
    }

    public void gaugeNumberOfMonitors(Long key, String namespace) {
        gauge("numberOfMonitors",1, getTagsNamespaceAlertId(namespace, key));
    }

    public void countThreadAlmostDied() {
        count("threadAlmostDied");
    }

    public void incMonitorExecutionFailures(String namespace, long alertId, String alertConfigType) {
        count("monitorExecutionFailed",getTagsNamespaceAlertIdConfigType(namespace,
                alertId,alertConfigType));
    }

    public void reportStatePersistenceFailure(AlertConfig alertConfig) {
        count("statePersistenceFailed",getTagsNamespaceAlertIdConfigType(alertConfig.getNamespace(),
                alertConfig.getAlertId(),alertConfig.getAlertType().toString()));
    }

    public void countTSNotEnoughForEval(long alertId, String namespace, String alertConfigType,
                                        String state) {
        count("tsNotEnoughForEval", getTagsNamespaceAlertIdConfigTypeState(namespace,
                alertId, alertConfigType, state));
    }


    /* ---------------- Custom Metric Aggregators -------------- */

    private static final class CustomCounterAggregator
            extends BasicCounterAggregator {

        public CustomCounterAggregator(final Counter counter) {
            super(counter.id, HASH_TABLE_CAPACITY);
        }

    }

    private static final class CustomGaugeAggregator
            extends BasicGaugeAggregator {

        public CustomGaugeAggregator(final Gauge gauge) {
            super(gauge.id, HASH_TABLE_CAPACITY);
        }

    }

    private static final class CustomGaugeDoubleAggregator
            extends BasicGaugeDoubleAggregator {

        public CustomGaugeDoubleAggregator(final GaugeDouble gaugeDouble) {
            super(gaugeDouble.id, HASH_TABLE_CAPACITY);
        }

    }

    private static final class CustomTimerAggregator
            extends BasicTimerAggregator {

        public CustomTimerAggregator(final Timer timer) {
            super(timer.id, HASH_TABLE_CAPACITY);
        }

    }

    private final ConcurrentMap<String, MetricRegistry> registries;

    private final MetricRegistry selfReportRegistry;

    private static class Wrapper {
        public static final Monitoring MONITORING = new Monitoring();
    }

    private Monitoring(final String selfReportNamespace) {

        if (selfReportNamespace == null
                || selfReportNamespace.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "selfReportNamespace cannot be null nor empty.");
        }
        this.registries = new ConcurrentHashMap<>();
        this.selfReportRegistry = createAndAddRegistry(selfReportNamespace);
    }

    Monitoring(){this(DEFAULT_SELF_REPORT_NAMESPACE);}

    public static Monitoring get() {return Wrapper.MONITORING;}

    private synchronized MetricRegistry createAndAddRegistry(
            final String namespaceName) {
        if (registries.containsKey(namespaceName)) {
            return registries.get(namespaceName);
        }
        final EnvironmentConfig environmentConfig = new EnvironmentConfig();

        Map<String,String> STATIC_TAG_MAP =
                     new HashMap<String,String>(){{
                             this.put(CONTAINER_NAME,environmentConfig.getContainerName());
                             this.put(CONTAINER_NAME,environmentConfig.getContainerName());
                             if(!Strings.isNullOrEmpty(environmentConfig.getBaseHost())) {
                                     this.put(HOST,environmentConfig.getBaseHost());
                                 }
                         }};
        final MetricRegistry registry = new MetricRegistry();
        // TODO - configuration of reporters.
        //registry.addReporter(reporter);
        registries.put(namespaceName, registry);
        return registry;
    }

    private void time(final String metricName,
                      final long duration,
                      final String ... tags) {
        try {
            selfReportRegistry.timer(metricName).update(duration, tags);
        } catch (IllegalArgumentException iae) {
            LOG.error("Reporting time for {}, duration {}",
                    metricName, duration, iae);
        }
    }

    private void count(final String metricName,
                       final String ... tags) {
        try {
            selfReportRegistry.counter(metricName).inc(tags);
        } catch (IllegalArgumentException iae) {
            LOG.error("Reporting count for {}", metricName, iae);
        }
    }

    private void count(final String metricName,final long count,
                       final String ... tags) {
        try {
            selfReportRegistry.counter(metricName).inc(count,tags);
        } catch (IllegalArgumentException iae) {
            LOG.error("Reporting count for {}", metricName, iae);
        }
    }

    private void gauge(final String metricName,
                       final long value,
                       final String ... tags) {
        try {
            selfReportRegistry.gauge(metricName).set(value, tags);
        } catch (IllegalArgumentException iae) {
            LOG.error("Reporting gauge for {}, value {}",
                    metricName, value, iae);
        }
    }

    public void timeConfigFetchTime(long valueInNanoSecs,int mirrorId) {
        time("configFetchTime",valueInNanoSecs,new String[]{"mirrorId",String.valueOf(mirrorId)});
    }

    public void timeTsdQueryRunTime(long diffInMs, long alertId, String namespace) {
        time("tsdQueryLatency",diffInMs,getTagsNamespaceAlertId(namespace,alertId));
    }

    public void reportCollectorPostLatency(long diffInMs, String namespace, String type, int index) {
        time("collectorPostLatency",diffInMs,
                getTagsNamespaceTypeIndex(namespace, type, index));
    }

    public void reportCollectorSuccesfullyBytesPosted(int bytesPosted, String namespace, String type, int index) {
        count("collectorSuccessfullyPostedBytes", bytesPosted,
                getTagsNamespaceTypeIndex(namespace, type, index));
    }

    public void reportCollector5xxFailedBytesPosted(int bytesPosted, String namespace, String type, int index) {
        count("collectorFailed5xxBytes", bytesPosted,
                getTagsNamespaceTypeIndex(namespace, type, index));
    }

    public void reportCollector4xxFailedBytesPosted(int bytesPosted, String namespace, String type, int index) {
        count("collectorFailed4xxBytes", bytesPosted,
                getTagsNamespaceTypeIndex(namespace, type, index));
    }

    public void reportCollector2xx(int i, String namespace, String type, int index) {
        count("collector2xx", i,
                getTagsNamespaceTypeIndex(namespace, type, index));
    }

    public void reportCollector5xx(int i, String namespace, String type, int index) {
        count("collector5xx", i,
                getTagsNamespaceTypeIndex(namespace, type, index));
    }

    public void reportCollector4xx(int i, String namespace, String type, int index) {
        count("collector4xx", i,
                getTagsNamespaceTypeIndex(namespace, type, index));
    }

    public void incStatusesToBePosted(String namespace, String type, int index) {
        count("statusesToBePosted", getTagsNamespaceTypeIndex(namespace, type, index));
    }

    public void incDroppedCollectorEvent(String namespace, String type, int index) {
        count("droppedPostsToCollector", 1,
                getTagsNamespaceTypeIndex(namespace, type, index));
    }


    public void countProcessingSummariesError(long alertId, String namespace) {
        count("countProcessingSummariesError",1,getTagsNamespaceAlertId(namespace,alertId));
    }

    public void countProcessingNonSummariesError(long alertId, String namespace) {
        count("countProcessingNonSummariesError",1,getTagsNamespaceAlertId(namespace,alertId));
    }

    public void countHeartBeatProcessingNonSummariesError(long alertId, String namespace) {
        count("countHeartBeatProcessingNonSummariesError",1,getTagsNamespaceAlertId(namespace,alertId));
    }

    public void countPostProcessingError(long alertId, String namespace) {
        count("countPostProcessingError",1,getTagsNamespaceAlertId(namespace,alertId));
    }

    public void countTsdbErrors(int errors,long alertId,String namespace) {
        count("countTsdError",errors,getTagsNamespaceAlertId(namespace,alertId));
    }

    public void countTsdbRequests(int request,long alertId, String namespace) {
        count("numTsdRequests",request,getTagsNamespaceAlertId(namespace,alertId));
    }

    public void incAlertsWrittenToKafka(long alertId, String namespace) {
        count("alertsWrittenTokafka",1,getTagsNamespaceAlertId(namespace,alertId));
    }

    public void incStatusesWrittenToKafka(long alertId, String namespace) {
        count("alertsWrittenTokafka",1,getTagsNamespaceAlertId(namespace,alertId));
    }

    public void reportDelay(Monitor monitor) {
        final long lastRuntimeInSecs = monitor.getLastRuntimeInSecs();
        final long l = System.currentTimeMillis() / 1000;
        long delay = 0l;
        if((l - lastRuntimeInSecs) > 120 ) {
            delay = l - lastRuntimeInSecs - 120;
        }
        timeAlertEvaluationDelay(delay,monitor.getAlertConfig().getNamespace(),monitor.getAlertConfig().getAlertId());
    }

    public void timeAlertEvaluationDelay(long valueInSecs, String namespace, long alertId) {
        time("alertEvaluationDelay",valueInSecs,getTagsNamespaceAlertId(namespace,alertId));
    }

    public void countTimeseriesEvaluated(long count, String namespace, long alertId, AlertState type) {
        count("timeSeriesEvaluated",count,getTagsNamespaceAlertIdType(namespace,alertId,type));
    }

    public void countTotalStatusesSeen(int totalStatuses, String[] tags) {
        count("timeSeriesEvaluated",totalStatuses, tags);
    }

    public void countStatusNotUpdated(int notUpdated, String[] tags) {
        count("statusesNotUpdated",notUpdated, tags);
    }

    public void countMissingRecovery(int evaluatedMissingRecovery, String[] tags) {
        count("timeseriesEvaluatedMissingRecovery",evaluatedMissingRecovery, tags);
    }

    public void reportSeriesInState(long count, String namespace, long alertId, AlertState type) {
        switch (type) {
            case GOOD:
                count("timeSeriesInGoodState",count,getTagsNamespaceAlertIdType(namespace,alertId,AlertState.GOOD));
            break;
            case BAD:
                count("timeSeriesInBadState",count,getTagsNamespaceAlertIdType(namespace,alertId,AlertState.BAD));
                break;
            case WARN:
                count("timeSeriesInWarnState",count,getTagsNamespaceAlertIdType(namespace,alertId,AlertState.WARN));
                break;

        }
    }

    public void reportAlertsRaised(long count,String namespace, long alertId, AlertState type) {
        count("alertsRaised",count,getTagsNamespaceAlertIdType(namespace,alertId,type));
    }

    public void statePersistenceTotalTimeMs(long deltaMs, final String[] tags)  {
        time("statePersistanceTotalTime", deltaMs, tags);
    }

    public void statePersistencePayloadTimeMs(long deltaMs, final String[] tags)  {
        time("statePersistancePayloadTime", deltaMs, tags);
    }

    public void statePersistencePayloadCount(long count, final String[] tags) {
        count("statePersistencePayloadCount", count, tags);
    }

    public void statePersistencePayloadSize(long size, final String[] tags) {
        gauge("statePersistencePayloadSize", size, tags);
    }

    public void statePersistenceSerializedCount(long count, String[] tags) {
        count("statePersistenceTimeseriesCount", count, tags);
    }

    public void countPulsarReadOk() {
        count("pulsar.read.ok");
    }

    public void countPulsarReadErr() {
        count("pulsar.read.err");
    }

    public void countPulsarSendOk() {
        count("pulsar.send.ok");
    }

    public void countPulsarSendErr() {
        count("pulsar.send.err");
    }

    public void gaugePulsarSendPayloadSize(int size) {
        gauge("pulsar.send.payload.size", size);
    }

    public void timePulsarSend(long timeMs) {
        time("pulsar.send.time.ms", timeMs);
    }

    public void timeBootstrapTimeMs(long deltaMs) {
        time("state.bootstrap.time.ms", deltaMs);
    }

    public void countPurged(int purged, String[] tags) {
        count("purged", purged, tags);
    }

    public void countErrorPurge(int notPurged, String[] tags) { count("notPurgedDueToError", notPurged, tags);}

    private String[] getTagsNamespace(String namespace) {
        return new String[] {"namespace", namespace};
    }

    private String[] getTagsNamespaceTypeIndex(String namespace, String type, int index) {
        return new String[] {"namespace", namespace, "type", type, "index", String.valueOf(index)};
    }

    private String[] getTagsNamespaceAlertIdType(String namespace, long alertId, AlertState type) {

        return new String[] {"alertId", String.valueOf(alertId) ,"namespace", namespace,"alertType",type.name()};
    }

    private static String[] getTagsNamespaceAlertIdConfigType(String namespace, long alertId, String alertConfigType) {

        return new String[] {"alertId", String.valueOf(alertId) ,"namespace", namespace,"alertConfigType",alertConfigType};
    }

    private static String[] getTagsNamespaceAlertIdConfigTypeState(String namespace,
                                                                   long alertId,
                                                                   String alertConfigType,
                                                                   String state) {

        return new String[] {
                "alertId", String.valueOf(alertId) ,
                "namespace", namespace,
                "alertConfigType",alertConfigType,
                "alertState", state};
    }

    public static String[] getTagsNamespaceAlertIdConfigType(final AlertConfig alertConfig) {

        return getTagsNamespaceAlertIdConfigType(
                alertConfig.getNamespace(),
                alertConfig.getAlertId(),
                alertConfig.getAlertType().getString()
        );
    }

    public static String[] getTagsPurgeType(final AlertConfig alertConfig, String purgeType) {

        return new String[] {"alertId", String.valueOf(alertConfig.getAlertId()) ,
                "namespace", alertConfig.getNamespace(),
                "alertConfigType", alertConfig.getAlertType().getString(),
                "purgeType", purgeType};
    }

    public String[] getTagsNamespaceAlertId(String namespace, long alertId) {

        return new String[] {"alertId",String.valueOf(alertId) ,"namespace", namespace};
    }

}
