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

package net.opentsdb.horizon.alerts.monitor;

import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.core.SingleMetricTsdbTestClient;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.processor.impl.UpdatableExecutorWrapper;
import net.opentsdb.horizon.alerts.query.tsdb.TSDBV3SlidingWindowQuery;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Functional
 */
public class SingleMetricAlertTest {


    @Test
    public void testBasicE2E() throws IOException {



        MetricAlertConfig alertConfig = TestUtil.getMetricAlertConfig(
                "src/main/resources/alerts/singleMetricConfig.json");
        alertConfig.setHasWarnThreshold(false);

        SingleMetricTsdbTestClient singleMetricTsdbTestClient
                = new SingleMetricTsdbTestClient("sample", "none");

        final TSDBV3SlidingWindowQuery tsdbv3SlidingWindowQuery
                = new TSDBV3SlidingWindowQuery(alertConfig, singleMetricTsdbTestClient);
        UpdatableExecutorWrapper<MetricAlertConfig> executorWrapper =
                new UpdatableExecutorWrapper<>(alertConfig,
                        tsdbv3SlidingWindowQuery);




    }


}
