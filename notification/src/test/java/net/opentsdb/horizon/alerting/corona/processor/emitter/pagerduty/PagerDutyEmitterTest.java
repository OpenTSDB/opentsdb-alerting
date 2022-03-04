package net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import mockit.Capturing;
import mockit.Verifications;
import net.opentsdb.horizon.alerting.corona.TestData;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty.impl.DefaultPagerDutyFormatter;
import net.opentsdb.horizon.alerting.corona.testutils.Utils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PagerDutyEmitterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Capturing
    PagerDutyClient pagerDutyClient;

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
                "test/pagerduty/SingleMetricAlert.golden",
                9
        );
    }

    @Test
    void processHealthCheckAlert() throws IOException {
        doProcess(
                AlertType.HEALTH_CHECK,
                "test/pagerduty/HealthCheckAlert.golden",
                4
        );
    }

    @Test
    void processEventAlert() throws IOException {
        doProcess(
                AlertType.EVENT,
                "test/pagerduty/EventAlert.golden",
                3
        );
    }

    @Test
    void processPeriodOverPeriodAlert() throws IOException {
        doProcess(
                AlertType.PERIOD_OVER_PERIOD,
                "test/pagerduty/PeriodOverPeriodAlert.golden",
                2
        );
    }

    private void doProcess(AlertType alertType, String goldenFile, int expectedCaptures) throws IOException {
        getEmitter().process(
                TestData.getMessageKit(Contact.Type.PAGERDUTY, alertType)
        );

        new Verifications() {{
            final List<PagerDutyEvent> captured = new ArrayList<>();
            pagerDutyClient.send(withCapture(captured));

            assertEquals(expectedCaptures, captured.size());

            final List<MockPagerDutyEvent> actual = new ArrayList<>();

            for (PagerDutyEvent payload : captured) {
                String serialized = OBJECT_MAPPER.writeValueAsString(payload);

                // Use this output for generating golden files.
                System.out.println(serialized);

                MockPagerDutyEvent event =
                        OBJECT_MAPPER.readValue(serialized, MockPagerDutyEvent.class);
                actual.add(event);
            }

            final List<MockPagerDutyEvent> expected = getExpected(goldenFile);

            expected.sort(PagerDutyEmitterTest::compare);
            actual.sort(PagerDutyEmitterTest::compare);

            assertEquals(expected, actual);
        }};
    }

    private PagerDutyEmitter getEmitter() {
        return PagerDutyEmitter.builder()
                .setClient(pagerDutyClient)
                .setFormatter(DefaultPagerDutyFormatter.builder().build())
                .build();
    }

    private List<MockPagerDutyEvent> getExpected(String resourceName)
            throws IOException
    {
        return OBJECT_MAPPER.readValue(
                Utils.load(resourceName),
                new TypeReference<List<MockPagerDutyEvent>>() {
                }
        );
    }

    private static int compare(MockPagerDutyEvent a, MockPagerDutyEvent b) {
        return Integer.compare(a.hashCode(), b.hashCode());
    }
}
