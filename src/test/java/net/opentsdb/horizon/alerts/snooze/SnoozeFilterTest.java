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

package net.opentsdb.horizon.alerts.snooze;

import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.SnoozeFetcher;
import net.opentsdb.horizon.alerts.core.TestUtil;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.model.MonitorEvent;
import net.opentsdb.horizon.alerts.model.Snooze;
import mockit.Expectations;
import mockit.Injectable;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static net.opentsdb.horizon.alerts.AlertUtils.ALERT_ID_TAG;
import static net.opentsdb.horizon.alerts.AlertUtils.ALERT_NAME_TAG;
import static net.opentsdb.horizon.alerts.AlertUtils.ALERT_TYPE_TAG;
import static net.opentsdb.horizon.alerts.AlertUtils.HORIZON_ALERT_TAG;

public class SnoozeFilterTest {

    private static final String TEST_DATA_ROOT = "src/test/resources/data/SnoozeFilterTest";

    @Injectable
    SnoozeFetcher snoozeFetcher;


    @Test
    public void snoozeOnAlertId() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-alertid.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                }};
            }
        };

        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        Assert.assertTrue("Condition for id: "+ alertConfig.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig));


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        Assert.assertFalse("Condition for id: "+ alertConfig1.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig1));

    }

    @Test
    public void snoozeOnAlertIdAndLabel() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-alertid-label.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        final String alertConfigPath_181 = TEST_DATA_ROOT + "/alertConfigs/alertid-181-label2.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                }};
            }
        };

        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        Assert.assertTrue("Condition for id: "+ alertConfig.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig));


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        Assert.assertTrue("Condition for id: "+ alertConfig1.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig1));

        final AlertConfig alertConfig2 = TestUtil.getMetricAlertConfig(alertConfigPath_181);

        Assert.assertFalse("Condition for id: "+ alertConfig2.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig2));

    }

    @Test
    public void snoozeOnLabel() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-label.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        final String alertConfigPath_181 = TEST_DATA_ROOT + "/alertConfigs/alertid-181-label2.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                }};
            }
        };

        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        Assert.assertTrue("Condition for id: "+ alertConfig.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig));


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        Assert.assertTrue("Condition for id: "+ alertConfig1.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig1));

        final AlertConfig alertConfig2 = TestUtil.getMetricAlertConfig(alertConfigPath_181);

        Assert.assertFalse("Condition for id: "+ alertConfig2.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig2));

    }

    @Test
    public void snoozeOnLabels() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-labels.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        final String alertConfigPath_181 = TEST_DATA_ROOT + "/alertConfigs/alertid-181-label2.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                }};
            }
        };

        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        Assert.assertTrue("Condition for id: "+ alertConfig.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig));


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        Assert.assertTrue("Condition for id: "+ alertConfig1.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig1));

        final AlertConfig alertConfig2 = TestUtil.getMetricAlertConfig(alertConfigPath_181);

        Assert.assertTrue("Condition for id: "+ alertConfig2.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig2));

    }

    @Test
    public void snoozeWithAlertIdAndTagsSingle() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-alertid-tag-filter.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        final String alertConfigPath_181 = TEST_DATA_ROOT + "/alertConfigs/alertid-181-label2.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                }};
            }
        };

        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        Assert.assertFalse("Condition for id: "+ alertConfig.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig));


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        Assert.assertFalse("Condition for id: "+ alertConfig1.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig1));

        final AlertConfig alertConfig2 = TestUtil.getMetricAlertConfig(alertConfigPath_181);

        Assert.assertFalse("Condition for id: "+ alertConfig2.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig2));

    }

    @Test
    public void snoozeWithAlertIdAndTags() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-alertid-tag-filter.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        final String alertConfigPath_181 = TEST_DATA_ROOT + "/alertConfigs/alertid-181-label2.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                    put("Region", "ap-northeast-2");
                    put("AwsId", "01");
                }};
            }
        };

        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        Assert.assertTrue("Condition for id: "+ alertConfig.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig));


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        Assert.assertFalse("Condition for id: "+ alertConfig1.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig1));

        final AlertConfig alertConfig2 = TestUtil.getMetricAlertConfig(alertConfigPath_181);

        Assert.assertFalse("Condition for id: "+ alertConfig2.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig2));

    }

    @Test
    public void snoozeWithTwoAlertIdAndTags() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-2alertid-tag-filter.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        final String alertConfigPath_181 = TEST_DATA_ROOT + "/alertConfigs/alertid-181-label2.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                    put("Region", "ap-northeast-2");
                    put("AwsId", "02");
                }};
            }
        };

        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        Assert.assertTrue("Condition for id: "+ alertConfig.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig));


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        Assert.assertTrue("Condition for id: "+ alertConfig1.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig1));

        final AlertConfig alertConfig2 = TestUtil.getMetricAlertConfig(alertConfigPath_181);

        Assert.assertFalse("Condition for id: "+ alertConfig2.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig2));

    }

    @Test
    public void snoozeWithOnlyTags() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-tag-filter.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        final String alertConfigPath_181 = TEST_DATA_ROOT + "/alertConfigs/alertid-181-label2.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                    put("Region", "ap-northeast-2");
                    put("AwsId", "03");
                }};
            }
        };

        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        Assert.assertTrue("Condition for id: "+ alertConfig.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig));


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        Assert.assertTrue("Condition for id: "+ alertConfig1.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig1));

        final AlertConfig alertConfig2 = TestUtil.getMetricAlertConfig(alertConfigPath_181);

        Assert.assertTrue("Condition for id: "+ alertConfig2.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig2));

    }

    @Test
    public void snoozeWithTagFilterWithoutChainWithLabel() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-tag-filter-without-chain-with-label.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_183 = TEST_DATA_ROOT + "/alertConfigs/alertid-183-foobar.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                    put("Region", "ap-northeast-1");
                    put("AwsId", "04");
                }};
            }
        };

        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        final AlertConfig alertConfig179 = TestUtil.getMetricAlertConfig(alertConfigPath_179);

        Assert.assertFalse("Condition for id: "+ alertConfig179.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig179));


        final AlertConfig alertConfig183 = TestUtil.getMetricAlertConfig(alertConfigPath_183);

        Assert.assertFalse("Condition for id: "+ alertConfig183.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig183));

        MonitorEvent monitorEventWithSnoozedTags = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "qux");
                    put("Region", "ap-northeast-2");
                    put("AwsId", "001523779248");
                }};
            }
        };

        Assert.assertFalse("Condition for id: "+ alertConfig179.getAlertId(),
                snoozeFilter.snooze(monitorEventWithSnoozedTags, alertConfig179));

        Assert.assertTrue("Condition for id: "+ alertConfig183.getAlertId(),
                snoozeFilter.snooze(monitorEventWithSnoozedTags, alertConfig183));


    }

    @Test
    public void snoozeWithTagFilterWithoutChain() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-tag-filter-without-chain.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_183 = TEST_DATA_ROOT + "/alertConfigs/alertid-183-foobar.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                    put("Region", "ap-northeast-2");
                    put("AwsId", "001523779248");
                }};
            }
        };

        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        final AlertConfig alertConfig179 = TestUtil.getMetricAlertConfig(alertConfigPath_179);

        Assert.assertFalse("Condition for id: "+ alertConfig179.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig179));


        final AlertConfig alertConfig183 = TestUtil.getMetricAlertConfig(alertConfigPath_183);

        Assert.assertFalse("Condition for id: "+ alertConfig183.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig183));

        MonitorEvent monitorEventWithSnoozedTags = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "qux");
                    put("Region", "ap-northeast-2");
                    put("AwsId", "001523779248");
                }};
            }
        };

        Assert.assertTrue("Condition for id: "+ alertConfig179.getAlertId(),
                snoozeFilter.snooze(monitorEventWithSnoozedTags, alertConfig179));

        Assert.assertTrue("Condition for id: "+ alertConfig183.getAlertId(),
                snoozeFilter.snooze(monitorEventWithSnoozedTags, alertConfig183));


    }

    @Test
    public void snoozeWithOnlyTagsNot() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-tag-filter.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        final String alertConfigPath_181 = TEST_DATA_ROOT + "/alertConfigs/alertid-181-label2.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                    put("Region", "ap-northeast-2");
                    put("AwsId", "001523779248");
                }};
            }
        };

        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        Assert.assertFalse("Condition for id: "+ alertConfig.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig));


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        Assert.assertFalse("Condition for id: "+ alertConfig1.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig1));

        final AlertConfig alertConfig2 = TestUtil.getMetricAlertConfig(alertConfigPath_181);

        Assert.assertFalse("Condition for id: "+ alertConfig2.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig2));

    }

    @Test
    public void snoozeWithLabelTagFilter() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-label-tag-filter.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        final String alertConfigPath_181 = TEST_DATA_ROOT + "/alertConfigs/alertid-181-label2.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                    put("Region", "ap-northeast-2");
                    put("AwsId", "02");
                }};
            }
        };

        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        Assert.assertTrue("Condition for id: "+ alertConfig.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig));


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        Assert.assertTrue("Condition for id: "+ alertConfig1.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig1));

        final AlertConfig alertConfig2 = TestUtil.getMetricAlertConfig(alertConfigPath_181);

        Assert.assertFalse("Condition for id: "+ alertConfig2.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig2));

    }

    @Test
    public void snoozeWithAlertIdLabelTagFilter() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-alertid-label-tag-filter.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        final String alertConfigPath_181 = TEST_DATA_ROOT + "/alertConfigs/alertid-181-label2.json";
        final String alertConfigPath_182 = TEST_DATA_ROOT + "/alertConfigs/alertid-182-label2.json";
        SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        MonitorEvent monitorEvent = new MonitorEvent() {
            @Override
            public String getNamespace() {
                return "NS";
            }

            @Override
            public Map<String, String> getTags() {
                return new HashMap<String, String>(){{
                    put("host", "proc.den.opentsdb.net");
                    put("Region", "ap-southeast-1");
                    put("AwsId", "03");
                }};
            }
        };

        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        Assert.assertTrue("Condition for id: "+ alertConfig.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig));


        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);

        Assert.assertTrue("Condition for id: "+ alertConfig1.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig1));

        final AlertConfig alertConfig2 = TestUtil.getMetricAlertConfig(alertConfigPath_181);

        Assert.assertTrue("Condition for id: "+ alertConfig2.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig2));

        final AlertConfig alertConfig3 = TestUtil.getMetricAlertConfig(alertConfigPath_182);

        Assert.assertFalse("Condition for id: "+ alertConfig3.getAlertId(),
                snoozeFilter.snooze(monitorEvent, alertConfig3));

    }


    @Test
    public void snoozeWithSystemGeneratedTagFilter() throws IOException {

        final String configPath = TEST_DATA_ROOT + "/config-with-system-tag-filter.json";
        final String alertConfigPath_179 = TEST_DATA_ROOT + "/alertConfigs/alertid-179-label1.json";
        final String alertConfigPath_180 = TEST_DATA_ROOT + "/alertConfigs/alertid-180-label1.json";
        final String alertConfigPath_181 = TEST_DATA_ROOT + "/alertConfigs/alertid-181-label2.json";


        final Map<Long,Snooze> snoozes = Collections.singletonMap(1l,
                TestUtil.makeActive(
                        TestUtil.getSnoozeCoonfigFromFile(
                                configPath
                        )
                )
        );

        new Expectations() {{

            snoozeFetcher.getSnoozeConfig();
            result = snoozes;
            times = 1;

        }};

        final SnoozeFilter snoozeFilter = new SnoozeFilter(snoozeFetcher);

        final Supplier<AlertEvent> supplier = () ->
                new AlertEvent() {
                    final Map<String, String> tags = new HashMap<String, String>(){
                        {
                            put("host", "proc.den.opentsdb.net");
                            put("Region", "ap-northeast-1");
                            put("AwsId", "04");
                        }};
                    @Override
                    public String getNamespace() {
                        return "NS";
                    }

                    @Override
                    public Map<String, String> getTags() {
                        return tags;
                    }
                };

        final BiConsumer<AlertConfig,Boolean> tester = (config, condition) -> {

            SnoozeAlert snoozeAlert = new SnoozeAlert(snoozeFilter);

            AlertEvent alert = supplier.get();

            ArrayList<AlertEvent> monitorEvents = new ArrayList<>();
            monitorEvents.add(alert);
            AlertEventBag alertEventBag = new AlertEventBag(monitorEvents, config);
            snoozeAlert.process(alertEventBag);
            Assert.assertEquals("Condition for id: " + config.getAlertId(), condition, alert.isSnoozed());

            Map<String, String> tags = alert.getTags();

            Assert.assertTrue((
                    !tags.containsKey(ALERT_ID_TAG) &&
                            !tags.containsKey(ALERT_NAME_TAG) &&
                            !tags.containsKey(ALERT_TYPE_TAG) &&
                            !tags.containsKey(HORIZON_ALERT_TAG)
            ));


        };


        final AlertConfig alertConfig = TestUtil.getMetricAlertConfig(alertConfigPath_179);
        final AlertConfig alertConfig1 = TestUtil.getHealthCheckConfig(alertConfigPath_180);
        final AlertConfig alertConfig2 = TestUtil.getMetricAlertConfig(alertConfigPath_181);

        tester.accept(alertConfig, true);
        tester.accept(alertConfig1, false);
        tester.accept(alertConfig2, true);

    }
}
