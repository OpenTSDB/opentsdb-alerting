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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class OpsGenieTagFormatterTest {

    @Test
    void formatTags() {
        // If OpsGenie tags are null, should return labels.
        assertArrayEquals(
                new String[]{"label1", "label2"},
                OpsGenieTagFormatter.formatTags(
                        new String[]{"label1", "label2"},
                        null,
                        new String[]{"host", "app"},
                        new String[]{"localhost", "nextfacebook"}
                )
        );
        // If OpsGenie tags are empty, should return labels.
        assertArrayEquals(
                new String[]{"label1", "label2"},
                OpsGenieTagFormatter.formatTags(
                        new String[]{"label1", "label2"},
                        Collections.emptyList(),
                        new String[]{"host", "app"},
                        new String[]{"localhost", "nextfacebook"}
                )
        );
        // If OpsGenie tags are empty string, should return labels.
        assertArrayEquals(
                new String[]{"label1", "label2"},
                OpsGenieTagFormatter.formatTags(
                        new String[]{"label1", "label2"},
                        Arrays.asList("", " ", "  "),
                        new String[]{"host", "app"},
                        new String[]{"localhost", "nextfacebook"}
                )
        );
        // If group keys are null, should return non-interpolated OpsGenie tags.
        assertArrayEquals(
                new String[]{"host-{{host}}", "app-{{app}}"},
                OpsGenieTagFormatter.formatTags(
                        new String[]{"label1", "label2"},
                        Arrays.asList("host-{{host}}", "app-{{app}}"),
                        null,
                        new String[]{"localhost", "nextfacebook"}
                )
        );
        // If group values are null, should return non-interpolated OpsGenie tags.
        assertArrayEquals(
                new String[]{"host-{{host}}", "app-{{app}}"},
                OpsGenieTagFormatter.formatTags(
                        new String[]{"label1", "label2"},
                        Arrays.asList("host-{{host}}", "app-{{app}}"),
                        new String[]{"host", "app"},
                        null
                )
        );
        // If group keys and values do not match, should return non-interpolated OpsGenie tags.
        assertArrayEquals(
                new String[]{"host-{{host}}", "app-{{app}}"},
                OpsGenieTagFormatter.formatTags(
                        new String[]{"label1", "label2"},
                        Arrays.asList("host-{{host}}", "app-{{app}}"),
                        new String[]{"host", "app"},
                        new String[]{"localhost", "nextfacebook", "roguevalue"}
                )
        );
        // If all conditions are met, should return interpolated OpsGenie tags.
        assertArrayEquals(
                new String[]{"host-localhost", "app-nextfacebook"},
                OpsGenieTagFormatter.formatTags(
                        new String[]{"label1", "label2"},
                        Arrays.asList("host-{{host}}", "app-{{app}}"),
                        new String[]{"host", "app"},
                        new String[]{"localhost", "nextfacebook"}
                )
        );
    }
}