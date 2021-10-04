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

package net.opentsdb.horizon.alerting.corona.processor.debug;

import java.util.List;
import java.util.Objects;

import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;

import it.unimi.dsi.fastutil.longs.Long2BooleanMap;

public class SyntheticMessageKitCounter
        extends ChainableProcessor<MessageKit, MessageKit> {

    public static SyntheticMessageKitCounter create(
            final Processor<MessageKit> next,
            final List<String> syntheticAlertIDs) {
        return new SyntheticMessageKitCounter(
                next,
                syntheticAlertIDs,
                AppMonitor.get()
        );
    }

    private final AppMonitor appMonitor;
    private final Long2BooleanMap syntheticAlertIDs;

    protected SyntheticMessageKitCounter(final Processor<MessageKit> next,
                                         final List<String> syntheticAlertIDs,
                                         final AppMonitor appMonitor) {
        super(next);
        Objects.requireNonNull(syntheticAlertIDs, "syntheticAlertIDs cannot be null");
        Objects.requireNonNull(appMonitor, "appMonitor cannot be null");
        this.syntheticAlertIDs =
                SyntheticAlertCounter.buildSyntheticAlertIDsMap(syntheticAlertIDs);
        this.appMonitor = appMonitor;
    }

    @Override
    public void process(MessageKit item) {
        final long alertId = item.getAlertId();
        if (syntheticAlertIDs.containsKey(alertId)) {
            appMonitor.countSyntheticAlertReceived(
                    item.getAlertGroup().getAlerts().size(),
                    alertId
            );
        }
        submit(item);
    }
}
