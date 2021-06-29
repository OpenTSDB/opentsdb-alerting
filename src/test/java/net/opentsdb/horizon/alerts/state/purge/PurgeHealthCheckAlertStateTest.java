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

import net.opentsdb.horizon.alerts.config.impl.HealthCheckConfig;
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

public class PurgeHealthCheckAlertStateTest {


    private static final String CONFIG_FILE_PATH = "src/test/resources/data/PurgeHealthCheckAlertStateTest/";

    @Test
    public void purgeWithoutMissing() {

        HealthCheckConfig healthCheckConfig = getAlertConfig("basic-config.json");

        final Purge purge = healthCheckConfig.createPurge();
        final long alertId = healthCheckConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                healthCheckConfig);

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

    @Test
    public void purgeWithMissingBasic() {

        HealthCheckConfig healthCheckConfig = getAlertConfig("missing-config.json");

        final Purge purge = healthCheckConfig.createPurge();
        final long alertId = healthCheckConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                healthCheckConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);

        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply(now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        final long h6 = generate.apply( now - 604799);

        final long h7 = generate.apply( now - 604800);

        final long h8 = generate.apply( now - 604801);

        final long h9 = generate.apply( now - 604802);

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
    public void purgeWithMissingWithPurgeInterval() {
        HealthCheckConfig healthCheckConfig = getAlertConfig("missing-purge-interval.json");

        final Purge purge = healthCheckConfig.createPurge();
        final long alertId = healthCheckConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                healthCheckConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);

        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply(now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        final long h6 = generate.apply( now - 89998);

        final long h7 = generate.apply( now - 89999);

        final long h8 = generate.apply( now - 90000);

        final long h9 = generate.apply( now - 90001);

        purge.purge(alertStateStore, false);

        final List<Long> hashes = getHashes(alertStateStore);

        Assert.assertTrue(!hashes.contains(h1));
        Assert.assertTrue(!hashes.contains(h2));
        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));
        Assert.assertTrue(hashes.contains(h6));
        Assert.assertTrue(hashes.contains(h7));
        Assert.assertTrue(!hashes.contains(h8));
        Assert.assertTrue(!hashes.contains(h9));
    }

    @Test
    public void purgeWithMissingBig() {
        HealthCheckConfig healthCheckConfig = getAlertConfig("missing-config-big.json");

        final Purge purge = healthCheckConfig.createPurge();
        final long alertId = healthCheckConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                healthCheckConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);

        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply(now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        final long h6 = generate.apply( now - 604800);

        final long h7 = generate.apply( now - 619199);

        final long h8 = generate.apply( now - 619201);

        final long h9 = generate.apply( now - 619202);

        purge.purge(alertStateStore, false);

        final List<Long> hashes = getHashes(alertStateStore);

        Assert.assertTrue(hashes.contains(h1));
        Assert.assertTrue(hashes.contains(h2));
        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));
        Assert.assertTrue(!hashes.contains(h6));
        Assert.assertTrue(!hashes.contains(h7));
        Assert.assertTrue(!hashes.contains(h8));
        Assert.assertTrue(!hashes.contains(h9));
    }

    @Test
    public void testFirstPurge() {
        HealthCheckConfig healthCheckConfig = getAlertConfig("missing-config-big.json");

        final Purge purge = healthCheckConfig.createPurge();
        final long alertId = healthCheckConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                healthCheckConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);

        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply(now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        final long h6 = generate.apply( now - 604800);

        final long h7 = generate.apply( now - 619199);

        final long h8 = generate.apply( now - 619201);

        final long h9 = generate.apply( now - 619202);

        final long h10 = generate.apply( -1l);

        final long h11 = generate.apply( -1l);

        purge.purge(alertStateStore, true);

        final List<Long> hashes = getHashes(alertStateStore);

        Assert.assertTrue(hashes.contains(h1));
        Assert.assertTrue(hashes.contains(h2));
        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));
        Assert.assertTrue(!hashes.contains(h6));
        Assert.assertTrue(!hashes.contains(h7));
        Assert.assertTrue(!hashes.contains(h8));
        Assert.assertTrue(!hashes.contains(h9));
        Assert.assertTrue(!hashes.contains(h10));
        Assert.assertTrue(!hashes.contains(h11));
    }

    @Test
    public void testNonFirstPurge() {
        HealthCheckConfig healthCheckConfig = getAlertConfig("missing-config-big.json");

        final Purge purge = healthCheckConfig.createPurge();
        final long alertId = healthCheckConfig.getAlertId();

        final ModifiableAlertStateStore alertStateStore = TestUtil.createStateStore(
                healthCheckConfig);

        final long now = Instant.now().getEpochSecond();

        final Function<Long, Long> generate = generate(alertStateStore, alertId);

        final long h1 = generate.apply( now - 172801);

        final long h2 = generate.apply( now - 172803);

        final long h3 = generate.apply(now);

        final long h4 = generate.apply( now - 60);

        final long h5 = generate.apply( now - 360);

        final long h6 = generate.apply( now - 604800);

        final long h7 = generate.apply( now - 619199);

        final long h8 = generate.apply( now - 619201);

        final long h9 = generate.apply( now - 619202);

        final long h10 = generate.apply( -1l);

        final long h11 = generate.apply( -1l);

        purge.purge(alertStateStore, false);

        final List<Long> hashes = getHashes(alertStateStore);

        Assert.assertTrue(hashes.contains(h1));
        Assert.assertTrue(hashes.contains(h2));
        Assert.assertTrue(hashes.contains(h3));
        Assert.assertTrue(hashes.contains(h4));
        Assert.assertTrue(hashes.contains(h5));
        Assert.assertTrue(!hashes.contains(h6));
        Assert.assertTrue(!hashes.contains(h7));
        Assert.assertTrue(!hashes.contains(h8));
        Assert.assertTrue(!hashes.contains(h9));
        Assert.assertTrue(hashes.contains(h10));
        Assert.assertTrue(hashes.contains(h11));
    }

    private HealthCheckConfig getAlertConfig(String bas) {
        return TestUtil.getHealthCheckConfig(Paths.get(CONFIG_FILE_PATH,
                bas).toFile());
    }

}
