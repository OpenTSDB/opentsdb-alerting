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

package net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.formatter;

import net.opentsdb.horizon.alerting.corona.model.alert.impl.PeriodOverPeriodAlert;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OcMeta;
import net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.OcRestEvent;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodMessageKitView;

public class PeriodOverPeriodAlertRestEventFacade
        extends AbstractRestEventFacade<
        PeriodOverPeriodAlert,
        PeriodOverPeriodAlertView,
        PeriodOverPeriodMessageKitView> {

    public PeriodOverPeriodAlertRestEventFacade(
            final String hostname,
            final PeriodOverPeriodMessageKitView messageKitView,
            final PeriodOverPeriodAlertView alertView,
            final OcMeta meta,
            final OcContact contact) {
        super(hostname, messageKitView, alertView, meta, contact);
    }

    private static final OcRestEvent.AlertDetails ALERT_DETAILS =
            new OcRestEvent.AlertDetails() {
                @Override
                public String getType() {
                    return "metric";
                }

                @Override
                public String getSubtype() {
                    return "periodOverPeriod";
                }
            };

    @Override
    protected String getAlertSpecificSource() {
        return alertView.getNamespace() + ": " + alertView.getMetric();
    }

    @Override
    public String getDescription() {
        return alertView.getDescription();
    }

    @Override
    public AlertDetails getAlertDetails() {
        return ALERT_DETAILS;
    }
}
