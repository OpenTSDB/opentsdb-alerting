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

package net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie.formatter;

import net.opentsdb.horizon.alerting.corona.TestData;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie.OpsGenieAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OpsGenieSingleMetricAlertFormatterTest {

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
    public void singleAlert() {

        OpsGenieSingleMetricAlertFormatter ogFormatter =
                new OpsGenieSingleMetricAlertFormatter(
                        "OpenTSDB-Dev-Team",
                        "OpenTSDB"
                );

        MessageKit messageKit = TestData.getMessageKit(
                Contact.Type.OPSGENIE,
                AlertType.SINGLE_METRIC
        );

        String expectedNote = "<strong>4 Bad </strong> (Warn → Bad, Good → Bad), showing 3.</br>\n" +
                "<em>cpu.utilization.pct &gt; 9.000000 at least once in the last 0 minutes.</em></br></br>\n" +
                "\n" +
                "Value: <strong>89.0</strong>\n" +
                "<em>Tags: </em> env:<strong>prod</strong>&nbsp;&nbsp;host:<strong>localhost</strong><br/>\n" +
                "Value: <strong>90.0</strong>\n" +
                "<em>Tags: </em> env:<strong>stage</strong>&nbsp;&nbsp;host:<strong>localhost</strong><br/>\n" +
                "Value: <strong>91.0</strong>\n" +
                "<em>Tags: </em> env:<strong>dev</strong>&nbsp;&nbsp;host:<strong>localhost</strong><br/>\n" +
                "</br>\n" +
                "<strong>1 Warn </strong> (Good → Warn).</br>\n" +
                "<em>visitors.count &gt;= 999.000000 at least once in the last 0 minutes.</em></br></br>\n" +
                "\n" +
                "Value: <strong>999.0</strong>\n" +
                "<em>Tags: </em> env:<strong>stage</strong>&nbsp;&nbsp;host:<strong>localhost</strong>&nbsp;&nbsp;owner:<strong>skhegay</strong><br/>\n" +
                "</br>";
        OpsGenieAlert ogAlert = ogFormatter.format(messageKit);
        assertEquals("a1a905ef03a4fc3fbbfaea0f51d803cafc68552301b6dcba0cf7e88f73e41f3c",
                ogAlert.getAlias());
        assertEquals("Single Metric Alert. Host: localhost<br/><br/>Grouped by: <br/>[host:<strong>localhost</strong>].",
                ogAlert.getDescription());
        assertEquals(2,
                ogAlert.getDetails().size());
        assertEquals("https://splunk.opentsdb.net/splunk/en-US/app/search/search?q=search+index%3Dcorona-alerts+alert_id%3D0+earliest%3D07%2F23%2F2019%3A18%3A50%3A00UTC+latest%3D07%2F23%2F2019%3A19%3A05%3A00UTC+timeformat%3D%25m%2F%25d%2F%25Y%3A%25H%3A%25M%3A%25S%25Z",
                ogAlert.getDetails().get("OpenTSDB View Details"));
        assertEquals("https://opentsdb.net/a/0/view",
                ogAlert.getDetails().get("OpenTSDB Modify Alert"));
        assertEquals("CPU High Alert. Host: localhost",
                ogAlert.getMessage());
        assertEquals("OpenTSDB",
                ogAlert.getNamespace());
        assertEquals(StringUtils.deleteWhitespace(expectedNote),
                TestData.deleteCopyright(StringUtils.deleteWhitespace(ogAlert.getNote())));
        assertEquals("P5",
                ogAlert.getPriority());
        assertEquals(Collections.singletonList("Test_moog_1"),
                ogAlert.getVisibleToTeams());
        assertEquals("OpenTSDB",
                ogAlert.getSource());
        assertArrayEquals(new String[] {"host-localhost"},
                ogAlert.getTags());
        assertEquals("OpenTSDB-Dev-Team",
                ogAlert.getUser());
    }

}
