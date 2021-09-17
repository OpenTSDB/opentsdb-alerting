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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertProcessorConfigTest {

    @Test
    void itWorks() {
        PropertiesConfiguration props = new PropertiesConfiguration();
        props.setProperty("config.api.url", "api.config.com");
        props.setProperty("config.api.client.http.certificate.path", "cert.pem");
        props.setProperty("config.api.client.http.private.key.path", "key.pem");
        props.setProperty("config.api.client.http.trust.store.path", "ts.jks");
        props.setProperty("config.api.client.http.trust.store.password", "secret");
        props.setProperty("alert.processor.kafka.topic", "alerts_topic");
        props.setProperty("alert.processor.kafka.group.id", "alerts_group");
        props.setProperty("kafka.zookeeper.connect", "zookeeper");
        props.setProperty("kafka.broker.list", "broker0,broker1");
        props.setProperty("monitoring.namespace", "NS");
        props.setProperty("monitoring.host", "localhost");
        props.setProperty("monitoring.container.name", "chubby");

        AlertProcessorConfig cfg = new AlertProcessorConfig(props);
        assertEquals("api.config.com", cfg.getConfigApiUrl());
        assertEquals(5, cfg.getConfigApiClientMaxConnPerRoute());
        assertEquals(5, cfg.getConfigApiClientMaxConnTotal());
        assertEquals(3, cfg.getConfigApiClientRetryMax());
        assertEquals(10_000, cfg.getConfigApiClientConnectionRequestTimeoutMs());
        assertEquals(10_000, cfg.getConfigApiClientConnectTimeoutMs());
        assertEquals(10_000, cfg.getConfigApiClientSocketTimeoutMs());
        assertEquals("cert.pem", cfg.getConfigApiClientHttpCertificatePath());
        assertEquals("key.pem", cfg.getConfigApiClientHttpPrivateKeyPath());
        assertEquals("ts.jks", cfg.getConfigApiClientHttpTrustStorePath());
        assertEquals("secret", cfg.getConfigApiClientHttpTrustStorePassword());

        assertEquals("true", cfg.getAlertProcessorKafkaAutoCommitEnable());
        assertEquals("largest", cfg.getAlertProcessorKafkaAutoOffsetReset());
        assertEquals("60000", cfg.getAlertProcessorKafkaRebalanceBackoffMs());
        assertEquals("20", cfg.getAlertProcessorKafkaRebalanceRetriesMax());
        assertEquals("120000", cfg.getAlertProcessorKafkaZookeeperConnectionTimeoutMs());
        assertEquals("120000", cfg.getAlertProcessorKafkaZookeeperSessionTimeoutMs());
        assertEquals("alerts_topic", cfg.getAlertProcessorKafkaTopic());
        assertEquals("alerts_group", cfg.getAlertProcessorKafkaGroupId());

        assertEquals(Collections.singletonList("zookeeper"), cfg.getKafkaZookeeperConnect());
        assertEquals("zookeeper", cfg.getKafkaZookeeperConnectAsString());
        assertEquals(Arrays.asList("broker0", "broker1"), cfg.getKafkaBrokerList());
        assertEquals("broker0,broker1", cfg.getKafkaBrokerListAsString());

        assertEquals("NS", cfg.getMonitoringNamespace());
        assertEquals("alert.processor", cfg.getMonitoringApplication());

        assertEquals(Collections.singletonList("3214"), cfg.getSyntheticAlertIds());

        assertEquals("test_corona_email", cfg.getEmailKafkaTopic());
        assertEquals("test_corona_opsgenie", cfg.getOpsgenieKafkaTopic());
        assertEquals("test_corona_slack", cfg.getSlackKafkaTopic());
        assertEquals("test_corona_oc", cfg.getOcKafkaTopic());

        assertTrue(cfg.isEnablePeriodOverPeriodAlertFilter());
        assertTrue(cfg.isDebug());
    }
}