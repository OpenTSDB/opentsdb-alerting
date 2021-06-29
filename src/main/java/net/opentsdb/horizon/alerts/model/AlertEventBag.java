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

package net.opentsdb.horizon.alerts.model;

import net.opentsdb.horizon.alerts.config.AlertConfig;
import lombok.Getter;

import java.util.List;

@Getter
public class AlertEventBag {

    private final List<AlertEvent> alertEvents;
    private final AlertConfig alertConfig;
    private final String namespace;
    private final long id;

    public AlertEventBag(List<AlertEvent> alertEvent, AlertConfig alertConfig) {
        this.alertEvents = alertEvent;
        this.namespace = alertConfig.getNamespace();
        this.alertConfig = alertConfig;
        this.id = alertConfig.getAlertId();
    }

    public String toString() {

        return "Event bag: "+ alertEvents.toString();

    }
}

