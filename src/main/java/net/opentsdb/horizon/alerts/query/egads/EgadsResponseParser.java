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

package net.opentsdb.horizon.alerts.query.egads;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.opentsdb.utils.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.core.validate.Validate;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.enums.ThresholdType;

public final class EgadsResponseParser {

    private static final Logger LOG =
            LoggerFactory.getLogger(EgadsResponseParser.class);
    private static final String KEY_ALERT_TYPE = "AlertType";
    private static final String KEY_DATA = "data";
    private static final String KEY_END = "end";
    private static final String KEY_INTERVAL = "interval";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_METRIC = "metric";
    private static final String KEY_NUMERIC_TYPE = "NumericType";
    private static final String KEY_RESULTS = "results";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_START = "start";
    private static final String KEY_TAGS = "tags";
    private static final String KEY_THRESHOLD = "threshold";
    private static final String KEY_TIME_SPECIFICATION = "timeSpecification";
    private static final String KEY_TYPE = "type";
    private static final String KEY_VALUE = "value";
    private static final String SFX_LOWER_BAD = ".lowerBad";
    private static final String SFX_LOWER_WARN = ".lowerWarn";
    private static final String SFX_PREDICTION = ".prediction";
    private static final String SFX_UPPER_BAD = ".upperBad";
    private static final String SFX_UPPER_WARN = ".upperWarn";
    private static final String TAG__ANOMALY_MODEL = "_anomalyModel";
    private static final String TYPE_LOWER_BAD = "lowerBad";
    private static final String TYPE_LOWER_WARN = "lowerWarn";
    private static final String TYPE_UPPER_BAD = "upperBad";
    private static final String TYPE_UPPER_WARN = "upperWarn";

    private static final Joiner.MapJoiner TAG_JOINER =
            Joiner.on(";").withKeyValueSeparator('#');

    public static EgadsResponseParser create(String egadsNodeId) {
        return new EgadsResponseParser(egadsNodeId);
    }

    private final String egadsNodeId;

    private EgadsResponseParser(String egadsNodeIdSubstring) {
        Validate.paramNotNull(egadsNodeIdSubstring, "egadsNodeId");
        this.egadsNodeId = egadsNodeIdSubstring;
    }

