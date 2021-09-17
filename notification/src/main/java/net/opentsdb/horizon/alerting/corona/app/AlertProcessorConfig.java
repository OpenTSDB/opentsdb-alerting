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

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import net.opentsdb.horizon.alerting.AbstractConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.ConfigItem;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.configuration.Configuration;

/**
 * Configuration for the {@link AlertProcessor}
 */
public class AlertProcessorConfig extends AbstractConfig {

    private static final String TRUE = "true";

    private enum C implements ConfigItem {

        CONFIG_API_URL,
        CONFIG_API_CLIENT_MAX_CONN_PER_ROUTE(5),
        CONFIG_API_CLIENT_MAX_CONN_TOTAL(5),
        CONFIG_API_CLIENT_RETRY_MAX(3),
        CONFIG_API_CLIENT_CONNECTION_REQUEST_TIMEOUT_MS(10_000),
        CONFIG_API_CLIENT_CONNECT_TIMEOUT_MS(10_000),
        CONFIG_API_CLIENT_SOCKET_TIMEOUT_MS(10_000),
        CONFIG_API_CLIENT_HTTP_CERTIFICATE_PATH,
        CONFIG_API_CLIENT_HTTP_PRIVATE_KEY_PATH,
        CONFIG_API_CLIENT_HTTP_TRUST_STORE_PATH,
        CONFIG_API_CLIENT_HTTP_TRUST_STORE_PASSWORD,

        ALERT_PROCESSOR_KAFKA_AUTO_COMMIT_ENABLE(TRUE),
        ALERT_PROCESSOR_KAFKA_AUTO_OFFSET_RESET("largest"),
        ALERT_PROCESSOR_KAFKA_REBALANCE_BACKOFF_MS("60000"),
        ALERT_PROCESSOR_KAFKA_REBALANCE_RETRIES_MAX("20"),
        ALERT_PROCESSOR_KAFKA_ZOOKEEPER_CONNECTION_TIMEOUT_MS("120000"),
        ALERT_PROCESSOR_KAFKA_ZOOKEEPER_SESSION_TIMEOUT_MS("120000"),
        ALERT_PROCESSOR_KAFKA_TOPIC,
        ALERT_PROCESSOR_KAFKA_GROUP_ID,

        KAFKA_ZOOKEEPER_CONNECT,
        KAFKA_BROKER_LIST,

        MONITORING_NAMESPACE,
        MONITORING_APPLICATION("alert.processor"),

        SYNTHETIC_ALERT_IDS(Collections.singletonList("3214")),

        /**
         * Since we run in a container we will not be able to get the
         * actual hostname, hence specify it during chef deployment.
         */
        MONITORING_HOST,
        MONITORING_CONTAINER_NAME,

        EMAIL_KAFKA_TOPIC("test_corona_email"),
        OPSGENIE_KAFKA_TOPIC("test_corona_opsgenie"),
        SLACK_KAFKA_TOPIC("test_corona_slack"),
        OC_KAFKA_TOPIC("test_corona_oc"),
        WEBHOOK_KAFKA_TOPIC("test_corona_webhook"),

        ENABLE_PERIOD_OVER_PERIOD_ALERT_FILTER(TRUE),

        DEBUG(TRUE);

        final String key;

        final Object defaultValue;

        C(final Object defaultValue,
          final Function<String, String> parameterNameGenerator)
        {
            this.defaultValue = defaultValue;
            this.key = parameterNameGenerator.apply(name());
        }

        C(final Object defaultValue)
        {
            this(defaultValue, defaultKeyNameGenerator);
        }

        C()
        {
            this(null);
        }

        @Override
        public String key()
        {
            return key;
        }

        @Override
        public Object getDefault()
        {
            return defaultValue;
        }
    }

    @VisibleForTesting
    AlertProcessorConfig(final Configuration configuration)
    {
        super(configuration);
    }

    /* ------------ Methods ------------ */

    @Override
    protected ConfigItem[] getConfigValues()
    {
        return C.values();
    }

    // ------ ConfigDB ------ //

    String getConfigApiUrl()
    {
        return get(C.CONFIG_API_URL);
    }

    Integer getConfigApiClientMaxConnPerRoute()
    {
        return getInt(C.CONFIG_API_CLIENT_MAX_CONN_PER_ROUTE);
    }

    Integer getConfigApiClientMaxConnTotal()
    {
        return getInt(C.CONFIG_API_CLIENT_MAX_CONN_TOTAL);
    }

    Integer getConfigApiClientRetryMax()
    {
        return getInt(C.CONFIG_API_CLIENT_RETRY_MAX);
    }

    Integer getConfigApiClientConnectionRequestTimeoutMs()
    {
        return getInt(C.CONFIG_API_CLIENT_CONNECTION_REQUEST_TIMEOUT_MS);
    }

    Integer getConfigApiClientConnectTimeoutMs()
    {
        return getInt(C.CONFIG_API_CLIENT_CONNECT_TIMEOUT_MS);
    }

    Integer getConfigApiClientSocketTimeoutMs()
    {
        return getInt(C.CONFIG_API_CLIENT_SOCKET_TIMEOUT_MS);
    }

