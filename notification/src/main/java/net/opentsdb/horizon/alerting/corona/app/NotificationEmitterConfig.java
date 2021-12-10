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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import net.opentsdb.horizon.alerting.AbstractConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.configuration.Configuration;
import com.google.common.annotations.VisibleForTesting;

import net.opentsdb.horizon.alerting.ConfigItem;

public class NotificationEmitterConfig extends AbstractConfig {

    private enum C implements ConfigItem {

        EMITTER_TYPE,

        KAFKA_AUTO_COMMIT_ENABLE("true"),
        KAFKA_AUTO_OFFSET_RESET("smallest"),
        KAFKA_REBALANCE_BACKOFF_MS("60000"),
        KAFKA_REBALANCE_RETRIES_MAX("20"),
        KAFKA_ZOOKEEPER_CONNECTION_TIMEOUT_MS("120000"),
        KAFKA_ZOOKEEPER_SESSION_TIMEOUT_MS("120000"),
        KAFKA_TOPIC,
        KAFKA_GROUP_ID,
        KAFKA_ZOOKEEPER_CONNECT,

        MONITORING_NAMESPACE,
        MONITORING_APPLICATION("emitter"),
        MONITORING_HOST,
        MONITORING_CONTAINER_NAME,

        SYNTHETIC_ALERT_IDS(Collections.singletonList("3214")),

        TLS_ENABLED("false"),
        TLS_INSECURE_SKIP_VERIFY("false"),
        TLS_PRIVATE_KEY_PATH,
        TLS_CERTIFICATE_PATH,
        TLS_TRUST_STORE_PATH,
        TLS_TRUST_STORE_PASSWORD,

        CKMS_SECRET_GROUPS,

        DEBUG("true"),
        DEBUG_EMAIL_PREFIX("STAGE: "),
        DEBUG_OPSGENIE_API_KEY,
        DEBUG_SLACK_ENDPOINT,
        DEBUG_WEBHOOK_ENDPOINT("https://set.me"),

        // Email Emitter.

        EMAIL_CLIENT_SMTP_HOST("mta.opentsdb.net"),
        EMAIL_CLIENT_CONNECTION_TIMEOUT_MS(300),

        // OpsGenie Emitter.

        OPSGENIE_USER("OpenTSDB-Test"),
        OPSGENIE_SOURCE("OpenTSDB-Test"),
        OPSGENIE_MAX_SEND_ATTEMPTS(3),
        OPSGENIE_APIKEY_ENCRYPTOR_SECRET_KEY_NAME("tsdb.opsgenie.apikey.encryptor.secret"),

        // OC Emitter.

        OC_HOST("alerts.opentsdb.net"),
        OC_COLO("dc-east"),
        OC_MOOG_ENDPOINT,
        OC_MOOG_AUTH_TOKEN_KEY_NAME,
        OC_WHITELIST_NAMESPACES,
        OC_WHITELIST_IDS,
        OC_DENIED_NAMESPACES(Collections.emptyList()),

        // Prism Emitter

        PRISM_HOST("alerts.opentsdb.net"),
        PRISM_ENDPOINT("prism.opentsdb.net"),

        VIEWS_HORIZON_URL,
        VIEWS_SPLUNK_URL,
        VIEWS_SPLUNK_INDEX,
        VIEWS_SPLUNK_LOCALE("en_US");

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
    NotificationEmitterConfig(final Configuration configuration)
    {
        super(configuration);
    }

    /* ------------ Methods ------------ */

    @Override
    protected ConfigItem[] getConfigValues()
    {
        // Filter out API key from being displayed.
        return Arrays.stream(C.values())
                .filter(v -> v != C.DEBUG_OPSGENIE_API_KEY)
                .filter(v -> v != C.DEBUG_SLACK_ENDPOINT)
                .filter(v -> v != C.TLS_TRUST_STORE_PASSWORD)
                .filter(v -> v != C.OC_MOOG_AUTH_TOKEN_KEY_NAME)
                .toArray(ConfigItem[]::new);
    }

    String getEmitterType()
    {
        return get(C.EMITTER_TYPE);
    }

    // ------ Email Emitter ------ //

    String getEmailClientSmtpHost()
    {
        return get(C.EMAIL_CLIENT_SMTP_HOST);
    }

    int getEmailClientConnectionTimeoutMs()
    {
        return getInt(C.EMAIL_CLIENT_CONNECTION_TIMEOUT_MS);
    }

    // ------ OpsGenie Emitter ------ //

    String getOpsgenieUser()
    {
        return get(C.OPSGENIE_USER);
    }

    String getOpsgenieSource()
    {
        return get(C.OPSGENIE_SOURCE);
    }

    int getOpsgenieMaxSendAttempts()
    {
        return getInt(C.OPSGENIE_MAX_SEND_ATTEMPTS);
    }

