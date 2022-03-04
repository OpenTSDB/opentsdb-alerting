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

package net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty.formatter;

import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.HealthCheckAlert;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.PagerDutyContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.PagerDutyMeta;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthCheckAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthCheckMessageKitView;

public class PagerDutyHealthCheckAlertFacade
    extends PagerDutyAbstractAlertFacade<
            HealthCheckAlert,
            HealthCheckAlertView,
            HealthCheckMessageKitView
            >
{
    /* ------------ Constants ------------ */

    /* ------------ Fields ------------ */

    /* ------------ Constructor ------------ */

    public PagerDutyHealthCheckAlertFacade(
            final HealthCheckMessageKitView messageKitView,
            final HealthCheckAlertView alertView,
            final PagerDutyMeta meta,
            final PagerDutyContact contact)
    {
        super(messageKitView, alertView, meta, contact);
    }

    /* ------------ Methods ------------ */

    @Override
    protected String getAlertSpecificSource() {
        return alertView.getDataNamespace() + ": " + alertView.getApplication();
    }

    @Override
    protected String getAlertType() {
        return AlertType.HEALTH_CHECK.name();
    }

}
