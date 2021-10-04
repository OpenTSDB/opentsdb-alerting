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

package net.opentsdb.horizon.alerting.corona.monitoring;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ultrabrew.metrics.MetricRegistry;

/**
 * Singleton class for application monitoring.
 * <p>
 * An instance of this class is thread-safe, but initialization mechanism
 * is not. Hence the initialization should happen in your {@code main(...)}
 * method, before any other components are built.
 * <p>
 * Example usage:
 * <pre> {@code
 *      final AppMonitor.Config cfg =
 *              new AppMonitor.Config()
 *                  .setNamespace("OpenTSDB")
 *                  .setApplication("AlertProcessor")
 *                  .setHost("localhost")
 *                  .addStaticTag("containerId", "alert-processor-0");
 *      AppMonitor.initialize(cfg);
 * }
 * </pre>
 */
public class AppMonitor {

    /* ------------ Constants ------------ */

    private static final Logger LOG = LoggerFactory.getLogger(AppMonitor.class);

    /* ------------ Static Fields ------------ */

    private static volatile AppMonitor INSTANCE = null;

    /* ------------ Static Methods ------------ */

    public static AppMonitor get()
    {
        return INSTANCE;
    }

    /**
     * Initialize the {@code AppMonitor} singleton in a not thread-safe way.
     * <p>
     * Note: Not thread safe.
     *
     * @param config {@code AppMonitor} configuration.
     */
    public static void initialize(final Config config)
    {
        INSTANCE = new AppMonitor(config);
    }

    /* ------------ Fields ------------ */

    private final MetricRegistry registry;

    /* ------------ Constructor ------------ */

    private AppMonitor(final Config config)
    {
        Objects.requireNonNull(config, "config cannot be null");
        Objects.requireNonNull(config.namespace, "namespace cannot be null");
        Objects.requireNonNull(config.application,
                "application cannot be null");
        Objects.requireNonNull(config.host, "host cannot be null");

        this.registry = new MetricRegistry();

        if (config.staticTags == null) {
            config.staticTags = new HashMap<>();
        }
        config.staticTags.put("host", config.host);

        // TODO - setup reporters.
        //registry.addReporter(reporter);
    }

    /* ------------ Methods ------------ */

    // ------------ Utility methods ------------ //

    private void time(final String metricName,
                      final long duration,
                      final String... tags)
    {
        try {
            registry.timer(metricName).update(duration, tags);
        } catch (Exception e) {
            LOG.error("Reporting time for {}, duration {}",
                    metricName, duration, e);
        }
    }

    private void count(final String metricName,
                       final String... tags)
    {
        try {
            registry.counter(metricName).inc(tags);
        } catch (Exception e) {
            LOG.error("Reporting count for {}", metricName, e);
        }
    }

    private void count(final String metricName,
                       final int count,
                       final String... tags)
    {
        try {
            registry.counter(metricName).inc(count, tags);
        } catch (Exception e) {
            LOG.error("Reporting count for {}", metricName, e);
        }
    }

    private void gauge(final String metricName,
                       final long value,
                       final String... tags)
    {
        try {
            registry.gauge(metricName).set(value, tags);
        } catch (Exception e) {
            LOG.error("Reporting gauge for {}, value {}",
                    metricName, value, e);
        }
    }

    // ------------ Metric Methods ------------ //

    public void gaugeKafkaQueueSize(final long size, final String component)
    {
        gauge("kafka.queue.size", size, "component", component);
    }

    public void countKafkaMessageRead(final String component)
    {
        count("kafka.message.read", "component", component);
    }

    public void gaugeKafkaReadMesssageSize(final long size, final String topic) {
        gauge("kafka.read.message.size", size, "topic", topic);
    }

    public void countKafkaDeserializationSuccess(final String component,
                                                 final String className)
    {
        count("kafka.deserialization.success",
                "component", component,
                "class", className);
    }

    public void countKafkaDeserializationFailed(final String component)
    {
        count("kafka.deserialization.failed", "component", component);
    }

    public void countKafkaMessageWrite(final String topic)
    {
        count("kafka.message.write", "topic", topic);
    }

    public void countDispatchFailed(final String type) {
        count("dispatch.failed", "type", type);
    }

    /**
     * Count number of alerts we failed to send (after retries).
     *
     * @param namespace alert namespace.
     */
    public void countAlertSendFailed(final String namespace)
    {
        count("alert.send.failed", "namespace", namespace);
    }

    public void countAlertSendSuccess(final String namespace)
    {
        count("alert.send.success", "namespace", namespace);
    }

    public void gaugeAlertSentEventsSize(final long size, final String namespace)
    {
        gauge("alert.sent.events.size", size, "namespace", namespace);
    }

    public void countAlertWebhookSendFailed(final String namespace, long alert_id, String contactName)
    {
        count("alert.send.failed", "namespace", namespace, "alert_id", String.valueOf(alert_id), "contact_name", contactName);
    }

