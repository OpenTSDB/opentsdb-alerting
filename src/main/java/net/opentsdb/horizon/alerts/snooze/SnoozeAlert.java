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

import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.model.MonitorEvent;
import net.opentsdb.horizon.alerts.processor.ChainableProcessor;
import net.opentsdb.horizon.alerts.processor.impl.EnrichmentProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class SnoozeAlert extends EnrichmentProcessor {

    private ChainableProcessor<AlertEventBag> nextProcessor;

    private final SnoozeFilter snoozeFilter;

    public SnoozeAlert(SnoozeFilter snoozeFilter) {
        this.snoozeFilter = snoozeFilter;
    }

    @Override
    public boolean process(AlertEventBag e) {

        final AlertConfig alertConfig = e.getAlertConfig();

        e.getAlertEvents()
                .stream()
                .filter(Objects::nonNull)
                .forEach(alert -> {
                    final Map<String, String> tagsClone = new HashMap<>(alert.getTags());
                    AlertUtils.addSystemTags(
                            tagsClone,
                            alertConfig.getAlertId(),
                            alertConfig.getAlertName(),
                            alertConfig.getAlertType().getString()
                    );

                    if (snoozeFilter.snooze(
                            new ClonedEvent(
                                    alert.getNamespace(),
                                    tagsClone),
                            alertConfig)) {
                        alert.setSnoozed(true);
                    }
                });

        return sendToNext(e);

    }

    private boolean sendToNext(AlertEventBag e) {
        if(nextProcessor != null) {
            nextProcessor.process(e);
        }
        return true;
    }


    @Override
    public void setNextProcessor(ChainableProcessor<AlertEventBag> nextProcessor) {

        this.nextProcessor = nextProcessor;

    }

    private static class ClonedEvent implements MonitorEvent {

        final Map<String, String> tags;
        final String namespace;
        final String toString;

        public ClonedEvent(String namespace, Map<String, String> tags) {
             this.namespace = namespace;
             this.tags = tags;
             this.toString = String.format("namespace: %s tags: %s", namespace, tags);
        }

        @Override
        public String getNamespace() {
            return this.namespace;
        }

        @Override
        public Map<String, String> getTags() {
            return Collections.unmodifiableMap(this.tags);
        }
        
        @Override
        public String toString() {
            return this.toString;
        }
    }
}
