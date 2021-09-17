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

package net.opentsdb.horizon.alerting.corona.model.messagekit.meta;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpsGenieMetaSerializerTest {

    @Test
    void testWriteRead() {
        final OpsGenieMeta expected = OpsGenieMeta.builder()
                .setSubject("subject")
                .setBody("body")
                .setLabels("opsgenie", "meta")
                .setOpsGeniePriority("P5")
                .setOpsGenieAutoClose(true)
                .setOpsGenieTags(Arrays.asList("hello", "world"))
                .build();

        final OpsGenieMetaSerializer serializer = new OpsGenieMetaSerializer();
        final OpsGenieMeta actual = serializer.fromBytes(serializer.toBytes(expected));

        assertEquals(expected, actual);
    }
}
