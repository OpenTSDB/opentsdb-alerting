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

package net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.view;

import java.util.List;

import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;

public final class SplunkViewSharedData {

    final long alertId;
    final String namespace;
    final AlertType alertType;
    final List<String> contacts;
    final String subject;
    final String body;
    final String viewUrl;

    public SplunkViewSharedData(long alertId,
                                String namespace,
                                AlertType alertType,
                                List<String> contacts,
                                String subject,
                                String body,
                                String viewUrl) {
        this.alertId = alertId;
        this.namespace = namespace;
        this.alertType = alertType;
        this.contacts = contacts;
        this.subject = subject;
        this.body = body;
        this.viewUrl = viewUrl;
    }
}
