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

package net.opentsdb.horizon.alerting.corona.processor.emitter.splunk;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.EventAlert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.HealthCheckAlert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.PeriodOverPeriodAlert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricAlert;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.view.SplunkAbstractView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.view.SplunkHealthCheckAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.view.SplunkPeriodOverPeriodAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.view.SplunkSingleMetricAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.view.SplunkViewSharedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.component.Triple;
import net.opentsdb.horizon.alerting.corona.processor.Processor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.view.SplunkEventAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SplunkJson implements Processor<Triple<AlertGroup, Metadata, Contacts>> {

    /* ------------ Constants ------------ */

    private static final Logger LOG = LoggerFactory.getLogger(SplunkJson.class);
    private static final Logger SPLUNK_LOG = LoggerFactory.getLogger("corona.splunk.alerts");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /* ------------ Constructors ------------ */

    public SplunkJson() {
    }

    /* ------------ Methods ------------ */

    @Override
    public void process(final Triple<AlertGroup, Metadata, Contacts> triple) {
        try {
            doProcess(triple);
        } catch (Throwable t) {
            LOG.error("Failed to write to Splunk log: triple={}", triple, t);
        }
    }

    private void doProcess(final Triple<AlertGroup, Metadata, Contacts> triple) {
        final AlertGroup alertGroup = triple.getFirst();
        final GroupKey groupKey = alertGroup.getGroupKey();
        final String namespace = alertGroup.getGroupKey().getNamespace();
        final Metadata metadata = triple.getSecond();
        final Contacts contacts = triple.getLast();

        final SplunkViewSharedData sharedData =
                getSharedData(alertGroup, metadata, contacts);

        final AlertType alertType = groupKey.getAlertType();
        for (Alert alert : alertGroup.getAlerts()) {
            final SplunkAbstractView splunkView;
            switch (alertType) {
                case SINGLE_METRIC:
                    splunkView =
                            new SplunkSingleMetricAlertView(
                                    sharedData,
                                    Views.of((SingleMetricAlert) alert)
                            );
                    break;
                case HEALTH_CHECK:
                    splunkView =
                            new SplunkHealthCheckAlertView(
                                    sharedData,
                                    Views.of((HealthCheckAlert) alert)
                            );
                    break;
                case EVENT:
                    splunkView =
                            new SplunkEventAlertView(
                                    sharedData,
                                    Views.of((EventAlert) alert)
                            );
                    break;
                case PERIOD_OVER_PERIOD:
                    splunkView =
                            new SplunkPeriodOverPeriodAlertView(
                                    sharedData,
                                    Views.of((PeriodOverPeriodAlert) alert)
                            );
                    break;
                default:
                    LOG.error("Unexpected alert type: alert_type={}", alertType);
                    continue;
            }

            log(splunkView, namespace);
        }
    }

    private SplunkViewSharedData getSharedData(
            final AlertGroup alertGroup,
            final Metadata metadata,
            final Contacts contacts) {
        final GroupKey groupKey = alertGroup.getGroupKey();

        final List<String> contactNames = contacts.stream()
                .filter(Objects::nonNull)
                .map(Contact::getName)
                .collect(Collectors.toList());

        return new SplunkViewSharedData(
                groupKey.getAlertId(),
                groupKey.getNamespace(),
                groupKey.getAlertType(),
                contactNames,
                metadata.getSubject(),
                metadata.getBody(),
                Views.get().alertViewUrl(groupKey.getAlertId())
        );
    }

    private void log(final SplunkAbstractView view,
                     final String namespace) {
        final String logLine;
        try {
            logLine = JSON_MAPPER.writeValueAsString(view);
        } catch (Exception e) {
            LOG.error("Failed JSON conversion: view={}", view, e);
            AppMonitor.get().countAlertFormatFailed(namespace);
            return;
        }

        try {
            SPLUNK_LOG.info(logLine);
            AppMonitor.get().countAlertSendSuccess(namespace);
        } catch (Exception e) {
            AppMonitor.get().countAlertSendFailed(namespace);
        }
    }
}
