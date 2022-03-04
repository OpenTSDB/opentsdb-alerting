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

package net.opentsdb.horizon.alerting.corona;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alert.Comparator;
import net.opentsdb.horizon.alerting.corona.model.alert.Event;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alert.ThresholdUnit;
import net.opentsdb.horizon.alerting.corona.model.alert.WindowSampler;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.EventAlert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.HealthCheckAlert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.PeriodOverPeriodAlert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricSimpleAlert;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact.Type;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.EmailContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OpsGenieContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.PagerDutyContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.SlackContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.WebhookContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.EmailMeta;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.Meta;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OcMeta;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OpsGenieMeta;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.PagerDutyMeta;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.SlackMeta;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.WebhookMeta;
import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TestData {

    public static final long ALERT_ID = 54321L;

    public static final String NAMESPACE = "JAVA";

    public static Metadata getMetadata()
    {
        return Metadata.builder()
                .setSubject("Metadata Subject: {{host}}")
                .setBody("Metadata Body: {{host}}")
                .setLabels("test", "dummy")
                .setOcSeverity(OcSeverity.SEV_4)
                .setOpsGeniePriority("P4")
                .setRunbookId("RB007")
                .build();
    }

    public static Contacts getContacts()
    {
        return Contacts.builder()
                .setEmailContacts(
                        EmailContact.builder()
                                .setName("bob@opentsdb.net")
                                .setEmail("bob@opentsdb.net")
                                .build(),
                        EmailContact.builder()
                                .setName("test@opentsdb.net")
                                .setEmail("test@opentsdb.net")
                                .build()
                )
                .setSlackContacts(
                        SlackContact.builder()
                                .setName("Test Slack Contact")
                                .setEndpoint("http://slack.edpoint.url")
                                .build()
                )
                .setOpsGenieContacts(
                        OpsGenieContact.builder()
                                .setName("Test OpsGenie Contact")
                                .setApiKey("secret.opsgenie.api.key")
                                .build()
                )
                .setOcContacts(
                        OcContact.builder()
                                .setName("Test OC Contact")
                                .setDisplayCount("1")
                                .setContext("analysis")
                                .setOpsdbProperty("NS")
                                .build()
                )
                .setWebhookContacts(
                        WebhookContact.builder()
                                .setName("Test HTTP Contact")
                                .setEndpoint("http://endpoint.url")
                                .build()
                )
                .setPagerDutyContacts(
                        PagerDutyContact.builder()
                                .setName("Test PagerDuty Contact")
                                .setRoutingKey("secret.pagerduty.routing.key")
                                .build()
                )
                .build();
    }


    private static long getTimeAnchorMs()
    {
        // ~ 23 Jul 2019 18:58:11 UTC
        return 1563908262735L;
    }

    public static long getTimestampSec()
    {
        return getTimeAnchorMs() / 1000L;
    }

    public static long[] getTimestampsSec(long periodSec, int count)
    {
        long endTs = getTimeAnchorMs() / 1000L;

        final long[] timestamps = new long[count];
        for (int i = count - 1; i >= 0; i--) {
            timestamps[i] = endTs;
            endTs -= periodSec;
        }
        return timestamps;
    }


    public static List<Alert> getSingleMetricAlerts()
    {
        return Arrays.asList(
                SingleMetricSimpleAlert.builder()
                        .setId(ALERT_ID)
                        .setIsSnoozed(true)
                        .setNamespace(NAMESPACE)
                        .addTag("host", "localhost")
                        .addTag("env", "prod")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.GOOD)
                        .setState(State.BAD)
                        .setComparator(Comparator.GREATER_THAN)
                        .setThreshold(9)
                        .setValuesInWindow(9, 59, 89)
                        .setTimestampsSec(getTimestampsSec(60, 3))
                        .setMetric("cpu.utilization.pct")
                        .setSampler(WindowSampler.AT_LEAST_ONCE)
                        .build(),
                SingleMetricSimpleAlert.builder()
                    .setId(ALERT_ID)
                    .setIsSnoozed(true)
                    .setNamespace(NAMESPACE)
                    .addTag("host", "localhost")
                    .addTag("env", "stage")
                    .setDetails("Very detailed info")
                    .setTimestampSec(getTimestampSec())
                    .setStateFrom(State.WARN)
                    .setState(State.BAD)
                    .setComparator(Comparator.GREATER_THAN)
                    .setThreshold(9)
                    .setValuesInWindow(9, 59, 90)
                    .setTimestampsSec(getTimestampsSec(60, 3))
                    .setMetric("cpu.utilization.pct")
                    .setSampler(WindowSampler.AT_LEAST_ONCE)
                    .build(),
                SingleMetricSimpleAlert.builder()
                    .setId(ALERT_ID)
                    .setIsSnoozed(true)
                    .setNamespace(NAMESPACE)
                    .addTag("host", "localhost")
                    .addTag("env", "dev")
                    .setDetails("Very detailed info")
                    .setTimestampSec(getTimestampSec())
                    .setStateFrom(State.GOOD)
                    .setState(State.BAD)
                    .setComparator(Comparator.GREATER_THAN)
                    .setThreshold(9)
                    .setValuesInWindow(9, 59, 91)
                    .setTimestampsSec(getTimestampsSec(60, 3))
                    .setMetric("cpu.utilization.pct")
                    .setSampler(WindowSampler.AT_LEAST_ONCE)
                    .build(),
                SingleMetricSimpleAlert.builder()
                    .setId(ALERT_ID)
                    .setIsSnoozed(true)
                    .setNamespace(NAMESPACE)
                    .addTag("host", "localhost")
                    .addTag("env", "prod-A")
                    .setDetails("Very detailed info")
                    .setTimestampSec(getTimestampSec())
                    .setStateFrom(State.GOOD)
                    .setState(State.BAD)
                    .setComparator(Comparator.GREATER_THAN)
                    .setThreshold(9)
                    .setValuesInWindow(9, 59, 92)
                    .setTimestampsSec(getTimestampsSec(60, 3))
                    .setMetric("cpu.utilization.pct")
                    .setSampler(WindowSampler.AT_LEAST_ONCE)
                    .build(),
                SingleMetricSimpleAlert.builder()
                        .setId(ALERT_ID)
                        .setNamespace(NAMESPACE)
                        .addTag("host", "localhost")
                        .addTag("env", "stage")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.WARN)
                        .setState(State.GOOD)
                        .setComparator(Comparator.LESS_THAN)
                        .setThreshold(67)
                        .setValuesInWindow(7, 8, 7, 9)
                        .setTimestampsSec(getTimestampsSec(60, 4))
                        .setMetric("mem.utilization.pct")
                        .setSampler(WindowSampler.AT_LEAST_ONCE)
                        .build(),
                SingleMetricSimpleAlert.builder()
                        .setId(ALERT_ID)
                        .setNamespace(NAMESPACE)
                        .addTag("host", "localhost")
                        .addTag("env", "stage")
                        .addTag("owner", "skhegay")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.GOOD)
                        .setState(State.WARN)
                        .setComparator(Comparator.GREATER_THAN_OR_EQUALS)
                        .setThreshold(999)
                        .setValuesInWindow(1000, 333, 444, 999)
                        .setTimestampsSec(getTimestampsSec(60, 4))
                        .setMetric("visitors.count")
                        .setSampler(WindowSampler.AT_LEAST_ONCE)
                        .build(),
                SingleMetricSimpleAlert.builder()
                        .setId(ALERT_ID)
                        .setNamespace(NAMESPACE)
                        .addTag("host", "remotehost")
                        .addTag("env", "prod")
                        .addTag("owner", "chiruvol")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.BAD)
                        .setState(State.GOOD)
                        .setComparator(Comparator.GREATER_THAN_OR_EQUALS)
                        .setThreshold(25)
                        .setValuesInWindow(10, 20, 30, 40)
                        .setTimestampsSec(getTimestampsSec(60, 4))
                        .setMetric("visitors.count")
                        .setSampler(WindowSampler.AT_LEAST_ONCE)
                        .build(),
                SingleMetricSimpleAlert.builder()
                        .setId(ALERT_ID)
                        .setNamespace(NAMESPACE)
                        .setIsSnoozed(true)
                        .addTag("host", "remotehost")
                        .addTag("env", "prod")
                        .addTag("owner", "chiruvol")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.BAD)
                        .setState(State.GOOD)
                        .setComparator(Comparator.GREATER_THAN_OR_EQUALS)
                        .setThreshold(25)
                        .setValuesInWindow(10, 20, 30, 40)
                        .setTimestampsSec(getTimestampsSec(60, 4))
                        .setMetric("visitors.count")
                        .setSampler(WindowSampler.AT_LEAST_ONCE)
                        .build(),
                SingleMetricSimpleAlert.builder()
                        .setId(ALERT_ID)
                        .setNamespace(NAMESPACE)
                        .setIsSnoozed(true)
                        .addTag("host", "remotehost")
                        .addTag("env", "prod")
                        .addTag("owner", "zb")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.BAD)
                        .setState(State.GOOD)
                        .setComparator(Comparator.GREATER_THAN_OR_EQUALS)
                        .setThreshold(25)
                        .setValuesInWindow(10, 20, 30, 40)
                        .setTimestampsSec(getTimestampsSec(60, 4))
                        .setMetric("visitors.count")
                        .setSampler(WindowSampler.AT_LEAST_ONCE)
                        .build()
        );
    }

    public static List<Alert> getHealthCheckAlerts()
    {
        return Arrays.asList(
                HealthCheckAlert.builder()
                        .setId(0)
                        .setNamespace("OpenTSDB")
                        .addTag("host", "localhost")
                        .addTag("env", "prod")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.GOOD)
                        .setState(State.BAD)
                        .setTimestampsSec(getTimestampsSec(60, 3))
                        .setStates(State.BAD, State.WARN, State.GOOD)
                        .setApplication("test.application")
                        .setDataNamespace("test.namespace")
                        .setThreshold(9)
                        .setMissingRecovery(false)
                        .build(),
                HealthCheckAlert.builder()
                        .setId(0)
                        .setNamespace("OpenTSDB")
                        .addTag("host", "localhost")
                        .addTag("env", "stage")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.WARN)
                        .setState(State.GOOD)
                        .setTimestampsSec(getTimestampsSec(60, 4))
                        .setStates(State.BAD, State.WARN, State.GOOD, State.BAD)
                        .setApplication("test.application")
                        .setDataNamespace("test.namespace")
                        .setThreshold(9)
                        .setMissingRecovery(false)
                        .build(),
                HealthCheckAlert.builder()
                        .setId(0)
                        .setNamespace("OpenTSDB")
                        .addTag("host", "localhost")
                        .addTag("env", "stage")
                        .addTag("owner", "skhegay")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.GOOD)
                        .setState(State.WARN)
                        .setTimestampsSec(getTimestampsSec(60, 4))
                        .setStates(State.BAD, State.WARN, State.GOOD, State.BAD)
                        .setApplication("test.application")
                        .setDataNamespace("test.namespace")
                        .setThreshold(9)
                        .setMissingRecovery(false)
                        .build(),
                HealthCheckAlert.builder()
                        .setId(0)
                        .setNamespace("OpenTSDB")
                        .addTag("host", "remotehost")
                        .addTag("env", "prod")
                        .addTag("owner", "chiruvol")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.BAD)
                        .setState(State.GOOD)
                        .setTimestampsSec(getTimestampsSec(60, 4))
                        .setStates(State.BAD, State.WARN, State.GOOD, State.BAD)
                        .setApplication("test.application")
                        .setDataNamespace("test.namespace")
                        .setThreshold(9)
                        .setMissingRecovery(false)
                        .build()
        );
    }

    public static List<Alert> getEventAlerts()
    {
        final Event event = new Event();
        event.setNamespace("Eventland");
        event.setTitle("Important matter");
        event.setMessage("Blah, blah, blah");
        event.setSource("docker");
        event.setTags(new HashMap<String, String>() {{
            put("deployment", "good");
        }});
        return Arrays.asList(
                EventAlert.builder()
                        .setId(0)
                        .setNamespace("OpenTSDB")
                        .addTag("host", "remotehost")
                        .addTag("env", "prod")
                        .addTag("owner", "chiruvol")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.BAD)
                        .setState(State.GOOD)
                        .setWindowSizeSec(600)
                        .setDataNamespace("Yahoooo")
                        .setFilterQuery("*:*")
                        .setThreshold(10)
                        .setCount(5)
                        .setEvent(null)
                        .build(),
                EventAlert.builder()
                        .setId(0)
                        .setNamespace("OpenTSDB")
                        .addTag("host", "remotehost")
                        .addTag("owner", "chiruvol")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.GOOD)
                        .setState(State.BAD)
                        .setWindowSizeSec(600)
                        .setDataNamespace("Yahoooo-Good")
                        .setFilterQuery("*:*")
                        .setThreshold(10)
                        .setCount(5)
                        .setEvent(null)
                        .build(),
                EventAlert.builder()
                        .setId(0)
                        .setNamespace("OpenTSDB")
                        .addTag("host", "remotehost")
                        .addTag("owner", "chiruvol")
                        .setDetails("Very detailed info")
                        .setTimestampSec(getTimestampSec())
                        .setStateFrom(State.GOOD)
                        .setState(State.BAD)
                        .setWindowSizeSec(600)
                        .setDataNamespace("Yahoooo-Bad")
                        .setFilterQuery("*:*")
                        .setThreshold(10)
                        .setCount(5)
                        .setEvent(event)
                        .build()
        );
    }

    public static List<Alert> getPeriodOverPeriodAlerts()
    {
        return Arrays.asList(
                PeriodOverPeriodAlert.builder()
                        .setNamespace("OpenTSDB")
                        .addTag("host", "localhost")
                        .addTag("emitter", "oc")
                        .setStateFrom(State.BAD)
                        .setState(State.GOOD)
                        .setTimestampSec(1574760300)
                        .setObservedValue(5.)
                        .setPredictedValue(10.)
                        .setUpperBadValue(Double.NaN)
                        .setUpperWarnValue(Double.NaN)
                        .setLowerBadValue(3)
                        .setLowerWarnValue(3)
                        .setTimestampsSec(1574760000, 1574760060, 1574760120, 1574760180, 1574760240, 1574760300)
                        .setObservedValues(1, 2, 3, 4, 5, 6)
                        .setPredictedValues(2, 3, 6, 3, 6, 4)
                        .setUpperBadValues(null)
                        .setUpperWarnValues(null)
                        .setLowerBadValues(3, 3, 3, 3, 3, 3)
                        .setLowerWarnValues(1, 1, 1, 1, 1, 1)
                        .setUpperBadThreshold(3)
                        .setUpperWarnThreshold(4)
                        .setLowerBadThreshold(3)
                        .setLowerWarnThreshold(2)
                        .setUpperThresholdUnit(ThresholdUnit.PERCENT)
                        .setLowerThresholdUnit(ThresholdUnit.VALUE)
                        .setIsSnoozed(false)
                        .build(),
                PeriodOverPeriodAlert.builder()
                        .setNamespace("OpenTSDB")
                        .addTag("host", "localhost")
                        .addTag("emitter", "oc")
                        .setStateFrom(State.BAD)
                        .setState(State.GOOD)
                        .setTimestampSec(1574760300)
                        .setObservedValue(5.)
                        .setPredictedValue(10.)
                        .setUpperBadValue(40)
                        .setUpperWarnValue(4)
                        .setLowerBadValue(3)
                        .setLowerWarnValue(3)
                        .setTimestampsSec(1574760000, 1574760060, 1574760120, 1574760180, 1574760240, 1574760300)
                        .setObservedValues(1, 2, 3, 4, 5, 6)
                        .setPredictedValues(2, 3, 6, 3, 6, 4)
                        .setUpperBadValues(6, 6, 6, 6, 6, 6)
                        .setUpperWarnValues(5, 5, 5, 5, 5, 5)
                        .setLowerBadValues(3, 3, 3, 3, 3, 3)
                        .setLowerWarnValues(1, 1, 1, 1, 1, 1)
                        .setUpperBadThreshold(3)
                        .setUpperWarnThreshold(4)
                        .setLowerBadThreshold(3)
                        .setLowerWarnThreshold(2)
                        .setUpperThresholdUnit(ThresholdUnit.PERCENT)
                        .setLowerThresholdUnit(ThresholdUnit.VALUE)
                        .setIsSnoozed(false)
                        .build()
        );
    }

    public static List<Alert> getAlerts(final AlertType alertType)
    {
        switch (alertType) {
            case SINGLE_METRIC:
                return getSingleMetricAlerts();
            case HEALTH_CHECK:
                return getHealthCheckAlerts();
            case EVENT:
                return getEventAlerts();
            case PERIOD_OVER_PERIOD:
                return getPeriodOverPeriodAlerts();
        }
        throw new RuntimeException("Unknown alert type: " + alertType);
    }

    public static Meta getMeta(final Type contactType)
    {
        switch (contactType) {
            case SLACK:
                return SlackMeta.builder()
                        .setSubject("A very complicated subject! Host: {{host}}")
                        .setBody("Somewhat sophisticated\nslack body. Host: {{host}}")
                        .build();
            case EMAIL:
                return EmailMeta.builder()
                        .setSubject("Test Email Subject. Host: {{host}}")
                        .setBody("Email Body. Host: {{host}}")
                        .build();
            case OPSGENIE:
                return OpsGenieMeta.builder()
                        .setSubject("CPU High Alert. Host: {{host}}")
                        .setBody("Single Metric Alert. Host: {{host}}")
                        .setOpsGeniePriority("P5")
                        .setOpsGenieTags(Arrays.asList("host-{{host}}"))
                        .build();
            case OC:
                return OcMeta.builder()
                        .setSubject("Subject. Host: {{host}}")
                        .setBody("Body\n with many lines. Host: {{host}}")
                        .setOcSeverity(OcSeverity.SEV_3)
                        .setOcTier(OcTier.TIER_2)
                        .setRunbookId("RB111")
                        .build();
            case WEBHOOK:
                return WebhookMeta.builder()
                        .setSubject("Subject. Host: {{host}}")
                        .setBody("Body\n with many lines. Host: {{host}}")
                        .build();
            case PAGERDUTY:
                return PagerDutyMeta.builder()
                        .setSubject("Subject. Host: {{host}}")
                        .setBody("Body\n with many lines. Host: {{host}}")
                        .build();
        }
        throw new RuntimeException(
                "Unknown contact type: " + contactType.name());
    }

    private static String getSlackEndpoint()
    {
        try {
            final File file = new File("slack.endpoint");
            final FileReader fr = new FileReader(file);
            return new BufferedReader(fr).readLine();
        } catch (IOException e) {
            return "https://IDoNotExist.com";
        }
    }

    private static String getOpsGenieApiKey()
    {
        try {
            final File file = new File("opsgenie.apikey");
            final FileReader fr = new FileReader(file);
            return new BufferedReader(fr).readLine();
        } catch (IOException e) {
            return "abra-cadabra";
        }
    }

    private static String getPagerDutyRoutingKey()
    {
        try {
            final File file = new File("pagerduty.routingkey");
            final FileReader fr = new FileReader(file);
            return new BufferedReader(fr).readLine();
        } catch (IOException e) {
            return "abra-cadabra";
        }
    }

    public static List<Contact> getContacts(final Type contactType)
    {
        switch (contactType) {
            case EMAIL:
                return Arrays.asList(
                        EmailContact.builder()
                                .setName("test@opentsdb.net")
                                .setEmail("test@opentsdb.net")
                                .build()
                );
            case SLACK:
                return Arrays.asList(
                        SlackContact.builder()
                                .setName("Skhegay Test Contact")
                                .setEndpoint(getSlackEndpoint())
                                .build()
                );
            case OPSGENIE:
                return Arrays.asList(
                        OpsGenieContact.builder()
                                .setName("Skhegay Test Contact")
                                .setApiKey(getOpsGenieApiKey())
                                .build()
                );
            case OC:
                return Arrays.asList(
                        OcContact.builder()
                                .setName("bob@opentsdb.net")
                                .setCustomer("customer")
                                .setDisplayCount("1")
                                .setContext("live")
                                .setOpsdbProperty("property")
                                .build()
                );
            case WEBHOOK:
                return Arrays.asList(
                        WebhookContact.builder()
                                .setEndpoint("https://test.endpoint.url")
                                .setName("test http contact")
                                .build()
                );
            case PAGERDUTY:
                return Arrays.asList(
                        PagerDutyContact.builder()
                                .setName("test pagerduty contact")
                                .setRoutingKey(getPagerDutyRoutingKey())
                                .build()
                );
        }
        throw new RuntimeException("Unknown contact type: " + contactType);
    }


    public static AlertGroup getAlertGroup(final AlertType alertType)
    {
        return AlertGroup.builder()
                .setGroupKey(GroupKey.builder()
                        .setAlertId(0)
                        .setAlertType(alertType)
                        .setNamespace("OpenTSDB")
                        .setKeys("host")
                        .setValues("localhost")
                        .build()
                )
                .setAlerts(getAlerts(alertType))
                .build();
    }

    public static MessageKit getMessageKit(final Type contactType,
                                           final AlertType alertType)
    {
        return MessageKit.builder()
                .setType(contactType)
                .setMeta(getMeta(contactType))
                .setContacts(getContacts(contactType))
                .setAlertGroup(getAlertGroup(alertType))
                .build();
    }

    public static String deleteCopyright(String result) {
        if (!result.contains("<!--")) {
            return result;
        }
        int index = result.indexOf("<!--");
        while (index >= 0) {
            if (index == 0) {
                result = result.substring(result.indexOf("-->") + 3);
            } else {
                String temp = result.substring(0, index);
                result = temp + result.substring(result.indexOf("-->", index) + 3);
            }
            index = result.indexOf("<!--");
        }
        return result;
    }
}
