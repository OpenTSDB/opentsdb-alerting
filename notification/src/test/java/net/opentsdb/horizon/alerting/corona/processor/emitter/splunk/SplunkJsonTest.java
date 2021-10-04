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

package net.opentsdb.horizon.alerting.corona.processor.emitter.splunk;

import mockit.Capturing;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import net.opentsdb.horizon.alerting.corona.TestData;
import net.opentsdb.horizon.alerting.corona.component.Triple;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplunkJsonTest {

    @Mocked
    AppMonitor appMonitor;

    @Capturing
    Logger logger;

    @BeforeEach
    void stubStaticMethods() {
        new MockUp<AppMonitor>() {
            @Mock
            public AppMonitor get() {
                return appMonitor;
            }
        };
        new MockUp<LoggerFactory>() {
            @Mock
            public Logger getLogger(String loggerName) {
                return logger;
            }
        };
        new MockUp<LogTime>() {
            @Mock
            public String get() {
                return LogTime.DATE_FORMAT.format(new Date(0));
            }
        };
    }

    SplunkJson getTested() {
        return new SplunkJson();
    }

    String getGolden(final String resource) {
        final ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream =
                     classLoader.getResourceAsStream(resource)
        ) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void processShouldFilterOutSnoozedAlerts() {
        final Triple<AlertGroup, Metadata, Contacts> triple =
                new Triple<>(
                        TestData.getAlertGroup(AlertType.SINGLE_METRIC),
                        TestData.getMetadata(),
                        TestData.getContacts()
                );

        // Test
        getTested().process(triple);

        new Verifications() {{
            final List<String> expected = Arrays.asList(
                    getGolden("SplunkJsonTest_SnoozeFilter.golden").split("[\r\n]")
            );

            final List<String> actual = new ArrayList<>();
            logger.info(withCapture(actual));

            System.out.println(actual); // Visual verification.
            assertEquals(expected, actual);
        }};
    }

    @Test
    void processPeriodOverPeriodAlerts() {
        final Triple<AlertGroup, Metadata, Contacts> triple =
                new Triple<>(
                        TestData.getAlertGroup(AlertType.PERIOD_OVER_PERIOD),
                        TestData.getMetadata(),
                        TestData.getContacts()
                );

        // Test
        getTested().process(triple);

        new Verifications() {{
            final List<String> expected = Arrays.asList(
                    getGolden("SplunkJsonTest_PeriodOverPeriod.golden").split("[\r\n]")
            );

            final List<String> actual = new ArrayList<>();
            logger.info(withCapture(actual));

            System.out.println(actual); // Visual verification.
            assertEquals(expected, actual);
        }};
    }

    @Test
    void processHealthCheckAlerts() {
        final Triple<AlertGroup, Metadata, Contacts> triple =
                new Triple<>(
                        TestData.getAlertGroup(AlertType.HEALTH_CHECK),
                        TestData.getMetadata(),
                        TestData.getContacts()
                );

        // Test
        getTested().process(triple);

        new Verifications() {{
            final List<String> expected = Arrays.asList(
                    getGolden("SplunkJsonTest_HealthCheck.golden").split("[\r\n]")
            );

            final List<String> actual = new ArrayList<>();
            logger.info(withCapture(actual));

            System.out.println(actual); // Visual verification.
            assertEquals(expected, actual);
        }};
    }
}