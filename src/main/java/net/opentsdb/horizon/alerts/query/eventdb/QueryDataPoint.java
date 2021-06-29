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

package net.opentsdb.horizon.alerts.query.eventdb;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerts.model.Event;

import com.fasterxml.jackson.databind.JsonNode;

class QueryDataPoint {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(QueryDataPoint.class);

    /* ------------ Fields ------------ */

    private JsonNode dataNode;

    /* ------------ Methods ------------ */

    /**
     * This method is exposed only to allow reuse of an instance for
     * multiple data nodes.
     *
     * @param dataNode new data node object.
     */
    void setDataNode(final JsonNode dataNode) {
        this.dataNode = dataNode;
    }

    /**
     * @return number of hits (events).
     */
    int getHits() {
        return dataNode.get("hits").asInt();
    }

    /**
     * @return sorted map of tags.
     */
    TreeMap<String, String> getTags() {
        return new TreeMap<>(parseGroupTags(dataNode));
    }

    private Map<String, String> parseGroupTags(final JsonNode dataNode) {
        if (dataNode.hasNonNull("EventsGroupType")) {
            final JsonNode groupTypeNode = dataNode.get("EventsGroupType");
            final Map<String, String> tags =
                    parseStringsMap(groupTypeNode.get("group"));
            replaceNoGroupTags(tags);
            return tags;
        }
        return Collections.emptyMap();
    }

    private Map<String, String> parseStringsMap(final JsonNode node) {
        final Map<String, String> tags = new HashMap<>();
        node.fields().forEachRemaining(
                e -> tags.put(e.getKey(), e.getValue().asText())
        );
        if (tags.isEmpty()) {
            return Collections.emptyMap();
        }
        return tags;
    }

    private void replaceNoGroupTags(final Map<String, String> tags) {
        tags.replaceAll(
                (key, val) -> "_NO_GROUP".equalsIgnoreCase(val) ? "n/a" : val
        );
    }

    /**
     * @return optional of an {@link Event}; the optional is empty in case
     * the event could not be parsed.
     */
    Optional<Event> getEvent() {
        return parseEvent(dataNode);
    }

    private Optional<Event> parseEvent(final JsonNode dataNode) {
        try {
            final JsonNode eventNode;
            final Map<String, String> eventTags;
            if (dataNode.hasNonNull("EventsGroupType")) {
                final JsonNode groupTypeNode = dataNode.get("EventsGroupType");
                eventNode = groupTypeNode.get("event");
                eventTags = parseStringsMap(eventNode.get("tags"));
            } else if (dataNode.hasNonNull("EventsType")) {
                eventNode = dataNode.get("EventsType");
                eventTags = parseStringsMap(dataNode.get("tags"));
            } else {
                return Optional.empty();
            }

            final Event event = deserializeEvent(eventNode);
            event.setTags(eventTags);

            return Optional.of(event);
        } catch (Exception e) {
            LOG.error("Failed to deserialize event. Reason={}", e.getMessage());
            return Optional.empty();
        }
    }

    private Event deserializeEvent(final JsonNode eventNode) {
        return new Event() {{
            setNamespace(eventNode.get("namespace").asText());
            setSource(eventNode.get("source").asText());
            setTitle(eventNode.get("title").asText());
            setMessage(eventNode.get("message").asText());
            setPriority(eventNode.get("priority").asText());
            setTimestamp(eventNode.get("timestamp").asLong());
        }};
    }
}
