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

import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.monitor.impl.SimpleRunnableMonitor;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.processor.ControlledAlertExecutor;
import net.opentsdb.horizon.alerts.processor.notification.NotificationProcessor;
import mockit.Expectations;
import mockit.Injectable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class TestSimpleRunnableMonitor {

    @Injectable
    private ControlledAlertExecutor<AlertEventBag, AlertConfig> controlledAlertExecutor;

    @Injectable
    private NotificationProcessor enrichmentProcessor;

    private static final String FILE = "src/main/resources/alerts/singleMetricConfig.json";

    private static final Logger LOG = LoggerFactory.getLogger(TestSimpleRunnableMonitor.class);

    /**
     * Test if the run advance caused time to advance as expected.
     */
    @Test
    public void testIsTimeToRunWithoutInitial() throws AlertException, IOException {
        LOG.info("Starting tests");
        final long l = System.currentTimeMillis() / 1000;

        long runTimeSecs = AlertUtils.getBatchTime(l,AlertUtils.dataFrequencyInSecs);
        final MetricAlertConfig metricAlertConfig = TestUtil.getMetricAlertConfig(FILE);
        SimpleRunnableMonitor simpleRunnableMonitor = new SimpleRunnableMonitor(controlledAlertExecutor,enrichmentProcessor);
        AlertEventBag bag = new AlertEventBag(new ArrayList<>(),metricAlertConfig);
        new Expectations() {{
            controlledAlertExecutor.evaluate(runTimeSecs , TimeUnit.SECONDS);
            result = bag;
            times = 1;
            enrichmentProcessor.process(bag);
            result = true;
            times = 1;
        }};
        simpleRunnableMonitor.prep();
        simpleRunnableMonitor.execute();
        simpleRunnableMonitor.finish(false);
    }

    /**
     * Test if the run advance caused time to advance as expected.
     */
    @Test
    public void testIsTimeToRunWithoutInitialNextRun() throws AlertException, IOException {
        LOG.info("Starting tests");
        final long l = System.currentTimeMillis() / 1000;

        long runTimeSecs = AlertUtils.getBatchTime(l,AlertUtils.dataFrequencyInSecs);

        SimpleRunnableMonitor simpleRunnableMonitor = new SimpleRunnableMonitor(controlledAlertExecutor,enrichmentProcessor);
        final MetricAlertConfig metricAlertConfig = TestUtil.getMetricAlertConfig(FILE);
        AlertEventBag bag = new AlertEventBag(new ArrayList<>(),metricAlertConfig);
        new Expectations() {{
            controlledAlertExecutor.evaluate(runTimeSecs , TimeUnit.SECONDS);
            result = bag;
            times = 1;
            enrichmentProcessor.process(bag);
            result = true;
            times = 2;
            controlledAlertExecutor.evaluate(runTimeSecs +AlertUtils.dataFrequencyInSecs , TimeUnit.SECONDS);
            result = bag;
            times = 1;
        }};

        simpleRunnableMonitor.prep();
        Assert.assertTrue(simpleRunnableMonitor.inProgress());
        simpleRunnableMonitor.execute();
        Assert.assertTrue(simpleRunnableMonitor.inProgress());
        simpleRunnableMonitor.finish(false);

        Assert.assertFalse(simpleRunnableMonitor.inProgress());
        simpleRunnableMonitor.prep();
        Assert.assertTrue(simpleRunnableMonitor.inProgress());
        simpleRunnableMonitor.execute();
        Assert.assertTrue(simpleRunnableMonitor.inProgress());
        simpleRunnableMonitor.finish(false);
        Assert.assertFalse(simpleRunnableMonitor.inProgress());



    }

    /**
     * Test if the run advance caused time to advance as expected.
     */
    @Test
    public void testIsTimeToRunWithInitial() throws AlertException, IOException {
        LOG.info("Starting tests");
        try {
            final long l = System.currentTimeMillis() / 1000;

            long runTimeSecs = AlertUtils.getBatchTime(l,AlertUtils.dataFrequencyInSecs);

            SimpleRunnableMonitor simpleRunnableMonitor = new SimpleRunnableMonitor(controlledAlertExecutor,enrichmentProcessor,runTimeSecs);
            final MetricAlertConfig metricAlertConfig = TestUtil.getMetricAlertConfig(FILE);
            AlertEventBag bag = new AlertEventBag(new ArrayList<>(),metricAlertConfig);
            new Expectations() {{
                controlledAlertExecutor.evaluate(runTimeSecs +AlertUtils.dataFrequencyInSecs, TimeUnit.SECONDS);
                result = bag;
                times = 1;
                enrichmentProcessor.process(bag);
                result = true;
                times = 1;
            }};
            simpleRunnableMonitor.prep();
            simpleRunnableMonitor.execute();
            simpleRunnableMonitor.finish(false);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
