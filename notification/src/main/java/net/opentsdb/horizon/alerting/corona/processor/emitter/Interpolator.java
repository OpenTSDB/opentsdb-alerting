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

package net.opentsdb.horizon.alerting.corona.processor.emitter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Interpolator {

    public static final Pattern TAG_KEY = Pattern.compile("\\{\\{([a-zA-Z0-9._:\\-/\\\\]+)}}");
    public static final Logger LOG = LoggerFactory.getLogger(Interpolator.class);

    /**
     * Safe version of the {@link Interpolator#interpolate(String, String[], String[])} method,
     * when original string is returned on any kind of failure.
     *
     * @param original string to interpolate.
     * @param keys     arrays of tag keys.
     * @param values   array of corresponding tag values.
     * @return interpolated string on success, original string on failure.
     */
    public static String tryInterpolate(String original, String[] keys, String[] values) {
        try {
            return interpolate(original, keys, values);
        } catch (Exception e) {
            LOG.warn("Interpolation failed: original=<<{}>>, keys={}, values={}, reason={}",
                    original, keys, values, e.getMessage());
            return original;
        }
    }

    public static String interpolate(String original, String[] keys, String[] values) {
        Objects.requireNonNull(keys, "keys cannot be null");
        Objects.requireNonNull(values, "values cannot be null");
        if (keys.length != values.length) {
            throw new IllegalArgumentException(String.format(
                    "keys.length != values.length: %d != %d", keys.length, values.length
            ));
        }
        Map<String, String> tags = new HashMap<>(keys.length);
        for (int i = 0; i < keys.length; ++i) {
            tags.put(keys[i], values[i]);
        }
        return interpolate(original, tags);
    }

    public static String interpolate(String original, Map<String, String> tags) {
        if (original == null || tags == null) {
            return original;
        }
        StringBuffer sb = new StringBuffer();
        Matcher matcher = TAG_KEY.matcher(original);

        while (matcher.find()) {
            String key = matcher.group(1);
            matcher.appendReplacement(sb, tags.getOrDefault(key, "$0"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
