/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.snooze;

import net.opentsdb.horizon.alerts.CacheMark;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.SnoozeFetcher;
import net.opentsdb.horizon.alerts.config.SnoozeFetcherFactory;
import net.opentsdb.horizon.alerts.model.MonitorEvent;
import net.opentsdb.horizon.alerts.model.Snooze;
import net.opentsdb.horizon.alerts.query.tsdb.TsdbV3QueryBuilder;
import lombok.extern.slf4j.Slf4j;

import net.opentsdb.query.filter.QueryFilter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class SnoozeFilter {

    private final SnoozeFetcher fetcher;

    private List<Snooze> snoozeConfig;

    private CacheMark mark = new CacheMark(60);

    public SnoozeFilter() {
        this(SnoozeFetcherFactory.getSnoozeFetcher());
    }

    public SnoozeFilter(SnoozeFetcher snoozeFetcher) {
        this.fetcher = snoozeFetcher;
    }


    public boolean snooze(MonitorEvent e, AlertConfig alertConfig) {

        final String namespace = e.getNamespace();

        if(Objects.isNull(snoozeConfig) || mark.expired()) {
            snoozeConfig = fetcher.getSnoozeConfig().values()
                    .stream()
                    .filter(entry -> namespace.equalsIgnoreCase(entry.getNamespace()))
                    .collect(Collectors.toList());
            log.debug("id: {} Fetched snooze config {}",alertConfig.getAlertId(),snoozeConfig);
            mark.fetched();
        }

        if(Objects.isNull(snoozeConfig)
                || snoozeConfig.isEmpty()) {
            return false;
        }
        log.info("id: {} event received in snooze filter: {} {}",alertConfig.getAlertId(),e,snoozeConfig);
        return doSnooze(e, alertConfig, System.currentTimeMillis());
    }

    private boolean doSnooze(final MonitorEvent event, final AlertConfig alertConfig,
                           final long currentTimeMillis) {

        return snoozeConfig.stream()
                .anyMatch(snooze -> {

                    if(currentTimeMillis
                            < (snooze.getStartTime())) {
                        log.debug("id: {} not time to apply snooze: {} {}",
                                event,
                                currentTimeMillis,
                                snooze.getStartTime());
                        return false;
                    }

                    if(currentTimeMillis >=  snooze.getEndTime()) {
                        log.debug("id: {} snooze expired: {} {}",
                                event,
                                currentTimeMillis,
                                snooze.getEndTime());
                        return false;
                    }

                    final List<Long> alertIds = snooze.getAlertIds();
                    final boolean alertListValid = isValid(alertIds);
                    final boolean snoozeFromAlertList =  alertListValid && alertIds.contains(alertConfig.getAlertId());

                    final List<String> labels = snooze.getLabels();
                    final boolean labelListValid = isValid(labels);
                    final boolean snoozeFromLabelList = labelListValid &&
                            isValid(alertConfig.getLabels()) &&
                            labels.stream().anyMatch(label -> alertConfig.getLabels().contains(label));

                    boolean toBesnoozed = false;
                    boolean alertAndLabelInvalid = false;
                    if((alertListValid && snoozeFromAlertList ||
                            labelListValid && snoozeFromLabelList) ) {


                        toBesnoozed = true;
                    }

                    if(!alertListValid && !labelListValid) {
                        alertAndLabelInvalid = true;
                        toBesnoozed = true;
                    }


                    final QueryFilter queryFilter = snooze.getFilter();
                    boolean queryFilterValid = Objects.nonNull(queryFilter);
                    boolean querySnooze = false;
                    try {
                        querySnooze = TsdbV3QueryBuilder.matches(queryFilter,
                                event.getTags());
                    } catch (Throwable t) {
                        log.error("Query snooze failed for namespace: {} snooze: {} {} message:",
                                alertConfig.getNamespace(), snooze.getId(), t.getMessage());
                        queryFilterValid = false;
                    }

                    if(alertAndLabelInvalid  && !queryFilterValid) {
                        //No valid config, should not happen
                        return false;
                    }


                    final boolean b3 = queryFilterValid && querySnooze;

                    return (b3 || !queryFilterValid) && toBesnoozed;
                });
    }

    private static boolean isValid(List list) {
        return Objects.nonNull(list) && !list.isEmpty();
    }
}