    String getOpsgenieApikeyEncryptorSecretKeyName()
    {
        return get(C.OPSGENIE_APIKEY_ENCRYPTOR_SECRET_KEY_NAME);
    }

    // ------ OC Emitter ------ //

    String getOcHost()
    {
        return get(C.OC_HOST);
    }

    String getOcColo()
    {
        return get(C.OC_COLO);
    }

    String getOcMoogEndpoint()
    {
        return get(C.OC_MOOG_ENDPOINT);
    }

    String getOcMoogAuthTokenKeyName()
    {
        return get(C.OC_MOOG_AUTH_TOKEN_KEY_NAME);
    }

    List<String> getOcWhitelistNamespaces()
    {
        return getList(C.OC_WHITELIST_NAMESPACES);
    }

    List<String> getOcWhitelistIds()
    {
        return getList(C.OC_WHITELIST_IDS);
    }

    List<String> getOcDeniedNamespaces()
    {
        return getList(C.OC_DENIED_NAMESPACES);
    }

    // ------ Prism Emitter ------ //

    String getPrismHost()
    {
        return get(C.PRISM_HOST);
    }

    String getPrismEndpoint()
    {
        return get(C.PRISM_ENDPOINT);
    }

    // ------ MessageKitProcessor ------ //

    String getKafkaAutoCommitEnable()
    {
        return get(C.KAFKA_AUTO_COMMIT_ENABLE);
    }

    String getKafkaAutoOffsetReset()
    {
        return get(C.KAFKA_AUTO_OFFSET_RESET);
    }

    String getKafkaRebalanceBackoffMs()
    {
        return get(C.KAFKA_REBALANCE_BACKOFF_MS);
    }

    String getKafkaRebalanceRetriesMax()
    {
        return get(C.KAFKA_REBALANCE_RETRIES_MAX);
    }

    String getKafkaZookeeperConnectionTimeoutMs()
    {
        return get(C.KAFKA_ZOOKEEPER_CONNECTION_TIMEOUT_MS);
    }

    String getKafkaZookeeperSessionTimeoutMs()
    {
        return get(C.KAFKA_ZOOKEEPER_SESSION_TIMEOUT_MS);
    }

    String getKafkaTopic()
    {
        return get(C.KAFKA_TOPIC);
    }

    String getKafkaGroupId()
    {
        return get(C.KAFKA_GROUP_ID);
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

    String getDebug()
    {
        return get(C.DEBUG);
    }

    boolean isDebug()
    {
        return "true".equalsIgnoreCase(getDebug());
    }

    public String getDebugEmailPrefix()
    {
        if (isDebug()) {
            return get(C.DEBUG_EMAIL_PREFIX);
        }
        return "";

    }

    String getTlsEnabled()
    {
        return get(C.TLS_ENABLED);
    }

    boolean isTlsEnabled()
    {
        return "true".equalsIgnoreCase(getTlsEnabled());
    }

    String getTlsInsecureSkipVerify()
    {
        return get(C.TLS_INSECURE_SKIP_VERIFY);
    }

    boolean isTlsInsecureSkipVerify()
    {
        return "true".equalsIgnoreCase(getTlsInsecureSkipVerify());
    }

    String getTlsCertificatePath()
    {
        return get(C.TLS_CERTIFICATE_PATH);
    }

    String getTlsPrivateKeyPath()
    {
        return get(C.TLS_PRIVATE_KEY_PATH);
    }

    String getTlsTrustStorePath()
    {
        return get(C.TLS_TRUST_STORE_PATH);
    }

    String getTlsTrustStorePassword()
    {
        return get(C.TLS_TRUST_STORE_PASSWORD);
    }

    List<String> getCkmsSecretGroups()
    {
        return getList(C.CKMS_SECRET_GROUPS);
    }

    public String getDebugOpsgenieApiKey()
    {
        return get(C.DEBUG_OPSGENIE_API_KEY);
    }

    public String getDebugSlackEndpoint()
    {
        return get(C.DEBUG_SLACK_ENDPOINT);
    }

    public String getDebugWebhookEndpoint()
    {
        return get(C.DEBUG_WEBHOOK_ENDPOINT);
    }

    public String getViewsHorizonUrl()
    {
        return get(C.VIEWS_HORIZON_URL);
    }

    public String getViewsSplunkUrl()
    {
        return get(C.VIEWS_SPLUNK_URL);
    }

    public String getViewsSplunkIndex()
    {
        return get(C.VIEWS_SPLUNK_INDEX);
    }

    public String getViewsSplunkLocale()
    {
        return get(C.VIEWS_SPLUNK_LOCALE);
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends AbstractConfig.Builder<NotificationEmitterConfig, B> {

        private static final Logger LOG =
                LoggerFactory.getLogger(Builder.class);

        @Override
        protected NotificationEmitterConfig build(Configuration configuration)
        {
            return new NotificationEmitterConfig(configuration);
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
