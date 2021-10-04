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

import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpsGenieMetaTest {

    @Test
    void testBuilder()
    {
        final OpsGenieMeta.Builder<?> builder = OpsGenieMeta.builder()
                .setSubject("subject")
                .setBody("body")
                .setLabels("opsgenie", "meta")
                .setOpsGeniePriority("P5")
                .setOpsGenieTags(Arrays.asList("hello", "world"));

        final OpsGenieMeta meta1 = builder.setOpsGenieAutoClose(true).build();
        assertEquals("subject", meta1.getSubject());
        assertEquals("body", meta1.getBody());
        assertArrayEquals(new String[]{"opsgenie", "meta"}, meta1.getLabels());
        assertEquals("P5", meta1.getOpsGeniePriority());
        assertTrue(meta1.isOpsGenieAutoClose());
        assertEquals(Arrays.asList("hello", "world"), meta1.getOpsGenieTags());

        final OpsGenieMeta meta2 = builder.setOpsGenieAutoClose(false).build();
        assertEquals("subject", meta2.getSubject());
        assertEquals("body", meta2.getBody());
        assertArrayEquals(new String[]{"opsgenie", "meta"}, meta2.getLabels());
        assertEquals("P5", meta2.getOpsGeniePriority());
        assertFalse(meta2.isOpsGenieAutoClose());
        assertEquals(Arrays.asList("hello", "world"), meta1.getOpsGenieTags());
    }

    @Test
    void fromMetadata()
    {
        final Metadata metadata = Metadata.builder()
                .setSubject("subject")
                .setBody("body")
                .setLabels("one", "two")
                .setOcSeverity(OcSeverity.SEV_4)
                .setOcTier(OcTier.TIER_2)
                .setOpsGeniePriority("P5")
                .setOpsGenieAutoClose(true)
                .setOpsGenieTags(Arrays.asList("hello", "world"))
                .setRunbookId("RB4000")
                .build();

        final OpsGenieMeta meta = OpsGenieMeta.from(metadata);
        assertEquals("subject", meta.getSubject());
        assertEquals("body", meta.getBody());
        assertArrayEquals(new String[]{"one", "two"}, meta.getLabels());
        assertEquals("P5", meta.getOpsGeniePriority());
        assertTrue(meta.isOpsGenieAutoClose());
        assertEquals(Arrays.asList("hello", "world"), meta.getOpsGenieTags());
    }
}
