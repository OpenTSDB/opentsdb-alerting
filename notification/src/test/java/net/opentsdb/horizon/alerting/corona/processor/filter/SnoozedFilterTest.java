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

package net.opentsdb.horizon.alerting.corona.processor.filter;

import mockit.Expectations;
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
import net.opentsdb.horizon.alerting.corona.processor.Processor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SnoozedFilterTest {

    @Mocked
    Processor<Triple<AlertGroup, Metadata, Contacts>> nextProcessor;

    @Mocked
    AppMonitor appMonitor;

    @BeforeEach
    void stubAppMonitor()
    {
        new MockUp<AppMonitor>() {
            @Mock
            public AppMonitor get()
            {
                return appMonitor;
            }
        };
    }

    public SnoozedFilter getTested()
    {
        return new SnoozedFilter(nextProcessor);
    }

    @Test
    @SuppressWarnings("unchecked")
    void processShouldFilterOutSnoozedAlerts()
    {
        final Triple<AlertGroup, Metadata, Contacts> triple =
                new Triple<>(
                        TestData.getAlertGroup(AlertType.SINGLE_METRIC),
                        TestData.getMetadata(),
                        TestData.getContacts()
                );

        new Expectations() {{

            nextProcessor.process((Triple<AlertGroup, Metadata, Contacts>) any);
            times = 1;
        }};

        // Test
        getTested().process(triple);

        // Instrumentation code is triggered.
        new Verifications() {{
            appMonitor.countAlertSnoozed(6, "OpenTSDB");
        }};

        // Resulting alert group doesn't contain snoozed alerts.
        new Verifications() {{
            final Triple<AlertGroup, Metadata, Contacts> result;
            nextProcessor.process(result = withCapture());

            assertNotNull(result.getFirst());
            result.getFirst().getAlerts()
                    .forEach(alert -> assertFalse(alert.isSnoozed()));
        }};
    }
}