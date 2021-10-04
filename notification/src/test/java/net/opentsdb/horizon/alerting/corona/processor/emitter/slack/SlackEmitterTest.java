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

package net.opentsdb.horizon.alerting.corona.processor.emitter.slack;

import net.opentsdb.horizon.alerting.corona.TestData;
import net.opentsdb.horizon.alerting.corona.component.http.CloseableHttpClientBuilder;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.impl.SlackClientImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SlackEmitterTest {

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

    SlackEmitter getEmitter()
    {
        final SlackClient client =
                new SlackClientImpl(CloseableHttpClientBuilder.create()
                        .setTLSEnabled(false)
                        .build());
        final SlackFormatter formatter = new SlackFormatter();
        return SlackEmitter.builder()
                .setClient(client)
                .setFormatter(formatter)
                .build();
    }

    @Test
    void processSingleMetricAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.SLACK,
                        AlertType.SINGLE_METRIC
                )
        );
    }

    @Test
    void processHealthCheckAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.SLACK,
                        AlertType.HEALTH_CHECK
                )
        );
    }

    @Test
    void processEventAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.SLACK,
                        AlertType.EVENT
                )
        );
    }

    @Test
    void processPeriodOverPeriodAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.SLACK,
                        AlertType.PERIOD_OVER_PERIOD
                )
        );
    }
}