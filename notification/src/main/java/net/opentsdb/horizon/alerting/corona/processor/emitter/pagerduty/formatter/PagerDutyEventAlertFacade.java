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
import net.opentsdb.horizon.alerting.corona.model.alert.impl.EventAlert;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.PagerDutyContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.PagerDutyMeta;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventMessageKitView;

public class PagerDutyEventAlertFacade
    extends PagerDutyAbstractAlertFacade<
            EventAlert,
            EventAlertView,
            EventMessageKitView
            >
{
    /* ------------ Constants ------------ */

    /* ------------ Fields ------------ */

    /* ------------ Constructor ------------ */

    public PagerDutyEventAlertFacade(
            final EventMessageKitView messageKitView,
            final EventAlertView alertView,
            final PagerDutyMeta meta,
            final PagerDutyContact contact)
    {
        super(messageKitView, alertView, meta, contact);
    }

    /* ------------ Methods ------------ */

    @Override
    protected String getAlertSpecificSource() {
        return alertView.getDataNamespace() + ": '"
                + alertView.getFilterQuery() + "'";
    }

    @Override
    protected String getAlertType() {
        return AlertType.EVENT.name();
    }

}
