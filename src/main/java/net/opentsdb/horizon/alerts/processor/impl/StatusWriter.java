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

package net.opentsdb.horizon.alerts.processor.impl;

import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.OutputWriter;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.http.CollectorWriters;
import net.opentsdb.horizon.alerts.kafka.KafkaWriter;
import net.opentsdb.horizon.alerts.model.tsdb.YmsStatusEvent;
import net.opentsdb.horizon.alerts.processor.ChainableProcessor;
import net.opentsdb.horizon.alerts.snooze.SnoozeFilter;

import static net.opentsdb.horizon.alerts.AlertUtils.SINK_KAFKA;

public class StatusWriter implements ChainableProcessor<YmsStatusEvent> {

    private ChainableProcessor<YmsStatusEvent> nextProcessor;

    private final OutputWriter outputWriter;

    private final SnoozeFilter snoozeFilter;

    private final AlertConfig alertConfig;

    private final EnvironmentConfig environmentConfig = new EnvironmentConfig();

    public static final String STATUS_SNOOZED_TAG = "opentsdb:snoozed";

    public StatusWriter(AlertConfig alertConfig, SnoozeFilter snoozeFilter) {
        if(environmentConfig.getStatusWriteSink().equals(SINK_KAFKA)) {
            this.outputWriter = new KafkaWriter();
        } else {
            this.outputWriter = CollectorWriters.getStatusCollectorWriter();
        }

        this.snoozeFilter = snoozeFilter;
        this.alertConfig = alertConfig;
    }

    public StatusWriter(AlertConfig alertConfig, SnoozeFilter snoozeFilter, OutputWriter outputWriter) {
        this.snoozeFilter = snoozeFilter;
        this.alertConfig = alertConfig;
        this.outputWriter = outputWriter;
    }


    @Override
    public boolean process(YmsStatusEvent e) {
        if(environmentConfig.isSnoozeTagsEnabled()) {
            if (snoozeFilter.snooze(e, alertConfig)) {

                e.getTags().put(STATUS_SNOOZED_TAG, String.valueOf(Boolean.TRUE));

            } else {

                e.getTags().put(STATUS_SNOOZED_TAG, String.valueOf(Boolean.FALSE));

            }
        }
        outputWriter.sendStatusEvent(e);
        return true;
    }

    @Override
    public void setNextProcessor(ChainableProcessor<YmsStatusEvent> nextProcessor) {
        this.nextProcessor = nextProcessor;
    }
}
