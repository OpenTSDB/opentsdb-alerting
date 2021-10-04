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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OcMetaTest {

    @Test
    void testBuilder()
    {
        final OcMeta ocMeta = OcMeta.builder()
                .setSubject("subject")
                .setBody("body")
                .setLabels("one", "two")
                .setOcSeverity(OcSeverity.SEV_4)
                .setOcTier(OcTier.TIER_2)
                .setRunbookId("RB4000")
                .build();

        assertEquals("subject", ocMeta.getSubject());
        assertEquals("body", ocMeta.getBody());
        assertArrayEquals(new String[]{"one", "two"}, ocMeta.getLabels());
        Assertions.assertEquals(OcSeverity.SEV_4, ocMeta.getOcSeverity());
        Assertions.assertEquals(OcTier.TIER_2, ocMeta.getOcTier());
        assertEquals("RB4000", ocMeta.getRunbookId());
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
                .setRunbookId("RB4000")
                .build();

        final OcMeta ocMeta = OcMeta.from(metadata);
        assertEquals("subject", ocMeta.getSubject());
        assertEquals("body", ocMeta.getBody());
        assertArrayEquals(new String[]{"one", "two"}, ocMeta.getLabels());
        Assertions.assertEquals(OcSeverity.SEV_4, ocMeta.getOcSeverity());
        Assertions.assertEquals(OcTier.TIER_2, ocMeta.getOcTier());
        assertEquals("RB4000", ocMeta.getRunbookId());
    }
}