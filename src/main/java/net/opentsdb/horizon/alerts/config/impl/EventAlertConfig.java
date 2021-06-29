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

package net.opentsdb.horizon.alerts.config.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;

import lombok.Getter;

import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.enums.AlertType;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.processor.impl.UpdatableExecutorWrapper;
import net.opentsdb.horizon.alerts.query.eventdb.EventProcessor;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import net.opentsdb.horizon.alerts.state.AlertStateStores;

import com.fasterxml.jackson.databind.JsonNode;

@Getter
public class EventAlertConfig extends AlertConfig {

    private static final String GET_ALL_FILTER = "*:*";

    private int slidingWindowSec = 300;

    private int threshold;

    public EventAlertConfig(final String namespace,
                            final AlertType alertType,
                            final long alertId,
                            final long lastModified) {
        super(namespace, alertType, alertId, lastModified);
    }


    public String getQueryNamespace() {
        return getQueryJson().get("namespace").asText().trim();
    }

    public String getQueryFilter() {
        String query = getQueryJson().get("filter").asText();
        if (query == null) {
            return GET_ALL_FILTER;
        }

        query = query.trim();
        if (query.isEmpty()) {
            return GET_ALL_FILTER;
        }

        return query;
    }

    public List<String> getQueryGroupBy() {
        final JsonNode groupByNode = getQueryJson().withArray("groupBy");
        if (groupByNode == null || !groupByNode.elements().hasNext()) {
            return Collections.emptyList();
        }

        final List<String> groupBy = new ArrayList<>();
        final Iterator<JsonNode> it = groupByNode.elements();
        while (it.hasNext()) {
            final String element = it.next().asText().trim();
            if (element.isEmpty()) {
                continue;
            }
            groupBy.add(element);
        }

        return groupBy;
    }

    @Override
    public void parseAlertSpecific(JsonNode conf) {
        final JsonNode eventAlert = conf.get("threshold").get("eventAlert");
        threshold = eventAlert.get("threshold").asInt();
        slidingWindowSec = eventAlert.get("slidingWindow").asInt();
    }

    @Override
    public UpdatableExecutorWrapper<EventAlertConfig> createAlertExecutor() {
        return new UpdatableExecutorWrapper<>(this);
    }

    @Override
    protected String getDefaultQueryType() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean validateConfig() {
        return slidingWindowSec > 0L && threshold >= 0L;
    }

    @Override
    public AlertEvent createAlertEvent(final long hash,
                                       final String tsField,
                                       final SortedMap<String, String> tags,
                                       final AlertState alertType) {
        return null;
    }

    /**
     * Overriding this method is a hack to enable storing alert state
     * in the implementation of {@link EventProcessor}.
     *
     * TODO: Revisit state management for persistence use case.
     *
     * @return alert state store with missing enabled.
     */
    @Override
    public AlertStateStore createAlertStateStore() {
        return AlertStateStores.withTransitionsAndMissing(
                String.valueOf(getAlertId()),
                getNagIntervalInSecs(),
                getTransitionConfig()
        );
    }

    @Override
    public boolean storeIdentity() {
        return true;
    }
}
