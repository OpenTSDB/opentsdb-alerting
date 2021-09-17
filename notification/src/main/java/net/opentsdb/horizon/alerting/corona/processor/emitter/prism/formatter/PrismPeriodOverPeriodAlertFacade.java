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

package net.opentsdb.horizon.alerting.corona.processor.emitter.prism.formatter;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;

import net.opentsdb.horizon.alerting.corona.model.alert.impl.PeriodOverPeriodAlert;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OcContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OcMeta;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodMessageKitView;

public class PrismPeriodOverPeriodAlertFacade
        extends PrismAbstractAlertFacade<
        PeriodOverPeriodAlert,
        PeriodOverPeriodAlertView,
        PeriodOverPeriodMessageKitView
        > {

    private final AlertDetails alertDetails;

    public PrismPeriodOverPeriodAlertFacade(
            final String hostname,
            final PeriodOverPeriodMessageKitView messageKitView,
            final PeriodOverPeriodAlertView alertView,
            final OcMeta meta,
            final OcContact contact) {
        super(hostname, messageKitView, alertView, meta, contact);
        this.alertDetails = new AlertDetails() {
            @Override
            public long getAlertId() {
                return messageKitView.getAlertId();
            }

            @Override
            public String getType() {
                return "period_over_period";
            }

            @Override
            public Map<String, String> getTypeMeta() {
                return Collections.emptyMap();
            }

            @Override
            public String getSubject() {
                return messageKitView.interpolateSubject(alertView);
            }

            @Override
            public String getBody() {
                return messageKitView.interpolateBody(alertView);
            }

            @Override
            public String getDescription() {
                return alertView.getDescription();
            }

            @Override
            public SortedMap<String, String> getTags() {
                return alertView.getSortedTags();
            }

            @Override
            public String getStateFrom() {
                return alertView.getStateFrom();
            }

            @Override
            public String getStateTo() {
                return alertView.getStateTo();
            }

            @Override
            public boolean isNag() {
                return alertView.isNag();
            }

            @Override
            public String getKey() {
                return alertView.getNamespace() + "." + alertView.getMetric();
            }
        };
    }

    @Override
    protected String getAlertSpecificSource() {
        return alertView.getNamespace() + ": " + alertView.getMetric();
    }

    @Override
    public String getDescription() {
        return messageKitView.interpolateSubject(alertView);
    }

    @Override
    public AlertDetails getAlertDetails() {
        return alertDetails;
    }
}
