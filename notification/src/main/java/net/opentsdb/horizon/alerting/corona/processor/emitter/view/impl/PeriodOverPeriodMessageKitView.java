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

package net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl;

import net.opentsdb.horizon.alerting.corona.model.alert.impl.PeriodOverPeriodAlert;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;

public class PeriodOverPeriodMessageKitView
        extends MessageKitView<PeriodOverPeriodAlert, PeriodOverPeriodAlertView> {

    public PeriodOverPeriodMessageKitView(final MessageKit messageKit) {
        super(messageKit);
    }

    @Override
    protected PeriodOverPeriodAlertView getViewOf(final PeriodOverPeriodAlert alert) {
        return Views.of(alert);
    }

    @Override
    public String interpolateSubject(PeriodOverPeriodAlertView alertView) {
        return super.interpolateSubject(alertView.getSortedTags());
    }

    @Override
    public String interpolateBody(PeriodOverPeriodAlertView alertView) {
        return super.interpolateBody(alertView.getSortedTags());
    }
}
