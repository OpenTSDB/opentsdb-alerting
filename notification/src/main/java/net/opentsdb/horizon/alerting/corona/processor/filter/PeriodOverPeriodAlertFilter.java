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

package net.opentsdb.horizon.alerting.corona.processor.filter;

import net.opentsdb.horizon.alerting.corona.component.Triple;
import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;
import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;

public class PeriodOverPeriodAlertFilter extends ChainableProcessor<
        Triple<AlertGroup, Metadata, Contacts>,
        Triple<AlertGroup, Metadata, Contacts>
        > {

    public PeriodOverPeriodAlertFilter(
            Processor<Triple<AlertGroup, Metadata, Contacts>> next) {
        super(next);
    }

    @Override
    public void process(Triple<AlertGroup, Metadata, Contacts> triple) {
        final AlertGroup alertGroup = triple.getFirst();
        if (alertGroup.getGroupKey().getAlertType() != AlertType.PERIOD_OVER_PERIOD) {
            submit(triple);
        }
    }
}
