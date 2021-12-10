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
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class EmailFormatterTest {

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

    @Test
    public void testFormat()
    {
        final EmailFormatter newFormatter = new EmailFormatter("TEST - ");

        final EmailMessage message =
                newFormatter.format(
                        TestData.getMessageKit(
                                Contact.Type.EMAIL,
                                AlertType.SINGLE_METRIC
                        )
                );

        assertThat(message, notNullValue());
    }
}