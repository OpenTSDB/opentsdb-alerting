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

import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OcMetaSerializerTest {

    @Test
    void testWriteRead()
    {
        final OcMeta expected = OcMeta.builder()
                .setSubject("subject")
                .setBody("body")
                .setLabels("oc", "meta")
                .setRunbookId("RB0007")
                .setOcSeverity(OcSeverity.SEV_4)
                .setOcTier(OcTier.TIER_2)
                .build();

        final OcMetaSerializer serializer = new OcMetaSerializer();
        final OcMeta actual = serializer.fromBytes(serializer.toBytes(expected));

        assertEquals(expected, actual);
    }

}