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

package net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie;

import mockit.Expectations;
import mockit.Injectable;
import net.opentsdb.horizon.alerting.corona.TestData;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.HealthCheckAlert;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OpsGenieContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OpsGenieMeta;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

class OpsGenieEmitterTest {

    @Injectable
    OpsGenieClient opsGenieClient;

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

    OpsGenieEmitter getEmitter()
    {
        final OpsGenieClient client = new OpsGenieClient();
        final OpsGenieFormatter formatter =
                OpsGenieFormatter.builder()
                        .setUser("OpenTSDB-Test-User")
                        .setSource("OpenTSDB-Test")
                        .build();

        return OpsGenieEmitter.builder()
                .setClient(client)
                .setFormatter(formatter)
                .setMaxSendAttempts(5)
                .build();
    }

    OpsGenieEmitter getEmitterWithMockedClient()
    {
        final OpsGenieFormatter formatter =
                OpsGenieFormatter.builder()
                        .setUser("OpenTSDB-Test-User")
                        .setSource("OpenTSDB-Test")
                        .build();

        return OpsGenieEmitter.builder()
                .setClient(opsGenieClient)
                .setFormatter(formatter)
                .setMaxSendAttempts(1)
                .build();
    }

    @Test
    void processSingleMetricAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.OPSGENIE,
                        AlertType.SINGLE_METRIC
                )
        );
    }

    @Test
    void processHealthCheckAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.OPSGENIE,
                        AlertType.HEALTH_CHECK
                )
        );
    }

    /**
     * Single message kit should be split in multiple for HealthCheck alerts.
     */
    @Test
    void processHealthCheckAlert_MessageKitShouldBeSplitIntoMultiplePerAlert()
    {
        final OpsGenieEmitter emitter = getEmitterWithMockedClient();

        final MessageKit messageKit = MessageKit.builder()
                .setType(Contact.Type.OPSGENIE)
                .setMeta(OpsGenieMeta.builder()
                        .setSubject("Jenkins Job Result")
                        .setBody("Health Check Alert body.")
                        .setOpsGeniePriority("P5")
                        .setOpsGenieAutoClose(true)
                        .build()
                )
                .setContacts(OpsGenieContact.builder()
                        .setName("Test OpsGenie Contact")
                        .setApiKey("secret.opsgenie.api.key")
                        .build()
                )
                .setAlertGroup(AlertGroup.builder()
                        .setGroupKey(GroupKey.builder()
                                .setAlertId(42)
                                .setAlertType(AlertType.HEALTH_CHECK)
                                .setNamespace("OpenTSDB")
                                .setKeys("env")
                                .setValues("prod")
                                .build()
                        )
                        .setAlerts(Arrays.asList(
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-1")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec())
                                        .setStateFrom(State.GOOD)
                                        .setState(State.BAD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 3))
                                        .setStates(State.BAD, State.WARN, State.GOOD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build(),
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-2")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec())
                                        .setStateFrom(State.GOOD)
                                        .setState(State.WARN)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 4))
                                        .setStates(State.BAD, State.WARN, State.GOOD, State.BAD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build()
                        ))
                        .build()
                )
                .build();

        new Expectations() {{
            final OpsGenieAlert firstAlert = OpsGenieAlert.builder()
                    .setNamespace("OpenTSDB")
                    .setMessage("Jenkins Job Result")
                    .setAlias("2d1385ae5775e97f5837c5f126fa30db69c97a39d6d47958960143c5f1851fcf")
                    .setDescription("Health Check Alert body.<br/><br/><strong>Grouped by</strong>:<br/>host:<strong>host-1</strong>, env:<strong>prod</strong>")
                    .setSource("OpenTSDB-Test")
                    .setTags()
                    .setUser("OpenTSDB-Test-User")
                    .setPriority("P5")
                    .addVisibleToTeams("Test_moog_1")
                    .setGeneralNote("<h4>Bad [1]</h4>\n[1] <strong>test.namespace: test.application</strong> was in the <strong>bad</strong> state for at least <strong>9 times.</strong> GOOD&rarr;BAD.<br/>\n<strong>Message</strong>:  Very detailed info<br />\n<strong>Tags</strong>:  env:<strong>prod</strong>,  host:<strong>host-1</strong><br/>\nTue Jul 23, 2019 06:57:42 PM UTC<hr/>")
                    .setRecoveryNote("")
                    .setCanBeClosed(false)
                    .setIncludeRecoveryNote(false)
                    .addDetail("OpenTSDB View Details", "https://splunk.opentsdb.net/splunk/en-US/app/search/search?q=search+index%3Dcorona-alerts+alert_id%3D42+earliest%3D07%2F23%2F2019%3A18%3A50%3A00UTC+latest%3D07%2F23%2F2019%3A19%3A05%3A00UTC+timeformat%3D%25m%2F%25d%2F%25Y%3A%25H%3A%25M%3A%25S%25Z")
                    .addDetail("OpenTSDB Modify Alert", "https://opentsdb.net/a/42/view")
                    .build();
            // TODO - Exact comparison again. The copyrights goof it up.
            opsGenieClient.isActive("secret.opsgenie.api.key", withAny(firstAlert));
            result = Optional.of(Boolean.FALSE);
            opsGenieClient.create("secret.opsgenie.api.key", withAny(firstAlert.removeRecoveryNote()));
            result = true;

            final OpsGenieAlert secondAlert = OpsGenieAlert.builder()
                    .setNamespace("OpenTSDB")
                    .setMessage("Jenkins Job Result")
                    .setAlias("82b696633575335e116dd5cc31ee84a9f25eccbac57a6ac357c2e00853e51900")
                    .setDescription("Health Check Alert body.<br/><br/><strong>Grouped by</strong>:<br/>host:<strong>host-2</strong>, env:<strong>prod</strong>")
                    .setSource("OpenTSDB-Test")
                    .setTags()
                    .setUser("OpenTSDB-Test-User")
                    .setPriority("P5")
                    .addVisibleToTeams("Test_moog_1")
                    .setGeneralNote("<h4>Warn [1]</h4>\n[1] <strong>test.namespace: test.application</strong> was in the <strong>warn</strong> state for at least <strong>9 times.</strong> GOOD&rarr;WARN.<br/>\n<strong>Message</strong>:  Very detailed info<br />\n<strong>Tags</strong>:  env:<strong>prod</strong>,  host:<strong>host-2</strong><br/>\nTue Jul 23, 2019 06:57:42 PM UTC<hr/>")
                    .setRecoveryNote("")
                    .setCanBeClosed(false)
                    .setIncludeRecoveryNote(false)
                    .addDetail("OpenTSDB View Details", "https://splunk.opentsdb.net/splunk/en-US/app/search/search?q=search+index%3Dcorona-alerts+alert_id%3D42+earliest%3D07%2F23%2F2019%3A18%3A50%3A00UTC+latest%3D07%2F23%2F2019%3A19%3A05%3A00UTC+timeformat%3D%25m%2F%25d%2F%25Y%3A%25H%3A%25M%3A%25S%25Z")
                    .addDetail("OpenTSDB Modify Alert", "https://opentsdb.net/a/42/view")
                    .build();
            // TODO - Exact comparison again. The copyrights goof it up.
            opsGenieClient.isActive("secret.opsgenie.api.key", withAny(secondAlert));
            result = Optional.of(Boolean.FALSE);
            opsGenieClient.create("secret.opsgenie.api.key", withAny(secondAlert.removeRecoveryNote()));
            result = true;
        }};

        emitter.process(messageKit);
    }

    /**
     * Recovery alert should trigger closure of an active alert.
     */
    @Test
    void processHealthCheckAlert_RecoveryAlertTriggersClosureOfActiveAlert()
    {
        final OpsGenieEmitter emitter = getEmitterWithMockedClient();

        final MessageKit messageKit = MessageKit.builder()
                .setType(Contact.Type.OPSGENIE)
                .setMeta(OpsGenieMeta.builder()
                        .setSubject("Jenkins Job Result")
                        .setBody("Health Check Alert body.")
                        .setOpsGeniePriority("P5")
                        .setOpsGenieAutoClose(true)
                        .build()
                )
                .setContacts(OpsGenieContact.builder()
                        .setName("Test OpsGenie Contact")
                        .setApiKey("secret.opsgenie.api.key")
                        .build()
                )
                .setAlertGroup(AlertGroup.builder()
                        .setGroupKey(GroupKey.builder()
                                .setAlertId(42)
                                .setAlertType(AlertType.HEALTH_CHECK)
                                .setNamespace("OpenTSDB")
                                .setKeys("env")
                                .setValues("prod")
                                .build()
                        )
                        .setAlerts(Collections.singletonList(
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-1")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec())
                                        .setStateFrom(State.BAD)
                                        .setState(State.GOOD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 3))
                                        .setStates(State.BAD, State.WARN, State.GOOD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build()
                        ))
                        .build()
                )
                .build();

        new Expectations() {{
            final OpsGenieAlert firstAlert = OpsGenieAlert.builder()
                    .setNamespace("OpenTSDB")
                    .setMessage("Jenkins Job Result")
                    .setAlias("2d1385ae5775e97f5837c5f126fa30db69c97a39d6d47958960143c5f1851fcf")
                    .setDescription("Health Check Alert body.<br/><br/><strong>Grouped by</strong>:<br/>host:<strong>host-1</strong>, env:<strong>prod</strong>")
                    .setSource("OpenTSDB-Test")
                    .setTags()
                    .setUser("OpenTSDB-Test-User")
                    .setPriority("P5")
                    .addVisibleToTeams("Test_moog_1")
                    .setGeneralNote("")
                    .setRecoveryNote("<h4>Recovery [1]</h4>\n[1] <strong>test.namespace: test.application</strong> was in the <strong>good</strong> state for at least <strong>9 times.</strong> BAD&rarr;GOOD.<br/>\n<strong>Message</strong>:  Very detailed info<br />\n<strong>Tags</strong>:  env:<strong>prod</strong>,  host:<strong>host-1</strong><br/>\nTue Jul 23, 2019 06:57:42 PM UTC<hr/>")
                    .setCanBeClosed(true)
                    .setIncludeRecoveryNote(true)
                    .addDetail("OpenTSDB View Details", "https://splunk.opentsdb.net/splunk/en-US/app/search/search?q=search+index%3Dcorona-alerts+alert_id%3D42+earliest%3D07%2F23%2F2019%3A18%3A50%3A00UTC+latest%3D07%2F23%2F2019%3A19%3A05%3A00UTC+timeformat%3D%25m%2F%25d%2F%25Y%3A%25H%3A%25M%3A%25S%25Z")
                    .addDetail("OpenTSDB Modify Alert", "https://opentsdb.net/a/42/view")
                    .build();
            // TODO - Exact comparison again. The copyrights goof it up.
            opsGenieClient.isActive("secret.opsgenie.api.key", withAny(firstAlert));
            result = Optional.of(Boolean.TRUE);
            opsGenieClient.close("secret.opsgenie.api.key", withAny(firstAlert));
            result = true;
        }};

        emitter.process(messageKit);
    }

    /**
     * Recovery alert should not be sent if there is no active alert.
     */
    @Test
    void processHealthCheckAlert_RecoveryAlertIsNotSentWhenNoActive()
    {
        final OpsGenieEmitter emitter = getEmitterWithMockedClient();

        final MessageKit messageKit = MessageKit.builder()
                .setType(Contact.Type.OPSGENIE)
                .setMeta(OpsGenieMeta.builder()
                        .setSubject("Jenkins Job Result")
                        .setBody("Health Check Alert body.")
                        .setOpsGeniePriority("P5")
                        .setOpsGenieAutoClose(true)
                        .build()
                )
                .setContacts(OpsGenieContact.builder()
                        .setName("Test OpsGenie Contact")
                        .setApiKey("secret.opsgenie.api.key")
                        .build()
                )
                .setAlertGroup(AlertGroup.builder()
                        .setGroupKey(GroupKey.builder()
                                .setAlertId(42)
                                .setAlertType(AlertType.HEALTH_CHECK)
                                .setNamespace("OpenTSDB")
                                .setKeys("env")
                                .setValues("prod")
                                .build()
                        )
                        .setAlerts(Collections.singletonList(
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-1")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec())
                                        .setStateFrom(State.BAD)
                                        .setState(State.GOOD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 3))
                                        .setStates(State.BAD, State.WARN, State.GOOD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build()
                        ))
                        .build()
                )
                .build();

        new Expectations() {{
            final OpsGenieAlert firstAlert = OpsGenieAlert.builder()
                    .setNamespace("OpenTSDB")
                    .setMessage("Jenkins Job Result")
                    .setAlias("2d1385ae5775e97f5837c5f126fa30db69c97a39d6d47958960143c5f1851fcf")
                    .setDescription("Health Check Alert body.<br/><br/><strong>Grouped by</strong>:<br/>host:<strong>host-1</strong>, env:<strong>prod</strong>")
                    .setSource("OpenTSDB-Test")
                    .setTags()
                    .setUser("OpenTSDB-Test-User")
                    .setPriority("P5")
                    .addVisibleToTeams("Test_moog_1")
                    .setGeneralNote("")
                    .setRecoveryNote("<h4>Recovery [1]</h4>\n[1] <strong>test.namespace: test.application</strong> was in the <strong>good</strong> state for at least <strong>9 times.</strong> BAD&rarr;GOOD.<br/>\n<strong>Message</strong>:  Very detailed info<br />\n<strong>Tags</strong>:  env:<strong>prod</strong>,  host:<strong>host-1</strong><br/>\nTue Jul 23, 2019 06:57:42 PM UTC<hr/>")
                    .setCanBeClosed(true)
                    .setIncludeRecoveryNote(true)
                    .addDetail("OpenTSDB View Details", "https://splunk.opentsdb.net/splunk/en-US/app/search/search?q=search+index%3Dcorona-alerts+alert_id%3D42+earliest%3D07%2F23%2F2019%3A18%3A50%3A00UTC+latest%3D07%2F23%2F2019%3A19%3A05%3A00UTC+timeformat%3D%25m%2F%25d%2F%25Y%3A%25H%3A%25M%3A%25S%25Z")
                    .addDetail("OpenTSDB Modify Alert", "https://opentsdb.net/a/42/view")
                    .build();
            // TODO - Exact comparison again. The copyrights goof it up.
            opsGenieClient.isActive("secret.opsgenie.api.key", withAny(firstAlert));
            result = Optional.of(Boolean.FALSE); // Indicate that the alert is not active.
            opsGenieClient.close("secret.opsgenie.api.key", withAny(firstAlert));
            times = 0;
            opsGenieClient.create("secret.opsgenie.api.key", withAny(firstAlert));
            times = 0;
        }};

        emitter.process(messageKit);
    }

    /**
     * Single message kit should be split in multiple for HealthCheck alerts.
     * In this test, the first alert should update the notes of an existing alert,
     * the second should close existing alert.
     */
    @Test
    void processHealthCheckAlert_NewAlertAndRecoveryAlertAreSentSeparately()
    {
        final OpsGenieEmitter emitter = getEmitterWithMockedClient();

        final MessageKit messageKit = MessageKit.builder()
                .setType(Contact.Type.OPSGENIE)
                .setMeta(OpsGenieMeta.builder()
                        .setSubject("Jenkins Job Result")
                        .setBody("Health Check Alert body.")
                        .setOpsGeniePriority("P5")
                        .setOpsGenieAutoClose(true)
                        .build()
                )
                .setContacts(OpsGenieContact.builder()
                        .setName("Test OpsGenie Contact")
                        .setApiKey("secret.opsgenie.api.key")
                        .build()
                )
                .setAlertGroup(AlertGroup.builder()
                        .setGroupKey(GroupKey.builder()
                                .setAlertId(42)
                                .setAlertType(AlertType.HEALTH_CHECK)
                                .setNamespace("OpenTSDB")
                                .setKeys("env")
                                .setValues("prod")
                                .build()
                        )
                        .setAlerts(Arrays.asList(
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-1")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec())
                                        .setStateFrom(State.GOOD)
                                        .setState(State.BAD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 3))
                                        .setStates(State.BAD, State.WARN, State.GOOD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build(),
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-2")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec())
                                        .setStateFrom(State.WARN)
                                        .setState(State.GOOD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 4))
                                        .setStates(State.BAD, State.WARN, State.GOOD, State.BAD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build()
                        ))
                        .build()
                )
                .build();

        new Expectations() {{
            final OpsGenieAlert.Builder firstAlertBuilder = OpsGenieAlert.builder()
                    .setNamespace("OpenTSDB")
                    .setMessage("Jenkins Job Result")
                    .setAlias("2d1385ae5775e97f5837c5f126fa30db69c97a39d6d47958960143c5f1851fcf")
                    .setDescription("Health Check Alert body.<br/><br/><strong>Grouped by</strong>:<br/>host:<strong>host-1</strong>, env:<strong>prod</strong>")
                    .setSource("OpenTSDB-Test")
                    .setTags()
                    .setUser("OpenTSDB-Test-User")
                    .setPriority("P5")
                    .addVisibleToTeams("Test_moog_1")
                    .setGeneralNote("<h4>Bad [1]</h4>\n[1] <strong>test.namespace: test.application</strong> was in the <strong>bad</strong> state for at least <strong>9 times.</strong> GOOD&rarr;BAD.<br/>\n<strong>Message</strong>:  Very detailed info<br />\n<strong>Tags</strong>:  env:<strong>prod</strong>,  host:<strong>host-1</strong><br/>\nTue Jul 23, 2019 06:57:42 PM UTC<hr/>")
                    .setRecoveryNote("")
                    .setCanBeClosed(false)
                    .setIncludeRecoveryNote(false)
                    .addDetail("OpenTSDB View Details", "https://splunk.opentsdb.net/splunk/en-US/app/search/search?q=search+index%3Dcorona-alerts+alert_id%3D42+earliest%3D07%2F23%2F2019%3A18%3A50%3A00UTC+latest%3D07%2F23%2F2019%3A19%3A05%3A00UTC+timeformat%3D%25m%2F%25d%2F%25Y%3A%25H%3A%25M%3A%25S%25Z")
                    .addDetail("OpenTSDB Modify Alert", "https://opentsdb.net/a/42/view");
            // TODO - Exact comparison again. The copyrights goof it up.
            opsGenieClient.isActive("secret.opsgenie.api.key", withAny(firstAlertBuilder.build()));
            result = Optional.of(Boolean.TRUE);
            opsGenieClient.addNote("secret.opsgenie.api.key", withAny(firstAlertBuilder.build().addRecoveryNote()));
            result = true;

            final OpsGenieAlert secondAlert = OpsGenieAlert.builder()
                    .setNamespace("OpenTSDB")
                    .setMessage("Jenkins Job Result")
                    .setAlias("82b696633575335e116dd5cc31ee84a9f25eccbac57a6ac357c2e00853e51900")
                    .setDescription("Health Check Alert body.<br/><br/><strong>Grouped by</strong>:<br/>host:<strong>host-2</strong>, env:<strong>prod</strong>")
                    .setSource("OpenTSDB-Test")
                    .setTags()
                    .setUser("OpenTSDB-Test-User")
                    .setPriority("P5")
                    .addVisibleToTeams("Test_moog_1")
                    .setGeneralNote("")
                    .setRecoveryNote("<h4>Recovery [1]</h4>\n[1] <strong>test.namespace: test.application</strong> was in the <strong>good</strong> state for at least <strong>9 times.</strong> WARN&rarr;GOOD.<br/>\n<strong>Message</strong>:  Very detailed info<br />\n<strong>Tags</strong>:  env:<strong>prod</strong>,  host:<strong>host-2</strong><br/>\nTue Jul 23, 2019 06:57:42 PM UTC<hr/>")
                    .setCanBeClosed(true)
                    .setIncludeRecoveryNote(true)
                    .addDetail("OpenTSDB View Details", "https://splunk.opentsdb.net/splunk/en-US/app/search/search?q=search+index%3Dcorona-alerts+alert_id%3D42+earliest%3D07%2F23%2F2019%3A18%3A50%3A00UTC+latest%3D07%2F23%2F2019%3A19%3A05%3A00UTC+timeformat%3D%25m%2F%25d%2F%25Y%3A%25H%3A%25M%3A%25S%25Z")
                    .addDetail("OpenTSDB Modify Alert", "https://opentsdb.net/a/42/view")
                    .build();
            // TODO - Exact comparison again. The copyrights goof it up.
            opsGenieClient.isActive("secret.opsgenie.api.key", withAny(secondAlert));
            result = Optional.of(Boolean.TRUE);
            opsGenieClient.close("secret.opsgenie.api.key", withAny(secondAlert));
            result = true;
        }};

        emitter.process(messageKit);
    }

    /**
     * Single message kit should NOT be split into multiple for HealthCheck
     * alerts because meta OpsGenieAutoClose is false.
     */
    @Test
    void processHealthCheckAlert_AlertsSentTogetherWithoutAutoCloseFlag()
    {
        final OpsGenieEmitter emitter = getEmitterWithMockedClient();

        final MessageKit messageKit = MessageKit.builder()
                .setType(Contact.Type.OPSGENIE)
                .setMeta(OpsGenieMeta.builder()
                        .setSubject("Jenkins Job Result")
                        .setBody("Health Check Alert body.")
                        .setOpsGeniePriority("P5")
                        .setOpsGenieAutoClose(false) // No auto-close.
                        .build()
                )
                .setContacts(OpsGenieContact.builder()
                        .setName("Test OpsGenie Contact")
                        .setApiKey("secret.opsgenie.api.key")
                        .build()
                )
                .setAlertGroup(AlertGroup.builder()
                        .setGroupKey(GroupKey.builder()
                                .setAlertId(42)
                                .setAlertType(AlertType.HEALTH_CHECK)
                                .setNamespace("OpenTSDB")
                                .setKeys("env")
                                .setValues("prod")
                                .build()
                        )
                        .setAlerts(Arrays.asList(
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-1")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec())
                                        .setStateFrom(State.GOOD)
                                        .setState(State.BAD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 3))
                                        .setStates(State.BAD, State.WARN, State.GOOD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build(),
                                HealthCheckAlert.builder()
                                        .setId(42)
                                        .setNamespace("OpenTSDB")
                                        .addTag("host", "host-2")
                                        .addTag("env", "prod")
                                        .setDetails("Very detailed info")
                                        .setTimestampSec(TestData.getTimestampSec())
                                        .setStateFrom(State.WARN)
                                        .setState(State.GOOD)
                                        .setTimestampsSec(TestData.getTimestampsSec(60, 4))
                                        .setStates(State.BAD, State.WARN, State.GOOD, State.BAD)
                                        .setApplication("test.application")
                                        .setDataNamespace("test.namespace")
                                        .setThreshold(9)
                                        .setMissingRecovery(false)
                                        .build()
                        ))
                        .build()
                )
                .build();

        new Expectations() {{
            final OpsGenieAlert.Builder firstAlertBuilder = OpsGenieAlert.builder()
                    .setNamespace("OpenTSDB")
                    .setMessage("Jenkins Job Result")
                    .setAlias("fd9a724a6067db8cf11082daf8a52fe6c203862d4251a80d23f5c17093281bc9")
                    .setDescription("Health Check Alert body.<br/><br/><strong>Grouped by</strong>:<br/>env:<strong>prod</strong>")
                    .setSource("OpenTSDB-Test")
                    .setTags()
                    .setUser("OpenTSDB-Test-User")
                    .setPriority("P5")
                    .addVisibleToTeams("Test_moog_1")
                    .setGeneralNote("<h4>Bad [1]</h4>\n[1] <strong>test.namespace: test.application</strong> was in the <strong>bad</strong> state for at least <strong>9 times.</strong> GOOD&rarr;BAD.<br/>\n<strong>Message</strong>:  Very detailed info<br />\n<strong>Tags</strong>:  env:<strong>prod</strong>,  host:<strong>host-1</strong><br/>\nTue Jul 23, 2019 06:57:42 PM UTC<hr/>")
                    .setRecoveryNote("<h4>Recovery [1]</h4>\n[1] <strong>test.namespace: test.application</strong> was in the <strong>good</strong> state for at least <strong>9 times.</strong> WARN&rarr;GOOD.<br/>\n<strong>Message</strong>:  Very detailed info<br />\n<strong>Tags</strong>:  env:<strong>prod</strong>,  host:<strong>host-2</strong><br/>\nTue Jul 23, 2019 06:57:42 PM UTC<hr/>")
                    .setCanBeClosed(false)
                    .setIncludeRecoveryNote(false)
                    .addDetail("OpenTSDB View Details", "https://splunk.opentsdb.net/splunk/en-US/app/search/search?q=search+index%3Dcorona-alerts+alert_id%3D42+earliest%3D07%2F23%2F2019%3A18%3A50%3A00UTC+latest%3D07%2F23%2F2019%3A19%3A05%3A00UTC+timeformat%3D%25m%2F%25d%2F%25Y%3A%25H%3A%25M%3A%25S%25Z")
                    .addDetail("OpenTSDB Modify Alert", "https://opentsdb.net/a/42/view");
            // TODO - Exact comparison again. The copyrights goof it up.
            opsGenieClient.isActive("secret.opsgenie.api.key", withAny(firstAlertBuilder.build()));
            times = 1;
            result = Optional.of(Boolean.TRUE);
            opsGenieClient.addNote("secret.opsgenie.api.key", withAny(firstAlertBuilder.build().addRecoveryNote()));
            times = 1;
            result = true;
        }};

        emitter.process(messageKit);
    }

    @Test
    void processEventAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.OPSGENIE,
                        AlertType.EVENT
                )
        );
    }

    @Test
    void processPeriodOverPeriodAlert()
    {
        getEmitter().process(
                TestData.getMessageKit(
                        Contact.Type.OPSGENIE,
                        AlertType.PERIOD_OVER_PERIOD
                )
        );
    }
}
