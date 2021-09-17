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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OcTierTest {

    @Test
    void validateTierIds()
    {
        assertEquals((byte) 0, OcTier.NOT_SET.getId());
        assertEquals((byte) 1, OcTier.TIER_1.getId());
        assertEquals((byte) 2, OcTier.TIER_2.getId());
        assertEquals((byte) 3, OcTier.TIER_3.getId());
        assertEquals((byte) 4, OcTier.TIER_4.getId());
    }

    @Test
    void fromId()
    {
        assertEquals(OcTier.fromId((byte)0), OcTier.NOT_SET);
        assertEquals(OcTier.fromId((byte)1), OcTier.TIER_1);
        assertEquals(OcTier.fromId((byte)2), OcTier.TIER_2);
        assertEquals(OcTier.fromId((byte)3), OcTier.TIER_3);
        assertEquals(OcTier.fromId((byte)4), OcTier.TIER_4);

        assertThrows(IllegalArgumentException.class, () -> OcTier.fromId((byte)-1));
        assertThrows(IllegalArgumentException.class, () -> OcTier.fromId((byte)5));
    }
}