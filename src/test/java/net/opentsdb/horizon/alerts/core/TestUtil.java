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

package net.opentsdb.horizon.alerts.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.impl.HealthCheckConfig;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.model.Snooze;
import net.opentsdb.horizon.alerts.state.AlertStateEntry;
import net.opentsdb.horizon.alerts.state.ModifiableAlertStateStore;
import net.opentsdb.horizon.alerts.state.impl.AlertStateEntryImpl;
import net.opentsdb.horizon.alerts.state.impl.AlertStateStoreImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;

public class TestUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static String NS = "ns1";

    private static Random random = new Random();

    public static AlertConfig configFromFile(File file) {
        try {
            final String content =
                    FileUtils.readFileToString(
                            file,
                            StandardCharsets.UTF_8
                    );
            return AlertUtils.loadConfig(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static MetricAlertConfig getMetricAlertConfig(String file) {
        return getMetricAlertConfig(new File(file));
    }

    public static MetricAlertConfig getMetricAlertConfig(File file) {

        return getAlertConfig(file, MetricAlertConfig.class);

    }

    public static HealthCheckConfig getHealthCheckConfig(File file) {
        return getAlertConfig(file, HealthCheckConfig.class);
    }

    public static <T extends AlertConfig> T getAlertConfig(File file, Class<T> klass) {
        final AlertConfig alertConfig = configFromFile(file);
        Assert.assertTrue(alertConfig.getClass() == klass);
        return klass.cast(alertConfig);
    }

    public static String loadResource(final String name) {
        final ClassLoader cl = TestUtil.class.getClassLoader();
        try {
            final InputStream is = cl.getResource(name).openStream();
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode parseJson(final String jsonString) {
        try {
            return OBJECT_MAPPER.readTree(jsonString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ModifiableAlertStateStore createStateStore(AlertConfig metricAlertConfig) {

        return new AlertStateStoreImpl(
                String.valueOf(metricAlertConfig.getAlertId()),
                metricAlertConfig.getNagIntervalInSecs(),
                metricAlertConfig.getTransitionConfig(),
                metricAlertConfig.storeIdentity()
        );

    }

    public static SortedMap<String, String> toSortedMap(String[][] ts1) {

        TreeMap<String,String> map = new TreeMap<>();

        for (int i = 0; i < ts1.length; i++) {
            map.put(ts1[i][0],ts1[i][1]);
        }

        return map;
    }

    public static SortedMap<String, String> getRandomTagSet() {

        final int i = random.nextInt(1000);
        final int j = random.nextInt(10000);

        final int k = random.nextInt(10000);
        final int l = random.nextInt(1000);

        final double m = random.nextDouble();
        final double n = random.nextDouble();

        final double o = random.nextDouble();
        final double p = random.nextDouble();

        SortedMap<String, String> tags =
                new TreeMap<String,String>(){{

                    put(i+"key"+j, k+"value1"+l);
                    put(m+"key1"+n, "value1"+p);

                }};

        return tags;
    }

    public static List<Long> getHashes(ModifiableAlertStateStore alertStateEntries) {
        List<Long> stateIds = new ArrayList<>();
        final Iterator<AlertStateEntry> iterator = alertStateEntries.iterator();
        while (iterator.hasNext()) {
            stateIds.add(iterator.next().getStateId());
        }
        return stateIds;
    }

    public static long add(ModifiableAlertStateStore alertStateStore,
                            String ns,
                            SortedMap<String, String> tags,
                            long alertId,
                            long lastSeen) {
        final long stateid = AlertUtils.
                getHashForNAMT(ns, alertId, tags);

        alertStateStore.put(
                new AlertStateEntryImpl(
                        stateid,
                        tags,
                        AlertState.BAD,
                        AlertState.GOOD,
                        lastSeen,
                        lastSeen
                )
        );


        return stateid;
    }

    public static Function<Long, Long> generate(
            final ModifiableAlertStateStore alertStateStore,
            final long alertId) {
        return val -> add(alertStateStore, NS, getRandomTagSet(), alertId, val);
    }

    public static Snooze getSnoozeCoonfigFromFile(String configPath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        final Snooze snooze = objectMapper.readValue(
                new File(configPath),
                Snooze.class);
        return snooze;
    }

    public static Snooze makeActive(Snooze snooze) {
        snooze.overrideEndTime(System.currentTimeMillis() + 3600000l);
        snooze.overrideStartTime(System.currentTimeMillis() - 3600000l);
        return snooze;
    }

    public static AlertConfig getHealthCheckConfig(String file) {
        return getHealthCheckConfig(new File(file));
    }
}