    String getConfigApiClientHttpCertificatePath()
    {
        return get(C.CONFIG_API_CLIENT_HTTP_CERTIFICATE_PATH);
    }

    String getConfigApiClientHttpPrivateKeyPath()
    {
        return get(C.CONFIG_API_CLIENT_HTTP_PRIVATE_KEY_PATH);
    }

    String getConfigApiClientHttpTrustStorePath()
    {
        return get(C.CONFIG_API_CLIENT_HTTP_TRUST_STORE_PATH);
    }

    String getConfigApiClientHttpTrustStorePassword()
    {
        return get(C.CONFIG_API_CLIENT_HTTP_TRUST_STORE_PASSWORD);
    }

    // ------ AlertProcessor ------ //

    String getAlertProcessorKafkaAutoCommitEnable()
    {
        return get(C.ALERT_PROCESSOR_KAFKA_AUTO_COMMIT_ENABLE);
    }

    String getAlertProcessorKafkaAutoOffsetReset()
    {
        return get(C.ALERT_PROCESSOR_KAFKA_AUTO_OFFSET_RESET);
    }

    String getAlertProcessorKafkaRebalanceBackoffMs()
    {
        return get(C.ALERT_PROCESSOR_KAFKA_REBALANCE_BACKOFF_MS);
    }

    String getAlertProcessorKafkaRebalanceRetriesMax()
    {
        return get(C.ALERT_PROCESSOR_KAFKA_REBALANCE_RETRIES_MAX);
    }

    String getAlertProcessorKafkaZookeeperConnectionTimeoutMs()
    {
        return get(C.ALERT_PROCESSOR_KAFKA_ZOOKEEPER_CONNECTION_TIMEOUT_MS);
    }

    String getAlertProcessorKafkaZookeeperSessionTimeoutMs()
    {
        return get(C.ALERT_PROCESSOR_KAFKA_ZOOKEEPER_SESSION_TIMEOUT_MS);
    }

    String getAlertProcessorKafkaTopic()
    {
        return get(C.ALERT_PROCESSOR_KAFKA_TOPIC);
    }

    String getAlertProcessorKafkaGroupId()
    {
        return get(C.ALERT_PROCESSOR_KAFKA_GROUP_ID);
    }

    // ------ Kafka ------ //

    List<String> getKafkaZookeeperConnect()
    {
        return getList(C.KAFKA_ZOOKEEPER_CONNECT);
    }

    String getKafkaZookeeperConnectAsString()
    {
        return String.join(",", getKafkaZookeeperConnect());
    }

    List<String> getKafkaBrokerList()
    {
        return getList(C.KAFKA_BROKER_LIST);
    }

    String getKafkaBrokerListAsString()
    {
        return String.join(",", getKafkaBrokerList());
    }

    // ------ Kafka Producers ------ //

    String getEmailKafkaTopic()
    {
        return get(C.EMAIL_KAFKA_TOPIC);
    }

    String getOpsgenieKafkaTopic()
    {
        return get(C.OPSGENIE_KAFKA_TOPIC);
    }

    String getSlackKafkaTopic()
    {
        return get(C.SLACK_KAFKA_TOPIC);
    }

    String getOcKafkaTopic()
    {
        return get(C.OC_KAFKA_TOPIC);
    }

    String getWebhookKafkaTopic()
    {
        return get(C.WEBHOOK_KAFKA_TOPIC);
    }

    // ------ Monitoring ------ //

    String getMonitoringNamespace()
    {
        return get(C.MONITORING_NAMESPACE);
    }

    String getMonitoringApplication()
    {
        return get(C.MONITORING_APPLICATION);
    }

    String getMonitoringHost()
    {
        return get(C.MONITORING_HOST);
    }

    String getMonitoringContainerName()
    {
        return get(C.MONITORING_CONTAINER_NAME);
    }

    List<String> getSyntheticAlertIds()
    {
        return getList(C.SYNTHETIC_ALERT_IDS);
    }

    String getEnablePeriodOverPeriodAlertFilter() {
        return get(C.ENABLE_PERIOD_OVER_PERIOD_ALERT_FILTER);
    }

    boolean isEnablePeriodOverPeriodAlertFilter() {
        return TRUE.equalsIgnoreCase(getEnablePeriodOverPeriodAlertFilter());
    }

    String getDebug() {
        return get(C.DEBUG);
    }

    boolean isDebug() {
        return TRUE.equalsIgnoreCase(getDebug());
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends AbstractConfig.Builder<AlertProcessorConfig, B>
    {

        private static final Logger LOG =
                LoggerFactory.getLogger(Builder.class);

        @Override
        protected AlertProcessorConfig build(Configuration configuration)
        {
            return new AlertProcessorConfig(configuration);
        }

        @Override
        protected Logger getLogger()
        {
            return LOG;
        }
    }

    private static class BuilderImpl extends Builder<BuilderImpl> {

        @Override
        protected BuilderImpl self()
        {
            return this;
        }
    }

    public static Builder<?> builder()
    {
        return new BuilderImpl();
    }
}
