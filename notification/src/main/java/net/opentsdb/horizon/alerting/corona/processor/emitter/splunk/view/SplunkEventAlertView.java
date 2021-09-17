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

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import net.opentsdb.horizon.alerting.corona.model.alert.Event;
import net.opentsdb.horizon.alerting.corona.processor.emitter.Interpolator;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventAlertView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = SplunkEventAlertView.class)
@JsonPropertyOrder({
        "_logged_at",
        "ts", "namespace", "alert_id", "state_from", "state_to", "description",
        "url", "snoozed", "nag", "tags", "contacts", "subject", "body", "alert_type",
        "event_namespace", "event_count", "last_event"
})
public final class SplunkEventAlertView extends SplunkAbstractView {

    private final EventAlertView innerView;

    public SplunkEventAlertView(SplunkViewSharedData sharedData, EventAlertView innerView) {
        super(sharedData, innerView);
        this.innerView = innerView;
    }

    @Override
    String getSubject() {
        return Interpolator.interpolate(sharedData.subject, innerView.getSortedTags());
    }

    @Override
    String getBody() {
        return Interpolator.interpolate(sharedData.body, innerView.getSortedTags());
    }

    @JsonProperty("event_namespace")
    public String getEventNamespace() {
        return innerView.getDataNamespace();
    }

    @JsonProperty("event_count")
    public int getEventCount() {
        return innerView.getCount();
    }

    @JsonProperty("last_event")
    public EventView getLastEvent() {
        final Event event = innerView.getEvent();
        if (innerView.isRecovery() || Objects.isNull(event)) {
            return null;
        }
        return new EventView(event);
    }

    @JsonPropertyOrder({"ts", "source", "title", "message", "tags"})
    static final class EventView {

        protected static final SimpleDateFormat DATE_FORMAT =
                new SimpleDateFormat("EEE MMM d, yyyy hh:mm:ss a z");

        static {
            DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        private final Event event;

        public EventView(Event event) {
            Objects.requireNonNull(event, "event cannot be null");
            this.event = event;
        }

        @JsonProperty("ts")
        String getHumanReadableTs() {
            // Timestamp is in seconds, we need milliseconds.
            return DATE_FORMAT.format(new Date(event.getTimestamp() * 1_000L));
        }

        @JsonProperty("source")
        String getSource() {
            return event.getSource();
        }

        @JsonProperty("title")
        String getTitle() {
            return event.getTitle();
        }

        @JsonProperty("message")
        String getMessage() {
            return event.getMessage();
        }

        @JsonProperty("tags")
        SortedMap<String, String> getTags() {
            final Map<String, String> tags = event.getTags();
            if (tags == null || tags.size() == 0) {
                return Collections.emptySortedMap();
            }
            return new TreeMap<>(tags);
        }
    }
}
