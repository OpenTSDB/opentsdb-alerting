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

package net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie.formatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import net.opentsdb.horizon.alerting.corona.processor.emitter.Interpolator;

public class OpsGenieTagFormatter {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Formats OpsGenie tags.
     * If `OpsGenie Tags` are not provided, then we use Horizon labels, to
     * ensure backward-compatibility. Otherwise, provided `OpsGenie Tags` will
     * be interpolated and used as actual tags in OpsGenie.
     *
     * @param labels       Horizon labels.
     * @param opsgenieTags interpolatable `OpsGenie Tags` configuration list.
     * @param tagKeys      group tag keys
     * @param tagValues    group tag values
     * @return labels if opsgenieTags not provided, or interpolated opsgenieTags.
     */
    public static String[] formatTags(
            String[] labels,
            List<String> opsgenieTags,
            String[] tagKeys,
            String[] tagValues) {
        if (opsgenieTags == null) {
            // OpsGenie tags are not configured, just use the labels.
            return labels;
        }

        // Clean up the values.
        opsgenieTags = opsgenieTags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (opsgenieTags.isEmpty()) {
            // We had bad values in configured opsgenie tags, just use labels
            return labels;
        }

        String[] tags = opsgenieTags.toArray(EMPTY_STRING_ARRAY);

        if (tagKeys == null || tagValues == null || tagKeys.length != tagValues.length) {
            // We have nothing to interpolate with, or have malformed message kit.
            return tags;
        }

        Map<String, String> groupTags = new HashMap<>(tagKeys.length);
        for (int i = 0; i < tagKeys.length; ++i) {
            groupTags.put(tagKeys[i], tagValues[i]);
        }

        for (int i = 0; i < tags.length; ++i) {
            tags[i] = Interpolator.interpolate(tags[i], groupTags);
        }
        return tags;
    }
}
