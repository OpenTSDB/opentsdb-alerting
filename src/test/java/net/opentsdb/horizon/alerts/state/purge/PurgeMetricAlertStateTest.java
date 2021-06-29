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

package net.opentsdb.horizon.alerts.state.purge;

import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.state.ModifiableAlertStateStore;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static net.opentsdb.horizon.alerts.core.TestUtil.generate;
import static net.opentsdb.horizon.alerts.core.TestUtil.getHashes;

public class PurgeMetricAlertStateTest {

    private static String CONFIG_FILE_PATH =
            "src/test/resources/data/PurgeMetricAlertStateTest/";


    @Test
    public void purgeAlertStateWithBasicSlidingWindow() {
        MetricAlertConfig metricAlertConfig = getAlertConfig("basic-config.json");

        final Purge purge = metricAlertConfig.createPurge();
        final long alertId = metricAlertConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                                                            metricAlertConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);
        
        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply(now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        purge.purge(alertStateStore, false);

        final List<Long> hashes = getHashes(alertStateStore);

        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));

        Assert.assertTrue(!hashes.contains(h1));
        Assert.assertTrue(!hashes.contains(h2));

    }

    private MetricAlertConfig getAlertConfig(String bas) {
        return TestUtil.getMetricAlertConfig(Paths.get(CONFIG_FILE_PATH,
                bas).toFile());
    }


    @Test
    public void purgeAlertStateWithBigSlidingWindow() {
        MetricAlertConfig metricAlertConfig = getAlertConfig("config-windowsize-big.json");

        final Purge purge = metricAlertConfig.createPurge();
        final long alertId = metricAlertConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                metricAlertConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);

        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply( now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        final long h6 = generate.apply( now - 172802);

        final long h7 = generate.apply( now - 172804);

        final long h8 = generate.apply( now - 172805);

        final long h9 = generate.apply( now - 172806);

        purge.purge(alertStateStore, false);

        final List<Long> hashes = getHashes(alertStateStore);


        Assert.assertTrue(hashes.contains(h1));
        Assert.assertTrue(hashes.contains(h2));
        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));
        Assert.assertTrue(hashes.contains(h6));
        Assert.assertTrue(!hashes.contains(h7));

        Assert.assertTrue(!hashes.contains(h8));
        Assert.assertTrue(!hashes.contains(h9));
    }

    @Test
    public void purgeAlertStateWithoutMissing() {
        MetricAlertConfig metricAlertConfig = getAlertConfig("config-notmissing.json");

        final Purge purge = metricAlertConfig.createPurge();
        final long alertId = metricAlertConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                metricAlertConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);


        final long h1 = generate.apply( now - 86401);

        final long h2 = generate.apply( now - 86402);

        final long h3 = generate.apply( now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        final long h6 = generate.apply( now - 1900);

        final long h7 = generate.apply( now - 540);

        final long h8 = generate.apply( now - 14400);

        final long h9 = generate.apply( now - 86399);

        purge.purge(alertStateStore, false);

        final List<Long> hashes = getHashes(alertStateStore);


        Assert.assertTrue(!hashes.contains(h1));
        Assert.assertTrue(!hashes.contains(h2));

        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));
        Assert.assertTrue(hashes.contains(h6));
        Assert.assertTrue(hashes.contains(h7));
        Assert.assertTrue(hashes.contains(h8));
        Assert.assertTrue(hashes.contains(h9));
    }

    @Test
    public void purgeAlertStateWithoutMissingBig() {
        MetricAlertConfig metricAlertConfig = getAlertConfig("config-notmissing-big.json");

        final Purge purge = metricAlertConfig.createPurge();
        final long alertId = metricAlertConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                metricAlertConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);


        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply( now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        final long h6 = generate.apply( now - 1900);

        final long h7 = generate.apply( now - 540);

        final long h8 = generate.apply( now - 14400);

        final long h9 = generate.apply( now - 64000);

        final long h10 = generate.apply( now - 230559);

        final long h11 = generate.apply( now - 230560);

        final long h12 = generate.apply( now - 288200);

        final long h13 = generate.apply( now - 288201);

        purge.purge(alertStateStore, false);

        final List<Long> hashes = getHashes(alertStateStore);


        Assert.assertTrue(hashes.contains(h1));
        Assert.assertTrue(hashes.contains(h2));

        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));
        Assert.assertTrue(hashes.contains(h6));
        Assert.assertTrue(hashes.contains(h7));
        Assert.assertTrue(hashes.contains(h8));
        Assert.assertTrue(hashes.contains(h9));
        Assert.assertTrue(hashes.contains(h10));

        Assert.assertTrue(!hashes.contains(h11));
        Assert.assertTrue(!hashes.contains(h12));
        Assert.assertTrue(!hashes.contains(h13));
    }

    @Test
    public void purgeAlertStateWithAutoRecovery() {
        MetricAlertConfig metricAlertConfig = getAlertConfig("config-autorecovery.json");

        final Purge purge = metricAlertConfig.createPurge();
        final long alertId = metricAlertConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                metricAlertConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);


        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply( now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        final long h6 = generate.apply( now - 1900);

        final long h7 = generate.apply( now - 540);

        final long h8 = generate.apply( now - 14400);

        final long h9 = generate.apply( now - 64000);

        final long h10 = generate.apply( now - 230559);

        final long h11 = generate.apply( now - 295399);

        final long h12 = generate.apply( now - 295400);

        final long h13 = generate.apply( now - 295401);

        purge.purge(alertStateStore, false);

        final List<Long> hashes = getHashes(alertStateStore);


        Assert.assertTrue(hashes.contains(h1));
        Assert.assertTrue(hashes.contains(h2));

        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));
        Assert.assertTrue(hashes.contains(h6));
        Assert.assertTrue(hashes.contains(h7));
        Assert.assertTrue(hashes.contains(h8));
        Assert.assertTrue(hashes.contains(h9));
        Assert.assertTrue(hashes.contains(h10));

        Assert.assertTrue(hashes.contains(h11));
        Assert.assertTrue(!hashes.contains(h12));
        Assert.assertTrue(!hashes.contains(h13));
    }

    @Test
    public void firstPurgeTest() {
        MetricAlertConfig metricAlertConfig = getAlertConfig("basic-config.json");

        final Purge purge = metricAlertConfig.createPurge();
        final long alertId = metricAlertConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                metricAlertConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);

        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply(now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        final long h6 = generate.apply( -1l);

        final long h7 = generate.apply( -1l);

        purge.purge(alertStateStore, true);

        final List<Long> hashes = getHashes(alertStateStore);

        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));

        Assert.assertTrue(!hashes.contains(h6));
        Assert.assertTrue(!hashes.contains(h7));

        Assert.assertTrue(!hashes.contains(h1));
        Assert.assertTrue(!hashes.contains(h2));

    }

    @Test
    public void notFirstPurgeTest() {
        MetricAlertConfig metricAlertConfig = getAlertConfig("basic-config.json");

        final Purge purge = metricAlertConfig.createPurge();
        final long alertId = metricAlertConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                metricAlertConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);

        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply(now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        final long h6 = generate.apply( -1l);

        final long h7 = generate.apply( -1l);

        purge.purge(alertStateStore, false);

        final List<Long> hashes = getHashes(alertStateStore);

        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));

        Assert.assertTrue(hashes.contains(h6));
        Assert.assertTrue(hashes.contains(h7));

        Assert.assertTrue(!hashes.contains(h1));
        Assert.assertTrue(!hashes.contains(h2));

    }

    @Test
    public void runPurgeOnJustNegEmptyStore() {
        MetricAlertConfig metricAlertConfig = getAlertConfig("basic-config.json");

        final Purge purge = metricAlertConfig.createPurge();
        final long alertId = metricAlertConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                metricAlertConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);

        final long h6 = generate.apply( -1l);

        final long h7 = generate.apply( -1l);

        purge.purge(alertStateStore, true);

        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply(now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);


        purge.purge(alertStateStore, false);

        final List<Long> hashes = getHashes(alertStateStore);

        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));

        Assert.assertTrue(!hashes.contains(h6));
        Assert.assertTrue(!hashes.contains(h7));

        Assert.assertTrue(!hashes.contains(h1));
        Assert.assertTrue(!hashes.contains(h2));
    }

    @Test
    public void runPurgeOnEmptyStore() {
        MetricAlertConfig metricAlertConfig = getAlertConfig("basic-config.json");

        final Purge purge = metricAlertConfig.createPurge();
        final long alertId = metricAlertConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                metricAlertConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);

        purge.purge(alertStateStore, true);

        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply(now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        final long h6 = generate.apply( -1l);

        final long h7 = generate.apply( -1l);


        purge.purge(alertStateStore, false);

        final List<Long> hashes = getHashes(alertStateStore);

        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));

        Assert.assertTrue(hashes.contains(h6));
        Assert.assertTrue(hashes.contains(h7));

        Assert.assertTrue(!hashes.contains(h1));
        Assert.assertTrue(!hashes.contains(h2));
    }

}
