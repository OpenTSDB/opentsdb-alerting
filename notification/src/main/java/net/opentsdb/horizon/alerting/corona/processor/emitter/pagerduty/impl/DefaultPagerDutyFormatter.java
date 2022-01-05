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

package net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty.impl;

import net.opentsdb.horizon.alerting.corona.model.contact.impl.PagerDutyContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.PagerDutyMeta;
import net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty.PagerDutyEvent;
import net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty.PagerDutyFormatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty.formatter.PagerDutyEventAlertFacade;
import net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty.formatter.PagerDutyHealthCheckAlertFacade;
import net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty.formatter.PagerDutyPeriodOverPeriodAlertFacade;
import net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty.formatter.PagerDutySingleMetricAlertFacade;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventMessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthCheckAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthCheckMessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodMessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricMessageKitView;

public class DefaultPagerDutyFormatter implements PagerDutyFormatter {

    @Override
    public PagerDutyEvent format(final MessageKitView messageKit,
                                 final AlertView alertView,
                                 final PagerDutyMeta meta,
                                 final PagerDutyContact contact)
    {
        if (alertView instanceof SingleMetricAlertView) {
            return new PagerDutySingleMetricAlertFacade(
                    (SingleMetricMessageKitView) messageKit,
                    (SingleMetricAlertView) alertView,
                    meta,
                    contact
            );
        } else if (alertView instanceof HealthCheckAlertView) {
            return new PagerDutyHealthCheckAlertFacade(
                    (HealthCheckMessageKitView) messageKit,
                    (HealthCheckAlertView) alertView,
                    meta,
                    contact
            );
        } else if (alertView instanceof EventAlertView) {
            return new PagerDutyEventAlertFacade(
                    (EventMessageKitView) messageKit,
                    (EventAlertView) alertView,
                    meta,
                    contact
            );
        } else if (alertView instanceof PeriodOverPeriodAlertView) {
            return new PagerDutyPeriodOverPeriodAlertFacade(
                    (PeriodOverPeriodMessageKitView) messageKit,
                    (PeriodOverPeriodAlertView) alertView,
                    meta,
                    contact
            );
        }

        throw new IllegalArgumentException("Unexpected alert view: class=" +
                alertView.getClass().getCanonicalName());
    }

    public interface Builder extends PagerDutyFormatter.Builder<Builder> {
    }

    private static final class DefaultBuilder implements Builder {
        @Override
        public PagerDutyFormatter build() {
            return new DefaultPagerDutyFormatter();
        }
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }
}
