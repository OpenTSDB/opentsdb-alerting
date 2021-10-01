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

import com.google.common.base.Strings;
import net.opentsdb.horizon.alerts.state.persistence.PulsarClientSingleton;
import org.apache.commons.io.FileUtils;
import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClientException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.opentsdb.horizon.alerts.AlertUtils.SINK_KAFKA;


public class EnvironmentConfig {


    private static final String ENVIRONMENT = "env";

    private static final String TEST_ENVIRONMENT = "TEST";

    public static Properties propertiesFile = new Properties();

    public static final String PROPERTIES_FILE = "src/test/resources/config.properties";

    public static final String LOCAL_PROPERTIES_FILE = "src/test/resources/config.properties";

    public static boolean IS_LOCAL = true;

    public static final String MIRROR_SERVER_PORT_NUMBER = "mirror_server_port_number";

    public static final String DEFAULT_MIRROR_SERVER_PORT_NUMBER = "5121";

    public static final String TRUST_STORE_PATH = "trust_store_path";

    public static final String DEFAULT_TRUST_STORE_PATH = "";

    public static final String MIRROR_SERVERS="mirror_servers";

    public static String DEFAULT_MIRROR_SERVERS= null;

    public static final String NUM_DAEMONS = "num_daemons";

    public static final String DEFAULT_NUM_DAEMONS = "1";

    public static String ATHENS_CERT_FILE = "athens_cert_file";

    public static String DEFAULT_ATHENS_CERT_FILE = "";

    public static String ATHENS_KEY_FILE = "athens_key_file";

    public static String DEFAULT_ATHENS_KEY_FILE = "";

    public static String ATHENS_CA_FILE = "athens_ca_file";

    public static String DEFAULT_ATHENS_CA_FILE = "";

    public static String ALERT_CONFIG_FILE_PATH = "alert_config_file_path";

    public static String DEAULT_ALERT_CONFIG_FILE_PATH = "src/main/resources/alerts";

    public static String CONFIG_SOURCE = "config_source";

    public static String NUM_THREADS_IN_ALERT_POOL = "num_threads_in_alert_pool";

    public static String DEFAULT_NUM_THREADS_IN_ALERT_POOL = "40";

    public static String DEFAULT_CONFIG_SOURCE = "local";

    public static String CONFIG_DB_ENDPOINT = "config_db_endpoint";

    public static String DEFAULT_CONFIG_DB_ENDPOINT = "";

    public static String TSDB_ENDPOINT = "tsdb_endpoint";

    public static String DEFAULT_TSDB_ENDPOINT = "";

    public static String MIRROR_SET_ID = "mirror_set_id";

    public static String DEFAULT_MIRROR_SET_ID = "0";

    public static String MIRROR_ID = "mirror_id";

    public static String DEFAULT_MIRROR_ID = "0";

    public static String NUMBER_OF_MIRROR_SETS = "number_of_mirror_sets";

    public static String DEFAULT_NUMBER_OF_MIRROR_SETS = "1";

    public static String NUMBER_OF_MIRRORS = "number_of_mirrors";

    public static String DEFAULT_NUMBER_OF_MIRRORS = "1";

    public static String MONITOR_CHECK_TIMEOUT_MS = "monitor_check_timeout_ms";

    public static String DEFAULT_MONITOR_CHECK_TIMEOUT = "10";

    public static String ALERT_DAEMON_INITIAL_DELAY_SECS = "alert_daemon_initial_delay_secs";

    public static String DEFAULT_ALERT_DAEMON_INITIAL_DELAY = "5";

    public static String ALERT_DAEMON_RUN_FREQ_SECS = "alert_daemon_run_freq_secs";

    public static String DEFAULT_ALERT_DAEMON_RUN_FREQ_SECS = "30";

    public static final String NUMBER_OF_KAFKA_PRODUCERS = "number_of_kafka_producers";

    public static final String DEFAULT_NUMBER_OF_KAFKA_PRODUCERS = "10";

    public static final String KAFKA_BROKERS = "kafka_brokers";

