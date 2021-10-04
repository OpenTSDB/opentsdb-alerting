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

package net.opentsdb.horizon.alerting.corona.model.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataTest {

    @Test
    private void testBuilder()
    {
        final Metadata metadata = Metadata.builder()
                .setSubject("subject")
                .setBody("body")
                .setLabels("one", "two")
                .setOcSeverity(OcSeverity.SEV_4)
                .setOcTier(OcTier.TIER_2)
                .setOpsGeniePriority("P5")
                .setOpsGenieAutoClose(true)
                .setRunbookId("RB4000")
                .build();

        assertEquals("subject", metadata.getSubject());
        assertEquals("body", metadata.getBody());
        assertArrayEquals(new String[] {"one", "two"}, metadata.getLabels());
        assertEquals(OcSeverity.SEV_4, metadata.getOcSeverity());
        assertEquals(OcTier.TIER_2, metadata.getOcTier());
        assertEquals("P5", metadata.getOpsGeniePriority());
        assertTrue(metadata.isOpsGenieAutoClose());
        assertEquals("RB4000", metadata.getRunbookId());
    }
}