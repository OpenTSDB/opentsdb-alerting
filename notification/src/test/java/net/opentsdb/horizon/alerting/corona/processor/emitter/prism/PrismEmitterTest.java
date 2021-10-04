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

package net.opentsdb.horizon.alerting.corona.processor.emitter.prism;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockit.Capturing;
import mockit.Verifications;
import net.opentsdb.horizon.alerting.corona.TestData;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.prism.impl.DefaultPrismFormatter;
import net.opentsdb.horizon.alerting.corona.testutils.Utils;
import net.opentsdb.utils.JSON;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrismEmitterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Capturing
    PrismClient prismClient;

    @BeforeAll
    static void stubMonitoring() {
        AppMonitor.initialize(
                AppMonitor.config()
                        .setApplication("corona.test")
                        .setNamespace("Skhegay")
                        .setHost("localhost")
        );
    }

    @Test
    void processSingleMetricAlert() throws IOException {
        doProcess(
                AlertType.SINGLE_METRIC,
                "test/PrismEmitterTest_processSingleMetricAlert.golden",
                9
        );
    }

    @Test
    void processHealthCheckAlert() throws IOException {
        doProcess(
                AlertType.HEALTH_CHECK,
                "test/PrismEmitterTest_processHealthCheckAlert.golden",
                4
        );
    }

    @Test
    void processEventAlert() throws IOException {
        doProcess(
                AlertType.EVENT,
                "test/PrismEmitterTest_processEventAlert.golden",
                3
        );
    }

    @Test
    void processPeriodOverPeriodAlert() throws IOException {
        doProcess(
                AlertType.PERIOD_OVER_PERIOD,
                "test/PrismEmitterTest_processPeriodOverPeriodAlert.golden",
                2
        );
    }

    private void doProcess(AlertType alertType, String goldenFile, int expectedCaptures) throws IOException {
        getEmitter().process(
                TestData.getMessageKit(Contact.Type.OC, alertType)
        );

        new Verifications() {{
            final List<String> captured = new ArrayList<>();
            prismClient.send(withCapture(captured));

            assertEquals(expectedCaptures, captured.size());

            final List<MockPrismEvent> actual = new ArrayList<>();

            for (String payload : captured) {
                final JsonParser parser = OBJECT_MAPPER.getFactory().createParser(payload);
                while (!parser.isClosed()) {
                    final MockPrismEvent event;
                    try {
                        event = OBJECT_MAPPER.readValue(parser, MockPrismEvent.class);
                    } catch (Exception e) {
                        break;
                    }
                    actual.add(event);
                }
            }

            final List<MockPrismEvent> expected = getExpected(goldenFile);

            expected.sort(PrismEmitterTest::compare);
            for (MockPrismEvent e : actual) {
                String serialized = OBJECT_MAPPER.writeValueAsString(e);

                // Use this output for generating golden files.
                System.out.println(serialized);
            }
            actual.sort(PrismEmitterTest::compare);

            assertEquals(expected, actual);
        }};
    }

    private PrismEmitter getEmitter() {
        return PrismEmitter.builder()
                .setClient(prismClient)
                .setFormatter(DefaultPrismFormatter.builder()
                        .setHostname("localhost")
                        .build()
                )
                .setMaxPayloadSizeBytes(1024)
                .build();
    }

    private List<MockPrismEvent> getExpected(String resourceName)
            throws IOException {
        return OBJECT_MAPPER.readValue(
                Utils.load(resourceName),
                new TypeReference<List<MockPrismEvent>>() {
                }
        );
    }

    private static int compare(MockPrismEvent a, MockPrismEvent b) {
        return a.getSignature().compareTo(b.getSignature());
    }
}