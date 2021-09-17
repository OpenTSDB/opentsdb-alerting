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

class OcSeverityTest {

    @Test
    void validateSeverityIds()
    {
        assertEquals((byte) 0, OcSeverity.NOT_SET.getId());
        assertEquals((byte) 1, OcSeverity.SEV_1.getId());
        assertEquals((byte) 2, OcSeverity.SEV_2.getId());
        assertEquals((byte) 3, OcSeverity.SEV_3.getId());
        assertEquals((byte) 4, OcSeverity.SEV_4.getId());
        assertEquals((byte) 5, OcSeverity.SEV_5.getId());
    }

    @Test
    void fromId()
    {
        assertEquals(OcSeverity.fromId((byte)0), OcSeverity.NOT_SET);
        assertEquals(OcSeverity.fromId((byte)2), OcSeverity.SEV_2);
        assertEquals(OcSeverity.fromId((byte)3), OcSeverity.SEV_3);
        assertEquals(OcSeverity.fromId((byte)4), OcSeverity.SEV_4);
        assertEquals(OcSeverity.fromId((byte)5), OcSeverity.SEV_5);

        assertThrows(IllegalArgumentException.class, () -> OcSeverity.fromId((byte)-1));
        assertThrows(IllegalArgumentException.class, () -> OcSeverity.fromId((byte)6));
    }
}