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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationEvent {

    private String subject;

    private String body;

    private List<String> groupingRules = new ArrayList<>();

    private List<Recipient> recipients = new ArrayList<>();

    private AlertEvent event;

    public NotificationEvent(AlertEvent event) {
        this.event = event;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = deriveSubject(subject,event);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = deriveBody(body,event);
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

    public AlertEvent getAlertEvent() {
        return event;
    }


    protected static String deriveSubject(final String subject, final AlertEvent event) {
        final Map<String, String> tags = event.getTags();

        String subjectHolder = new String(subject);

        for(String key : tags.keySet()) {
            if(subjectHolder.contains("$"+key)) {
                subjectHolder = subjectHolder.replace("$"+key,tags.get(key));
            }
        }

        return subjectHolder;
    }

    protected static String deriveBody(String body, AlertEvent event) {
        final Map<String, String> tags = event.getTags();

        String bodyHolder = new String(body);

        for(String key : tags.keySet()) {
            if(bodyHolder.contains("$"+key)) {
                bodyHolder = bodyHolder.replace("$"+key,(" the "+key+"  "+tags.get(key)));
            }
        }

        return bodyHolder;
    }


}
