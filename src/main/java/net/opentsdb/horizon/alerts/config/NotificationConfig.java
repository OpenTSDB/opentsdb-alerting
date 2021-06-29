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

package net.opentsdb.horizon.alerts.config;

import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.NotificationEvent;
import net.opentsdb.horizon.alerts.model.Recipient;

import java.util.ArrayList;
import java.util.List;

public class NotificationConfig {


    private String subject;

    private String body;

    private List<String> groupingRules = new ArrayList<>();

    private List<Recipient> recipients = new ArrayList<>();
    private String namespace;
    private long alertid;
    private String name;

    public NotificationConfig(String namespace, long alertid, String name) {

        this.namespace = namespace;
        this.alertid = alertid;
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public long getAlertid() {
        return alertid;
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }


    public List<String> getGroupingRules() {
        return groupingRules;
    }

    public void setGroupingRules(List<String> groupingRules) {
        this.groupingRules = groupingRules;
    }

    public List<Recipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<Recipient> recipients) {
        this.recipients = recipients;
    }

    public NotificationEvent createNotificationEvent(AlertEvent event) {

        NotificationEvent notificationEvent = new NotificationEvent(event);

        notificationEvent.setBody(body);
        notificationEvent.setSubject(subject);
        notificationEvent.setRecipients(recipients);
        notificationEvent.setGroupingRules(groupingRules);
        return notificationEvent;
    }


}
