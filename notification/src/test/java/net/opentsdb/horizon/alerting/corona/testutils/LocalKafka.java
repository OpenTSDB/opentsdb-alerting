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

package net.opentsdb.horizon.alerting.corona.testutils;

import kafka.admin.AdminUtils;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import org.I0Itec.zkclient.ZkClient;
import org.apache.curator.test.TestingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class LocalKafka {

    private static final Logger LOG = LoggerFactory.getLogger(LocalKafka.class);

    public static final String KAFKA_LOG_DIR = "/tmp/test/kafka";
    public static final int KAFKA_BROKER_ID = 0;
    public static final int KAFKA_BROKER_PORT = 5000;
    public static final String KAFKA_BROKER =
            String.format("localhost:%d", KAFKA_BROKER_PORT);

    public static final String ZOOKEEPER_LOG_DIR = "/tmp/test/zookeeper";
    public static final int ZOOKEEPER_PORT = 2000;
    public static final String ZOOKEEPER_HOST =
            String.format("localhost:%d", ZOOKEEPER_PORT);

    public static void deleteDir(File dir)
    {
        File[] files = dir.listFiles();
        if (files != null) { // some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDir(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    private final TestingServer zookeeper;

    private final KafkaServerStartable kafka;

    public LocalKafka()
    {
        deleteTestDirs();
        try {
            this.zookeeper =
                    new TestingServer(
                            ZOOKEEPER_PORT,
                            new File(ZOOKEEPER_LOG_DIR)
                    );

            Thread.sleep(1_000L);

            this.kafka = new KafkaServerStartable(getKafkaConfig());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteTestDirs()
    {
        File zookeeperDir = new File(ZOOKEEPER_LOG_DIR);
        if (zookeeperDir.exists()) {
            deleteDir(zookeeperDir);
        }

        File kafkaDir = new File(KAFKA_LOG_DIR);
        if (kafkaDir.exists()) {
            deleteDir(kafkaDir);
        }
    }


    private KafkaConfig getKafkaConfig()
    {
        final Properties properties = new Properties();
        properties.put("port", KAFKA_BROKER_PORT + "");
        properties.put("broker.id", KAFKA_BROKER_ID + "");
        properties.put("log.dir", KAFKA_LOG_DIR);
        properties.put("zookeeper.connect", ZOOKEEPER_HOST);
        properties.put("default.replication.factor", "1");
        properties.put("delete.topic.enable", "true");
        properties.put("auto.instance.topics.enable", "true");

        return new KafkaConfig(properties);
    }

    public void createTopic(String topic)
    {
        int sessionTimeoutMs = 10_000;
        int connectionTimeoutMs = 10_000;
        ZkClient client =
                new ZkClient(
                        ZOOKEEPER_HOST,
                        sessionTimeoutMs,
                        connectionTimeoutMs
                );

        AdminUtils.createTopic(client, topic, 1, 1, new Properties());
        client.close();
    }

    public void start()
    {
        try {
            zookeeper.start();
            Thread.sleep(10_000L);
            kafka.startup();
            Thread.sleep(10_000L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop()
    {
        kafka.shutdown();
        kafka.awaitShutdown();
        try {
            zookeeper.stop();
            zookeeper.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
