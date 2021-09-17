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
import java.util.Objects;
import java.util.SortedMap;

import net.opentsdb.horizon.alerting.corona.model.alert.AlertType;
import net.opentsdb.horizon.alerting.corona.processor.emitter.splunk.LogTime;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = SplunkAbstractView.class)
public abstract class SplunkAbstractView {

    protected final SplunkViewSharedData sharedData;
    private final AlertView innerView;

    public SplunkAbstractView(SplunkViewSharedData sharedData, AlertView innerView) {
        Objects.requireNonNull(sharedData, "sharedData cannot be null");
        Objects.requireNonNull(innerView, "innerView cannot be null");
        this.sharedData = sharedData;
        this.innerView = innerView;
    }

    @JsonProperty("subject")
    abstract String getSubject();

    @JsonProperty("body")
    abstract String getBody();

    @JsonProperty("alert_id")
    long getAlertId() {
        return sharedData.alertId;
    }

    @JsonProperty("namespace")
    public String getNamespace() {
        return sharedData.namespace;
    }

    @JsonProperty("alert_type")
    AlertType getAlertType() {
        return sharedData.alertType;
    }

    @JsonProperty("contacts")
    List<String> getContacts() {
        return sharedData.contacts;
    }

    @JsonProperty("url")
    String getURL() {
        return sharedData.viewUrl;
    }

    @JsonProperty("ts")
    String getHumanTimestamp() {
        return innerView.getHumanTimestamp();
    }

    @JsonProperty("snoozed")
    boolean isSnoozed() {
        return innerView.isSnoozed();
    }

    @JsonProperty("nag")
    boolean isNag() {
        return innerView.isNag();
    }

    @JsonProperty("description")
    String getDescription() {
        return innerView.getDescription();
    }

    @JsonProperty("state_to")
    String getStateTo() {
        return innerView.getStateTo();
    }

    @JsonProperty("state_from")
    String getStateFrom() {
        return innerView.getStateFrom();
    }

    @JsonProperty("tags")
    SortedMap<String, String> getTags() {
        return innerView.getSortedTags();
    }

    @JsonProperty("_logged_at")
    String getLoggedAtTime() {
        return LogTime.get();
    }
}
