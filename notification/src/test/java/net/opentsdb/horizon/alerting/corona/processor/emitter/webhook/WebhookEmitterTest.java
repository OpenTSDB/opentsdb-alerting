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

package net.opentsdb.horizon.alerting.corona.processor.emitter.webhook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockit.Capturing;
import mockit.Verifications;
import net.opentsdb.horizon.alerting.corona.TestData;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.impl.DefaultWebhookFormatter;
import net.opentsdb.horizon.alerting.corona.testutils.Utils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebhookEmitterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String ENDPOINT = "https://test.endpoint.url";

    @Capturing
    WebhookClient webhookClient;

    @BeforeAll
    static void stubMonitoring() {
        AppMonitor.initialize(
                AppMonitor.config()
                        .setApplication("corona.test")
                        .setNamespace("test")
                        .setHost("localhost")
        );
    }

    @Test
    void processSingleMetricAlert() throws IOException {
        doProcess(
                AlertType.SINGLE_METRIC,
                "test/webhook/SingleMetricAlert.golden"
        );
    }

    @Test
    void processHealthCheckAlert() throws IOException {
        doProcess(
                AlertType.HEALTH_CHECK,
                "test/webhook/HealthCheckAlert.golden"
        );
    }

    @Test
    void processEventAlert() throws IOException {
        doProcess(
                AlertType.EVENT,
                "test/webhook/EventAlert.golden"
        );
    }

    @Test
    void processPeriodOverPeriodAlert() throws IOException {
        doProcess(
                AlertType.PERIOD_OVER_PERIOD,
                "test/webhook/PeriodOverPeriodAlert.golden"
        );
    }

    private void doProcess(AlertType alertType, String goldenFile) throws IOException {
        getEmitter().process(
                TestData.getMessageKit(Contact.Type.WEBHOOK, alertType)
        );

        new Verifications() {{
            final List<List<WebhookEvent>> captured = new ArrayList<>();
            webhookClient.send(withCapture(captured), ENDPOINT);
            final List<MockWebhookEvent> actual = new ArrayList<>();
            assertEquals(1, captured.size());
            for (WebhookEvent e : captured.get(0)) {
                String serialized = OBJECT_MAPPER.writeValueAsString(e);

                MockWebhookEvent event =
                        OBJECT_MAPPER.readValue(serialized, MockWebhookEvent.class);
                actual.add(event);
            }
            final List<MockWebhookEvent> expected = getExpected(goldenFile);
            expected.sort(WebhookEmitterTest::compare);
            actual.sort(WebhookEmitterTest::compare);
            assertEquals(expected, actual);
        }};
    }

    private WebhookEmitter getEmitter() {
        return WebhookEmitter.builder()
                .setClient(webhookClient)
                .setFormatter(DefaultWebhookFormatter.builder().build())
                .build();
    }

    private List<MockWebhookEvent> getExpected(String resourceName)
            throws IOException {
        return OBJECT_MAPPER.readValue(
                Utils.load(resourceName),
                new TypeReference<List<MockWebhookEvent>>() {
                }
        );
    }

    private static int compare(MockWebhookEvent a, MockWebhookEvent b) {
        return a.getSignature().compareTo(b.getSignature());
    }
}