    public static final String DEFAULT_KAFKA_BROKERS = null;

    public static final String KAFKA_RETRY_BACKOFF = "kafka_retry_backoff";

    public static final String DEFAULT_KAFKA_RETRY_BACKOFF = "0";

    public static final String KAFKA_MAX_SEND_RETRIES = "kafka_max_send_retries";

    public static final String DEFAULT_KAFKA_MAX_SEND_RETRIES = "10";

    public static final String HEARTBEAT_SERVER_PATH = "heartbeat_server_path";

    public static final String DEFAULT_HEARTBEAT_SERVER_PATH = "/health";

    public static final String MIRRORING_ENABLED = "mirroring_enabled";

    public static final String DEFAULT_MIRRORING_ENABLED = "false";

    public static final String CONFIG_DB_AUTH_PROVIDER = "config_db_auth_provider";

    public static final String DEFAULT_CONFIG_DB_AUTH_PROVIDER = "athens";

    public static final String TSDB_AUTH_PROVIDER = "tsdb_auth_provider";

    public static final String DEFAULT_TSDB_AUTH_PROVIDER = "athens";

    public static final String TSDB_PROVIDERS_FILE = "tsdb_providers_file";

    public static final String DEFAULT_TSDB_PROVIDERS_FILE = "src/test/resources/providers.yaml";

    public static final String BASE_HOST = "base_host";

    public static final String DEFAULT_BASE_HOST = null;

    public static final String INTERVALS_TO_CHANGE = "intervals_to_change";

    public static final String DEFAULT_INTERVALS_TO_CHANGE = "20";

    public static final String INTERVALS_TO_RECOVER = "intervals_to_recover";

    public static final String DEFAULT_INTERVALS_TO_RECOVER = "5";

    public static final String EMAIL_RELAY_SERVER = "email_relay_server";

    public static final String DEFAULT_EMAIL_RELAY_SERVER = "";

    public static final String MAIL_ADDRESS_SUFFIX = "mail_address_suffix";

    public static final String DEFAULT_MAIL_ADDRESS_SUFFIX = "opentsdb-alert-do-not-reply@opentsdb.net";

    public static final String TIME_FOR_GRAPH = "time_for_graph";

    public static final String DEFAULT_TIME_FOR_GRAPH = "20";

    public static final String ALERTS_TOPIC = "alerts_topic";

    public static final String STATUS_TOPIC = "status_topic";

    public static final String FIRE_EMAILS = "fire_emails";

    public static final String DEFAULT_FIRE_EMAILS = "false";

    public static final String BASE_STATUS_JSON_PATH = "base_status_json_path";

    public static final String PULSAR_ATHENZ_ENABLED = "pulsar_athenz_enabled";

    public static final String DEFAULT_PULSAR_ATHENZ_ENABLED = "true";

    public static final String PULSAR_BROKER_NAME = "pulsar_broker_name";

    public static final String DEFAULT_PULSAR_BROKER_NAME = "";

    public static final String PULSAR_TOPIC_NAME = "pulsar_topic_name";

    public static final String DEFAULT_PULSAR_TOPIC_NAME = "persistent://opentsdb/global/test_alert/alert_state";

    public static final String PULSAR_STATE_PERSISTENCE_ENABLED = "pulsar_state_persistence_enabled";

    public static final String DEFAULT_PULSAR_STATE_PERSISTENCE_ENABLED = "false";

    public static final String STATUS_WRITE_SINK = "status_write_sink";

    public static final String DEFAULT_STATUS_WRITE_SINK = SINK_KAFKA;

    public static final String COLLECTOR_FLUSH_SIZE = "collector_flush_size";

    public static final String DEFAULT_COLLECTOR_FLUSH_SIZE = "200";

    public static final String COLLECTOR_BATCH_TIMEOUT = "collector_batch_timeout";

    public static final String DEFAULT_COLLECTOR_BATCH_TIMEOUT = "5";

    public static final String COLLECTOR_FLUSH_FREQUENCY = "collector_flush_frequency";

