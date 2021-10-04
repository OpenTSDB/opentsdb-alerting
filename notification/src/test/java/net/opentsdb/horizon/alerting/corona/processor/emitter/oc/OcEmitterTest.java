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

package net.opentsdb.horizon.alerting.corona.processor.emitter.oc;

import mockit.Capturing;
import mockit.Verifications;
import net.opentsdb.horizon.alerting.corona.TestData;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OcEmitterTest {

    @Capturing
    private OcClient ocClient;

    @BeforeAll
    static void stubMonitoring() {
        AppMonitor.initialize(
                AppMonitor.config()
                        .setApplication("corona.test")
                        .setNamespace("Skhegay")
                        .setHost("localhost")
        );
    }

    OcEmitter getEmitter()
    {
        return new OcEmitter.Builder()
                .setClient(new OcClient())
                .setFormatter(new OcFormatter("us-west", "localhost"))
                .build();
    }

    @Test
    void processSingleMetricAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.OC,
                        AlertType.SINGLE_METRIC
                )
        );
    }

    @Test
    void processHealthCheckAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.OC,
                        AlertType.HEALTH_CHECK
                )
        );
    }

    @Test
    void processEventAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.OC,
                        AlertType.EVENT
                )
        );
    }

    @Test
    void processPeriodOverPeriodAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.OC,
                        AlertType.PERIOD_OVER_PERIOD
                )
        );
    }

    @Test
    void messageKitsWithDeniedNamespacesAreDropped() {
        final OcEmitter emitter = OcEmitter.builder()
                .setClient(ocClient)
                .setFormatter(new OcFormatter("us-west", "localhost"))
                .setDeniedNamespaces(Collections.singletonList("OpenTSDB"))
                .build();

        // Verify that message kit has the namespace which is deny-listed.
        final MessageKit mk = TestData.getMessageKit(
                Contact.Type.OC,
                AlertType.SINGLE_METRIC
        );
        assertEquals("OpenTSDB", mk.getNamespace());

        emitter.process(mk);

        new Verifications() {{
            ocClient.send((OcCommand) any, (OcContact) any);
            times = 0;
        }};
    }

    @Test
    void messageKitsWithNonDeniedNamespacesArePreserved() {
        final OcEmitter emitter = OcEmitter.builder()
                .setClient(ocClient)
                .setFormatter(new OcFormatter("us-west", "localhost"))
                .setDeniedNamespaces(Collections.singletonList("O2INFRA"))
                .build();

        // Verify that message kit doesn't have denied namespace
        final MessageKit mk = TestData.getMessageKit(
                Contact.Type.OC,
                AlertType.SINGLE_METRIC
        );
        assertEquals("OpenTSDB", mk.getNamespace());

        emitter.process(
                TestData.getMessageKit(
                        Contact.Type.OC,
                        AlertType.SINGLE_METRIC
                )
        );

        new Verifications() {{
            final List<OcCommand> commands = new ArrayList<>();
            ocClient.send(withCapture(commands), (OcContact) any);
            assertEquals(9, commands.size());
        }};
    }
}
