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

package net.opentsdb.horizon.alerting.corona.processor.emitter.email;

import net.opentsdb.horizon.alerting.corona.TestData;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class EmailEmitterTest {

    static final String SMTP_HOST = "mta.opentsdb.net";

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

    EmailEmitter getEmitter()
    {
        return EmailEmitter.builder()
                .setEmailClient(new EmailClient(SMTP_HOST, 200))
                .setFormatter(new EmailFormatter("TEST - "))
                .build();
    }

    @Test
    void processSingleMetricAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.EMAIL,
                        AlertType.SINGLE_METRIC
                )
        );
    }

    @Test
    void processHealthCheckAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.EMAIL,
                        AlertType.HEALTH_CHECK
                )
        );
    }

    @Test
    void processEventAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.EMAIL,
                        AlertType.EVENT
                )
        );
    }


    @Test
    void processPeriodOverPeriodAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.EMAIL,
                        AlertType.PERIOD_OVER_PERIOD
                )
        );
    }
}