    public static final String DEFAULT_COLLECTOR_FLUSH_FREQUENCY = "200";

    public static final String NUMBER_OF_COLLECTOR_CLIENTS = "number_of_collector_clients";

    public static final String DEFAULT_NUMBER_OF_COLLECTOR_CLIENTS = "5";

    public static final String MAX_STATUS_BACKLOG = "max_collector_backlog";

    public static final String DEFAULT_MAX_STATUS_BACKLOG = "2000000";

    public static final String MAX_POP_BACKLOG = "max_pop_backlog";

    public static final String DEFAULT_MAX_POP_BACKLOG = "50000";

    public static final String COLLECTOR_URL = "collector_url";

    public static final String DEFAULT_COLLECTOR_URL = "";

    public static final String PULSAR_CONSUMER_PREFIX = "pulsar_consumer_prefix";

    public static final String DEFAULT_PULSAR_CONSUMER_PREFIX = "horizon-alerts";

    private static final String PULSAR_SEND_TIMEOUT_MS = "pulsar_send_timeout_ms";

    private static final String DEFAULT_PULSAR_SEND_TIMEOUT_MS = "1000";

    private static final String PULSAR_MAX_PENDING_MESSAGES = "pulsar_max_pending_messages";

    private static final String DEFAULT_PULSAR_MAX_PENDING_MESSAGES = "300";

    private static final String METRIC_ALERT_PURGE_INTERVAL_NM = "non_missing_purge_interval_metric";

    private static final String DEFAULT_METRIC_ALERT_PURGE_INTERVAL_NM = "86400";

    private static final String METRIC_ALERT_PURGE_INTERVAL_M = "missing_purge_interval_metric";

    private static final String DEFAULT_METRIC_ALERT_PURGE_INTERVAL_M = "604800";

    private static final String HEALTH_CHECK_ALERT_PURGE_INTERVAL_NM = "non_missing_purge_interval_check";

    private static final String DEFAULT_HEALTH_CHECK_ALERT_PURGE_INTERVAL_NM = "172800";

    private static final String HEALTH_CHECK_ALERT_PURGE_INTERVAL_M = "missing_purge_interval_check";

    private static final String DEFAULT_HEALTH_CHECK_ALERT_PURGE_INTERVAL_M = "604800";

    private static final String NAMESPACES_TO_RUN = "namespaces_to_run";

    private static final String DEFAULT_NAMESPACES_TO_RUN = "";

    private static final String NAMESPACES_TO_REJECT = "namespaces_to_reject";

    private static final String NAMESPACE_DELIMITER = ",";

    public static final String ALL_NAMESPACES = "*";

    private static final String CONTAINER_NAME = "container_name";

    private static final String ENABLE_SNOOZE_TAGS = "enable_snooze_tags";

    private static final String DEFAULT_ENABLE_SNOOZE_TAGS = "false";

    public static final String PULSAR_ATHENZ_TYPE = "pulsar_athenz_type";

    public static final String DEFAULT_PULSAR_ATHENZ_TYPE = "tls";

    public static final String PULSAR_ATHENZ_TENANT_DOMAIN = "pulsar_athenz_tenant_domain";

    public static final String DEFAULT_PULSAR_ATHENZ_TENANT_DOMAIN = "";

    public static final String PULSAR_ATHENZ_TENANT_SERVICE = "pulsar_athenz_tenant_service";

    public static final String DEFAULT_PULSAR_ATHENZ_TENANT_SERVICE = "";

    public static final String PULSAR_ATHENZ_PROVIDER_DOMAIN = "pulsar_athenz_provider_domain";

    public static final String DEFAULT_PULSAR_ATHENZ_PROVIDER_DOMAIN = "";

    public static final String PULSAR_ATHENZ_PRIVATE_KEY = "pulsar_athenz_private_key";

    public static final String DEFAULT_PULSAR_ATHENZ_PRIVATE_KEY = "";

    public static final String PULSAR_ATHENZ_KEY_ID = "pulsar_athenz_key_id";

