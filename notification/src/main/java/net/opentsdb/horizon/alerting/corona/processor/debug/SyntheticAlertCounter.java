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
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;

import com.google.common.primitives.Longs;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;

/**
 * SyntheticAlertCounter counts number of alerts with id in the synthetic
 * alert ids list.
 *
 * @param <A> alert
 */
public class SyntheticAlertCounter<A extends Alert> implements Processor<A> {

    public static <A extends Alert> Processor<A> create(
            final List<String> syntheticAlertIDs) {
        if (syntheticAlertIDs == null || syntheticAlertIDs.isEmpty()) {
            return new NopCounter<>();
        }
        return new SyntheticAlertCounter<>(syntheticAlertIDs, AppMonitor.get());
    }

    private final AppMonitor appMonitor;
    private final Long2BooleanMap syntheticAlertIDs;

    protected SyntheticAlertCounter(final List<String> syntheticAlertIDs,
                                    final AppMonitor appMonitor) {
        Objects.requireNonNull(syntheticAlertIDs, "syntheticAlertIDs cannot be null");
        Objects.requireNonNull(appMonitor, "appMonitor cannot be null");
        this.syntheticAlertIDs = buildSyntheticAlertIDsMap(syntheticAlertIDs);
        this.appMonitor = appMonitor;
    }

    static Long2BooleanMap buildSyntheticAlertIDsMap(
            final List<String> syntethicAlertIDs) {
        final Map<Long, Boolean> ids = syntethicAlertIDs.stream()
                .filter(Objects::nonNull)
                .map(Longs::tryParse)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Function.identity(), e -> Boolean.TRUE));
        return new Long2BooleanOpenHashMap(ids);
    }

    @Override
    public void process(A alert) {
        final long alertId = alert.getId();
        if (syntheticAlertIDs.containsKey(alertId)) {
            appMonitor.countSyntheticAlertReceived(1, alertId);
        }
    }

    private static final class NopCounter<A extends Alert> implements Processor<A> {
        @Override
        public void process(A item) {
            // Do nothing
        }
    }
}