    public EgadsResponse parse(final String payload) {
        final JsonNode root;
        try {
            root = AlertUtils.parseJsonTree(payload);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse response.", e);
        }
        final JsonNode egadsNode = findEgadsNode(root);

        // Generate data timestamps.

        final JsonNode timeSpecNode = egadsNode.get(KEY_TIME_SPECIFICATION);
        final long startSec = timeSpecNode.get(KEY_START).asLong();
        final long endSecExclusive = timeSpecNode.get(KEY_END).asLong();
        final long intervalSec =
                DateTime.parseDuration(timeSpecNode.get(KEY_INTERVAL).asText()) / 1_000L;
        final long[] timestampsSec =
                generateTimestampsSec(startSec, endSecExclusive, intervalSec);
        final long endSec = endSecExclusive - intervalSec;

        // Process `data` node.

        final JsonNode dataNode = egadsNode.get(KEY_DATA);

        final String metric;
        final List<EgadsDataItem> dataItems;
        if (dataNode.size() == 0) {
            metric = null;
            dataItems = Collections.emptyList();
        } else {
            // Theoretically, we should have a metric name in the config, but parsing
            // the query is somewhat problematic. Here, in the response, we have a
            // good strategy how to do it.
            metric = parseMetric(dataNode);

            final List<EgadsDataItem.Builder<?>> dataItemBuilders = parseData(dataNode);
            dataItems = dataItemBuilders.stream()
                    .map(builder -> {
                        try {
                            return builder.setTimestampsSec(timestampsSec).build();
                        } catch (Exception e) {
                            LOG.error("Failed to build an EgadsDataItem: builder={}.", builder, e);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return DefaultEgadsResponse.builder()
                .setStartTimeSec(startSec)
                .setEndTimeSec(endSec)
                .setMetricName(metric)
                .setDataItems(dataItems)
                .build();
    }

    private JsonNode findEgadsNode(final JsonNode root) {
        final JsonNode resultsNode = root.get(KEY_RESULTS);
        for (JsonNode result : resultsNode) {
            if (result.get(KEY_SOURCE).asText().contains(egadsNodeId)) {
                return result;
            }
        }
        throw new RuntimeException("egads node not found in 'results'");
    }

    /**
     * Parse metric name from 'data' array.
     * <p>
     * Example of the 'data' node structure:
     * <pre>{@code
     *  ...
     *  "data": [
     *      {
     *          "metric": "egads.metric.hourly.prediction",
     *          "tags": {
     *              "_anomalyModel": "OlympicScoring",
     *              "host": "web02"
     *          },
     *          "AlertType": {
     *              "1546340580": {
     *                  "level": "WARN",
     *                  "value": -4.0,
     *                  "threshold": -1.5,
     *                  "type": "lower"
     *          },
     *          "NumericType": [...],
     *          "aggregateTags": [...]
     *      },
     *      {
     *          "metric": "egads.metric.hourly.lowerBad",
     *          "tags": {
     *              "_anomalyModel": "OlympicScoring",
     *              "host": "web02"
     *          },
     *          "NumericType": [...],
     *          "aggregateTags": [...]
     *      },
     *      {
     *          "metric": "egads.metric.hourly",
     *          "tags": {
     *              "host": "web02"
     *          },
     *          "NumericType": [...],
     *          "aggregateTags": [...]
     *     },
     *  ],
     *  ...
     * }</pre>
     *
     * @param dataNode non empty 'data' array node.
     * @return metric name.
     */
    final String parseMetric(final JsonNode dataNode) {
        final JsonNode anyNode = dataNode.get(0);
        String metric = anyNode.get(KEY_METRIC).asText();

        if (anyNode.hasNonNull(KEY_TAGS)
                && anyNode.get(KEY_TAGS).has(TAG__ANOMALY_MODEL)) {
            // This is a generated data from the model, hence .prediction,
            // .lower, or .upper is appended to the metric name.
            metric = metric.substring(0, metric.lastIndexOf("."));
        }

        return metric;
    }

    private List<EgadsDataItem.Builder<?>> parseData(final JsonNode dataNode) {
        final Map<String, EgadsDataItem.Builder<?>> dataBuilders = Maps.newHashMap();

        for (JsonNode entry : dataNode) {
            final SortedMap<String, String> tags = parseTags(entry.get(KEY_TAGS));
            final boolean isObservedData = !tags.containsKey(TAG__ANOMALY_MODEL);
            if (!isObservedData) {
                tags.remove(TAG__ANOMALY_MODEL);
            }

            final EgadsDataItem.Builder<?> builder = dataBuilders.computeIfAbsent(
                    getIdFromTags(tags),
                    (id) -> DefaultEgadsDataItem.builder().setTags(tags)
            );

            parseDataEntry(entry, isObservedData, builder);
        }

        return Lists.newArrayList(dataBuilders.values());
    }

    private SortedMap<String, String> parseTags(final JsonNode tagsNode) {
        final int size = tagsNode.size();
        if (size == 0) {
            return Collections.emptySortedMap();
        }

        final SortedMap<String, String> tags = new TreeMap<>();
        final Iterator<Map.Entry<String, JsonNode>> it = tagsNode.fields();
        while (it.hasNext()) {
            final Map.Entry<String, JsonNode> entry = it.next();
            tags.put(entry.getKey(), entry.getValue().asText());
        }

        return tags;
    }

    private String getIdFromTags(SortedMap<String, String> tags) {
        return TAG_JOINER.join(tags);
    }

    private void parseDataEntry(final JsonNode dataEntry,
                                final boolean isObservedData,
                                final EgadsDataItem.Builder<?> builder) {
        final double[] values = parseValuesArray(dataEntry.get(KEY_NUMERIC_TYPE));

        if (isObservedData) {
            builder.setObservedValues(values);
            return;
        }

        final String metric = dataEntry.get(KEY_METRIC).asText().trim();
        if (metric.endsWith(SFX_PREDICTION)) {
            builder.setPredictedValues(values);

            // `AlertType` field might be missing if there are no alerts.
            if (dataEntry.has(KEY_ALERT_TYPE)) {
                builder.setAlerts(
                        parseAlertsArray(dataEntry.get(KEY_ALERT_TYPE))
                );
            }
        } else if (metric.endsWith(SFX_LOWER_WARN)) {
            builder.setLowerWarnValues(values);
        } else if (metric.endsWith(SFX_LOWER_BAD)) {
            builder.setLowerBadValues(values);
        } else if (metric.endsWith(SFX_UPPER_WARN)) {
            builder.setUpperWarnValues(values);
        } else if (metric.endsWith(SFX_UPPER_BAD)) {
            builder.setUpperBadValues(values);
        } else {
            LOG.warn("Unknown metric suffix in 'data' entry: metric={}, entry={}",
                    metric, dataEntry);
        }
    }

    private double[] parseValuesArray(final JsonNode doublesNode) {
        final int n = doublesNode.size();
        final double[] doubles = new double[n];
        int i = 0;
        for (JsonNode doubleNode : doublesNode) {
            doubles[i++] = doubleNode.asDouble();
        }
        return doubles;
    }

    private long[] generateTimestampsSec(final long startSec,
                                         final long endSec,
                                         final long deltaSec) {
        final int n = (int) ((endSec - startSec) / deltaSec);
        final long[] timestamps = new long[n];

        int i = 0;
        long initial = startSec;
        while (i < n) {
            timestamps[i++] = initial;
            initial += deltaSec;
        }

        return timestamps;
    }

    private EgadsAlert[] parseAlertsArray(final JsonNode alertsNode) {
        final int n = alertsNode.size();
        final EgadsAlert[] alerts = new EgadsAlert[n];
        int i = 0;

        final Iterator<Map.Entry<String, JsonNode>> it = alertsNode.fields();
        while (it.hasNext()) {
            final Map.Entry<String, JsonNode> entry = it.next();
            try {
                alerts[i++] = parseAlertEntry(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                LOG.error("Failed to parse alert entry: entry={}", entry, e);
                throw new RuntimeException(e);
            }
        }

        final EgadsAlert[] toReturn = Arrays.stream(alerts)
                .filter(Objects::nonNull)
                .toArray(EgadsAlert[]::new);
        Arrays.sort(
                toReturn,
                (a, b) -> (int) (a.getTimestampSec() - b.getTimestampSec())
        );
        return toReturn;
    }

    private EgadsAlert parseAlertEntry(final String timestampSecStr,
                                       final JsonNode alertEntry) {
        long timestampSec = Long.parseLong(timestampSecStr);
        String levelStr = alertEntry.get(KEY_LEVEL).asText().trim().toUpperCase();
        String typeStr = alertEntry.get(KEY_TYPE).asText().trim();

        return DefaultEgadsAlert.builder()
                .setTimestampSec(timestampSec)
                .setAlertState(AlertState.valueOf(levelStr))
                .setMessage(alertEntry.get(KEY_MESSAGE).asText())
                .setObservedValue(alertEntry.get(KEY_VALUE).doubleValue())
                .setThresholdValue(alertEntry.get(KEY_THRESHOLD).doubleValue())
                .setThresholdType(parseThresholdType(typeStr))
                .build();
    }

    private ThresholdType parseThresholdType(final String typeStr) {
        switch (typeStr) {
            case TYPE_LOWER_WARN:
                return ThresholdType.LOWER_WARN;
            case TYPE_LOWER_BAD:
                return ThresholdType.LOWER_BAD;
            case TYPE_UPPER_WARN:
                return ThresholdType.UPPER_WARN;
            case TYPE_UPPER_BAD:
                return ThresholdType.UPPER_BAD;
        }
        throw new IllegalArgumentException("Unknown threshold type: " + typeStr);
    }
}