    public static final String DEFAULT_PULSAR_ATHENZ_KEY_ID = "0";

    static {
        String file = PROPERTIES_FILE;
        if(IS_LOCAL) {
            file = LOCAL_PROPERTIES_FILE;
        }
        try {
            propertiesFile.load(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void seedFromFile(String file) {
        propertiesFile.clear();
        try {
            propertiesFile.load(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void seedFromProps(Properties properties) {
        propertiesFile.clear();
        propertiesFile.putAll(properties);
    }

    public int getPort() {
        return Integer.parseInt(propertiesFile.getProperty(MIRROR_SERVER_PORT_NUMBER,
                                DEFAULT_MIRROR_SERVER_PORT_NUMBER));
    }

    public boolean isMirroringEnabled() {
        return Boolean.parseBoolean(propertiesFile.getProperty(MIRRORING_ENABLED,DEFAULT_MIRRORING_ENABLED));
    }

    public int getNumThreadsInAlertPool() {
        return Integer.parseInt(propertiesFile.getProperty(NUM_THREADS_IN_ALERT_POOL,DEFAULT_NUM_THREADS_IN_ALERT_POOL));
    }

    public String[] getMirrors() {

        final String property = propertiesFile.getProperty(MIRROR_SERVERS,
                DEFAULT_MIRROR_SERVERS);

        final String[] servers;

        if(!AlertUtils.isEmpty(property)) {
            servers =  property.split(",");
        } else {
            servers = null;
        }
        return servers;
    }

    public String getTrustStorePath() {

        return propertiesFile.getProperty(TRUST_STORE_PATH,DEFAULT_TRUST_STORE_PATH);

    }

    public String getTrustStorePassword() {
        return "changeit";
    }

    public String getAthenzCert() {

        return propertiesFile.getProperty(ATHENS_CERT_FILE,DEFAULT_ATHENS_CERT_FILE);
    }

    public String getAthenzKey() {
        return propertiesFile.getProperty(ATHENS_KEY_FILE,DEFAULT_ATHENS_KEY_FILE);
    }

    public String getAthenzCA() {
        return propertiesFile.getProperty(ATHENS_CA_FILE,DEFAULT_ATHENS_CA_FILE);
    }

    public String getAlertConfigFilePath() {
        return propertiesFile.getProperty(ALERT_CONFIG_FILE_PATH,
                                DEAULT_ALERT_CONFIG_FILE_PATH);
    }

    public String getConfigSource(){
        return propertiesFile.getProperty(CONFIG_SOURCE,DEFAULT_CONFIG_SOURCE);
    }

    public String getConfigDbEndpoint() {
        return propertiesFile.getProperty(CONFIG_DB_ENDPOINT,DEFAULT_CONFIG_DB_ENDPOINT);
    }

    public String getTsdbEndpoint() {
        return propertiesFile.getProperty(TSDB_ENDPOINT,DEFAULT_TSDB_ENDPOINT);
    }

    public int getMirrorSetId() {
        return Integer.parseInt(propertiesFile.getProperty(MIRROR_SET_ID,DEFAULT_MIRROR_SET_ID));
    }

    public int getMirrorId() {
        return Integer.parseInt(propertiesFile.getProperty(MIRROR_ID,DEFAULT_MIRROR_ID));
    }

    public int getNumberOfMirrors() {
        return Integer.parseInt(propertiesFile.getProperty(NUMBER_OF_MIRRORS,DEFAULT_NUMBER_OF_MIRRORS));
    }

    public int getNumberOfMirrorSets() {
        return Integer.parseInt(propertiesFile.getProperty(NUMBER_OF_MIRROR_SETS,DEFAULT_NUMBER_OF_MIRROR_SETS));
    }

    public int getNumberOfDaemons() {
        return Integer.parseInt(propertiesFile.getProperty(NUM_DAEMONS,DEFAULT_NUM_DAEMONS));
    }

    public long getMonitorCheckTimeoutMs() {
        return Long.parseLong(propertiesFile.getProperty(MONITOR_CHECK_TIMEOUT_MS,DEFAULT_MONITOR_CHECK_TIMEOUT));
    }

    public long getAlertDaemonInitialDelaySecs() {
        return Long.parseLong(propertiesFile.getProperty(ALERT_DAEMON_INITIAL_DELAY_SECS,DEFAULT_ALERT_DAEMON_INITIAL_DELAY));
    }

    public long getAlertDaemonRunFreqSecs() {
        return Long.parseLong(propertiesFile.getProperty(ALERT_DAEMON_RUN_FREQ_SECS,DEFAULT_ALERT_DAEMON_RUN_FREQ_SECS));
    }

    public int getNumberOfProducers() {
        return Integer.parseInt(propertiesFile.getProperty(NUMBER_OF_KAFKA_PRODUCERS,DEFAULT_NUMBER_OF_KAFKA_PRODUCERS));
    }

    public String getKafkaBrokersFormatted() {
        return propertiesFile.getProperty(KAFKA_BROKERS,DEFAULT_KAFKA_BROKERS);
    }

    public String getMessageSendMaxRetries() {
        return propertiesFile.getProperty(KAFKA_MAX_SEND_RETRIES,DEFAULT_KAFKA_MAX_SEND_RETRIES);
    }

    public String getRetryBackOff() {
        return propertiesFile.getProperty(KAFKA_RETRY_BACKOFF,DEFAULT_KAFKA_RETRY_BACKOFF);
    }

    public String getHeartbeatServerPath() {
        return propertiesFile.getProperty(HEARTBEAT_SERVER_PATH,DEFAULT_HEARTBEAT_SERVER_PATH);
    }

    public String getConfigDbAuthProvider() {
        return propertiesFile.getProperty(CONFIG_DB_AUTH_PROVIDER,DEFAULT_CONFIG_DB_AUTH_PROVIDER);
    }

    public String getTSDBAuthProvider() {
        return propertiesFile.getProperty(TSDB_AUTH_PROVIDER,DEFAULT_TSDB_AUTH_PROVIDER);
    }

    public String getTSDBConfigFile() {
        return propertiesFile.getProperty(TSDB_PROVIDERS_FILE,DEFAULT_TSDB_PROVIDERS_FILE);
    }

    public String getBaseHost() {
        return propertiesFile.getProperty(BASE_HOST,DEFAULT_BASE_HOST);
    }

    public String getContainerName() {
        String containerName = propertiesFile.getProperty(CONTAINER_NAME);
        if (containerName == null) {
            throw new RuntimeException("Container name cannot be empty. Please set it in config file.");
        }
        return containerName;
    }

    public int getIntervalsToChange() {
        return Integer.parseInt(propertiesFile.getProperty(INTERVALS_TO_CHANGE,DEFAULT_INTERVALS_TO_CHANGE));
    }

    public int getIntervalsToRecover() {
        return Integer.parseInt(propertiesFile.getProperty(INTERVALS_TO_RECOVER,DEFAULT_INTERVALS_TO_RECOVER));
    }

    public String getEmailRelayServer() {
        return propertiesFile.getProperty(EMAIL_RELAY_SERVER,DEFAULT_EMAIL_RELAY_SERVER);
    }

    public String getMailAddressSuffix() {
        return propertiesFile.getProperty(MAIL_ADDRESS_SUFFIX,DEFAULT_MAIL_ADDRESS_SUFFIX);
    }

    public long getTimeFactorForGraph() {
        return Long.parseLong(propertiesFile.getProperty(TIME_FOR_GRAPH,DEFAULT_TIME_FOR_GRAPH));
    }

    public String getStatusTopic() {
        return propertiesFile.getProperty(STATUS_TOPIC);
    }

    public String getAlertsTopic() {
        return propertiesFile.getProperty(ALERTS_TOPIC);
    }

    public boolean fireEmails() {
        return Boolean.parseBoolean(propertiesFile.getProperty(FIRE_EMAILS,DEFAULT_FIRE_EMAILS));
    }

    public String getEnv() {
        return propertiesFile.getProperty(ENVIRONMENT,TEST_ENVIRONMENT);
    }

    public boolean inTestEnv() {
        return getEnv().equalsIgnoreCase(TEST_ENVIRONMENT);
    }

    public String getBaseStatusJson() throws IOException {
        return FileUtils.readFileToString(new File(propertiesFile.getProperty(BASE_STATUS_JSON_PATH))
                ,StandardCharsets.UTF_8);
    }

    public boolean isPulsarAthenzEnabled() {
        return Boolean.parseBoolean(
                propertiesFile.getProperty(
                        PULSAR_ATHENZ_ENABLED,
                        DEFAULT_PULSAR_ATHENZ_ENABLED
                )
        );
    }

    public String getPulsarBrokerName() {
        return propertiesFile.getProperty(PULSAR_BROKER_NAME, DEFAULT_PULSAR_BROKER_NAME);
    }

    public String getPulsarTopicName() {
        return propertiesFile.getProperty(PULSAR_TOPIC_NAME, DEFAULT_PULSAR_TOPIC_NAME);
    }

    public String getPulsarAthenzType() {
        return propertiesFile.getProperty(PULSAR_ATHENZ_TYPE, DEFAULT_PULSAR_ATHENZ_TYPE);
    }

    public String getPulsarAthenzTenantDomain() {
        return propertiesFile.getProperty(PULSAR_ATHENZ_TENANT_DOMAIN, DEFAULT_PULSAR_ATHENZ_TENANT_DOMAIN);
    }

    public String getPulsarAthenzTenantService() {
        return propertiesFile.getProperty(PULSAR_ATHENZ_TENANT_SERVICE, DEFAULT_PULSAR_ATHENZ_TENANT_SERVICE);
    }

    public String getPulsarAthenzProviderDomain() {
        return propertiesFile.getProperty(PULSAR_ATHENZ_PROVIDER_DOMAIN, DEFAULT_PULSAR_ATHENZ_PROVIDER_DOMAIN);
    }

    public String getPulsarAthenzPrivateKey() {
        return propertiesFile.getProperty(PULSAR_ATHENZ_PRIVATE_KEY, DEFAULT_PULSAR_ATHENZ_PRIVATE_KEY);
    }

    public String getPulsarAthenzKeyId() {
        return propertiesFile.getProperty(PULSAR_ATHENZ_KEY_ID, DEFAULT_PULSAR_ATHENZ_KEY_ID);
    }

    public String getStatusWriteSink() {
        return propertiesFile.getProperty(STATUS_WRITE_SINK, DEFAULT_STATUS_WRITE_SINK);
    }

    public int getCollectorFlushSize() {
        return Integer.parseInt(propertiesFile.getProperty(COLLECTOR_FLUSH_SIZE, DEFAULT_COLLECTOR_FLUSH_SIZE));
    }

    public int getCollectorBatchTimeout() {
        return Integer.parseInt(propertiesFile.getProperty(COLLECTOR_BATCH_TIMEOUT, DEFAULT_COLLECTOR_BATCH_TIMEOUT));
    }

    public int getCollectorFlushFrequency() {
        return Integer.parseInt(propertiesFile.getProperty(COLLECTOR_FLUSH_FREQUENCY, DEFAULT_COLLECTOR_FLUSH_FREQUENCY));
    }

    public int getNumCollectorClients() {
        return Integer.parseInt(propertiesFile.getProperty(NUMBER_OF_COLLECTOR_CLIENTS, DEFAULT_NUMBER_OF_COLLECTOR_CLIENTS));
    }

    public int getMaxStatusBacklogToCollector() {
        return Integer.parseInt(propertiesFile.getProperty(MAX_STATUS_BACKLOG, DEFAULT_MAX_STATUS_BACKLOG));
    }

    public int getMaxPopBacklogToCollector() {
        return Integer.parseInt(propertiesFile.getProperty(MAX_POP_BACKLOG, DEFAULT_MAX_POP_BACKLOG));
    }

    public String getCollectorUrl() {
        return propertiesFile.getProperty(COLLECTOR_URL, DEFAULT_COLLECTOR_URL);
    }

    public boolean isPulsarStatePersistenceEnabled() {
        return Boolean.parseBoolean(
                propertiesFile.getProperty(
                        PULSAR_STATE_PERSISTENCE_ENABLED,
                        DEFAULT_PULSAR_STATE_PERSISTENCE_ENABLED
                )
        );
    }

    public String getPulsarConsumerName() {
        return getContainerName() + "-" + getMirrorId();
    }

    public int getPulsarSendTimeoutMs() {
        return Integer.parseInt(propertiesFile
                .getProperty(PULSAR_SEND_TIMEOUT_MS, DEFAULT_PULSAR_SEND_TIMEOUT_MS));
    }

    public int getPulsarMaxPendingMessages() {
        return Integer.parseInt(propertiesFile
                .getProperty(PULSAR_MAX_PENDING_MESSAGES, DEFAULT_PULSAR_MAX_PENDING_MESSAGES));
    }

    public Producer getPulsarPersistorConfig() {
        try {
            return PulsarClientSingleton.get().newProducer()
                    .topic(getPulsarTopicName())
                    .sendTimeout(getPulsarSendTimeoutMs(), TimeUnit.MILLISECONDS)
                    .blockIfQueueFull(false)
                    .maxPendingMessages(getPulsarMaxPendingMessages())
                    .compressionType(CompressionType.LZ4)
                    .create();
        } catch (PulsarClientException e) {
            throw new RuntimeException("Unable to instantiate the producer", e);
        }
    }
    
    public int getMetricPurgeIntervalNonMissing() {
        return Integer.parseInt(propertiesFile
                .getProperty(METRIC_ALERT_PURGE_INTERVAL_NM,
                        DEFAULT_METRIC_ALERT_PURGE_INTERVAL_NM));
    }

    public int getMetricPurgeIntervalMissing() {
        return Integer.parseInt(propertiesFile
                .getProperty(METRIC_ALERT_PURGE_INTERVAL_M,
                        DEFAULT_METRIC_ALERT_PURGE_INTERVAL_M));
    }

    public int getHealthCheckPurgeIntervalNonMissing() {
        return Integer.parseInt(propertiesFile
                .getProperty(HEALTH_CHECK_ALERT_PURGE_INTERVAL_NM,
                        DEFAULT_HEALTH_CHECK_ALERT_PURGE_INTERVAL_NM));
    }

    public int getHealthCheckPurgeIntervalMissing() {
        return Integer.parseInt(propertiesFile
                .getProperty(HEALTH_CHECK_ALERT_PURGE_INTERVAL_M,
                        DEFAULT_HEALTH_CHECK_ALERT_PURGE_INTERVAL_M));
    }

    public Optional<List<String>> getNamespacesToRun() {
        return getList(NAMESPACES_TO_RUN,
                DEFAULT_NAMESPACES_TO_RUN,
                NAMESPACE_DELIMITER);
    }

    public Optional<List<String>> getNamespacesToReject() {
        return getList(NAMESPACES_TO_REJECT,
                null,
                NAMESPACE_DELIMITER);
    }

    public boolean isSnoozeTagsEnabled() {
        return Boolean.parseBoolean(
                propertiesFile.getProperty(
                        ENABLE_SNOOZE_TAGS,
                        DEFAULT_ENABLE_SNOOZE_TAGS
                ));
    }

    public Optional<List<String>> getList(final String property,
                                          final String defaultValue,
                                          final String delimiter) {
        final String slist = propertiesFile.getProperty(property,
                        defaultValue);
        final List<String> list;
        if(!Strings.isNullOrEmpty(slist)) {
            list = Arrays.asList(slist.split(delimiter))
                    .stream()
                    .map(ns -> ns.trim())
                    .collect(Collectors.toList());
        } else {
            list = null;
        }
        return Optional.ofNullable(list);
    }
}
