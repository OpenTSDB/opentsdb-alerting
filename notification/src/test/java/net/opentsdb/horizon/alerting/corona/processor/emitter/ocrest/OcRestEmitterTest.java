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

package net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockit.Capturing;
import mockit.Verifications;
import net.opentsdb.horizon.alerting.corona.TestData;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.impl.DefaultOcRestFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import net.opentsdb.horizon.alerting.corona.testutils.Utils;
import net.opentsdb.utils.JSON;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OcRestEmitterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Capturing
    OcRestClient ocRestClient;

    @BeforeAll
    public static void setup()
    {
        Views.initialize(Views.config()
                .setHorizonUrl("https://opentsdb.net")
                .setSplunkUrl("https://splunk.opentsdb.net/splunk")
                .setSplunkIndex("corona-alerts")
                .setSplunkLocale("en-US")
        );
    }

    @BeforeAll
    static void stubMonitoring()
    {
        AppMonitor.initialize(
                AppMonitor.config()
                        .setApplication("corona.test")
                        .setNamespace("Skhegay")
                        .setHost("localhost")
        );
    }

    @Test
    void processSingleMetricAlert() throws IOException
    {
        doProcess(
                AlertType.SINGLE_METRIC,
                "test/OcRestEmitterTest_processSingleMetricAlert.golden"
        );
    }

    @Test
    void processHealthCheckAlert() throws IOException
    {
        doProcess(
                AlertType.HEALTH_CHECK,
                "test/OcRestEmitterTest_processHealthCheckAlert.golden"
        );
    }

    @Test
    void processEventAlert() throws IOException
    {
        doProcess(
                AlertType.EVENT,
                "test/OcRestEmitterTest_processEventAlert.golden"
        );
    }

    private void doProcess(AlertType alertType, String goldenFile) throws IOException
    {
        getEmitter().process(
                TestData.getMessageKit(Contact.Type.OC, alertType)
        );

        new Verifications() {{
            final List<OcRestEvent> captured = new ArrayList<>();
            ocRestClient.send(withCapture(captured));

            final List<MockOcRestEvent> actual = new ArrayList<>();
            for (OcRestEvent e : captured) {
                String serialized = OBJECT_MAPPER.writeValueAsString(e);

                // Use this output for generating golden files.
                System.out.println(serialized);

                MockOcRestEvent event =
                        OBJECT_MAPPER.readValue(serialized, MockOcRestEvent.class);
                actual.add(event);
            }

            final List<MockOcRestEvent> expected = getExpected(goldenFile);

            expected.sort(OcRestEmitterTest::compare);
            actual.sort(OcRestEmitterTest::compare);

            assertEquals(expected, actual);
        }};
    }

    private OcRestEmitter getEmitter()
    {
        return OcRestEmitter.builder()
                .setClient(ocRestClient)
                .setFormatter(DefaultOcRestFormatter.builder()
                        .setHostname("localhost")
                        .build()
                )
                .build();
    }

    private List<MockOcRestEvent> getExpected(String resourceName)
            throws IOException
    {
        return OBJECT_MAPPER.readValue(
                Utils.load(resourceName),
                new TypeReference<List<MockOcRestEvent>>() {
                }
        );
    }

    private static int compare(MockOcRestEvent a, MockOcRestEvent b) {
        return a.getSignature().compareTo(b.getSignature());
    }
}