    public void countAlertWebhookSendSuccess(final String namespace, long alert_id, String contactName)
    {
        count("alert.send.success", "namespace", namespace, "alert_id", String.valueOf(alert_id), "contact_name", contactName);
    }

    public void timeAlertSendLatencyMs(final long latency)
    {
        time("alert.send.latency.ms", latency);
    }

    public void countAlertFormatFailed(final String namespace)
    {
        count("alert.format.failed", "namespace", namespace);
    }

    public void countAlertEventTooBig(final String namespace, final long alert_id)
    {
        count("alert.event.too.big", "namespace", namespace, "alert_id", String.valueOf(alert_id));
    }

    public void countAlertCancelFailed()
    {
        count("alert.cancel.failed");
    }

    public void countAlertCancelSuccess()
    {
        count("alert.cancel.success");
    }

    public void countAlertSnoozed(final int count, final String namespace) {
        count("alert.snoozed", count, "namespace", namespace);
    }

    public void gaugeGroupByStateSize(final long size)
    {
        gauge("groupby.state.size", size);
    }

    public void timeGroupByFlushTotalLatencyMs(final long latency)
    {
        time("groupby.flush.total.latency.ms", latency);
    }

    public void timeGroupByFlushItemLatencyMs(final long latency)
    {
        time("groupby.flush.item.latency.ms", latency);
    }

    /**
     * Counts number of alert groups that failed to be submitted to the
     * next processor.
     */
    public void countGroupBySubmitFailed()
    {
        count("groupby.submit.failed");
    }

    /**
     * Gauges sizes of resulting alert groups.
     *
     * @param size      number of alerts in the alert group
     * @param namespace namespace
     * @param alertId   alert id
     */
    public void gaugeGroupByAlertGroupSize(final long size,
                                           final String namespace,
                                           final long alertId)
    {
        gauge("groupby.alertgroup.size", size, "namespace", namespace, "alert_id", Long.toString(alertId));
    }

    /**
     * Counts number of alerts for which grouping rules could not
     * be retrieved.
     *
     * @param alertId alert id
     */
    public void countGroupKeyGeneratorStranded(final long alertId)
    {
        count("groupkey.generator.stranded", "alert_id", Long.toString(alertId));
    }

    /**
     * Counts number of alert groups for which contact or metadata
     * information is missing.
     *
     * @param alertId alert id
     */
    public void countAddresseeAppenderStranded(final long alertId)
    {
        count("addressee.appender.stranded", "alert_id", Long.toString(alertId));
    }

    public void countMetadataUpdateNamespaces(final boolean success)
    {
        count("metadata.update.namespaces",
                "status", success ? "ok" : "failed");
    }

    public void countMetadataUpdateNamespaceMeta(final boolean success, final String namespace)
    {
        count("metadata.update.namespace.meta",
                "status", success ? "ok" : "failed",
                "namespace", namespace);
    }

    /**
     * Count the number of synthetic alerts received. This metric is reported
     * in the alert processor and all the emitters.
     *
     * @param count number of alerts received
     * @param alertID synthetic alert id
     */
    public void countSyntheticAlertReceived(final int count, final long alertID)
    {
        count("synthetic_alert.received", count, "alert_id", Long.toString(alertID));
    }

    public void countEmitterAlertGroupSize(final int count,
                                           final String emitter,
                                           final String namespace,
                                           final long alertID)
    {
        count("emitter.alert_group.size", count,
                "emitter", emitter,
                "namespace", namespace,
                "alert_id", Long.toString(alertID)
        );
    }

    public void countOldAlertsDiscarded(final int count,
                                        final String emitter,
                                        final String namespace,
                                        final long alertID)
    {
        count("emitter.old.alerts.discarded", count,
                "emitter", emitter,
                "namespace", namespace,
                "alert_id", Long.toString(alertID)
        );
    }

    public void countYwrMessageKitDenied(final String namespace, final long alertId) {
        count("emitter.ywr.messagekit.denied",
                "namespace", namespace,
                "alert_id", Long.toString(alertId)
        );
    }

    /* ------------ Configuration ------------ */

    public static class Config {

        private String namespace;

        private String application;

        private String host;

        private Map<String, String> staticTags;

        private Config() { }

        public Config setNamespace(final String namespace)
        {
            this.namespace = namespace;
            return this;
        }

        public Config setApplication(final String application)
        {
            this.application = application;
            return this;
        }

        public Config setHost(final String host)
        {
            this.host = host;
            return this;
        }

        public Config setStaticTags(final Map<String, String> staticTags)
        {
            if (staticTags != null) {
                throw new IllegalStateException("static map are already set");
            }
            this.staticTags = staticTags;
            return this;
        }

        public Config addStaticTag(final String key, final String value)
        {
            if (staticTags == null) {
                staticTags = new HashMap<>();
            }
            staticTags.put(key, value);
            return this;
        }
    }

    public static Config config()
    {
        return new Config();
    }
}
