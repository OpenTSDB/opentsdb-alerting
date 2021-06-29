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

import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.processor.Conditional;
import net.opentsdb.horizon.alerts.query.tsdb.TSDBV3SlidingWindowQuery;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class HeartbeatSuppressConditional implements Conditional {

    private static final Logger LOG = LoggerFactory.getLogger(TSDBV3SlidingWindowQuery.class);

    private Long2BooleanMap heartbeatMap;
    private Set<String> tagKeys;
    private String namespace;
    private long alertId;

    public HeartbeatSuppressConditional(final Long2BooleanMap booleanMap,
                                        final Set<String> tagKeys,
                                        final String namespace,
                                        final long alertId) {
        this.heartbeatMap = booleanMap;
        this.tagKeys = tagKeys;
        this.namespace = namespace;
        this.alertId = alertId;
    }

    @Override
    public Boolean checkCondition(SortedMap<String, String> tagsMap) {
        final TreeMap<String, String> subMap = new TreeMap<>(tagsMap);
        subMap.keySet().retainAll(tagKeys);
        final long hashCode = AlertUtils.getHashForNAMT(namespace, alertId, subMap);
        if (!this.heartbeatMap.containsKey(hashCode)) {
            LOG.info("id: {} not in heartbeat subMap: {}", alertId, convertWithStream(subMap));
            return false;
        }
        if (this.heartbeatMap.containsKey(hashCode) && this.heartbeatMap.get(hashCode)) {
            LOG.info("id: {} has value set to true in submap: {}", alertId, convertWithStream(subMap));
            return false;
        }
        return true;
    }

    private static String convertWithStream(SortedMap<String, ?> map) {
        return map.keySet().stream()
                .map(key -> key + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
