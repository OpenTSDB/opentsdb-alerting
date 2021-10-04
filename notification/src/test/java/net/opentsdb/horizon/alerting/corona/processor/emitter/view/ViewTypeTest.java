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

package net.opentsdb.horizon.alerting.corona.processor.emitter.view;

import net.opentsdb.horizon.alerting.corona.model.alert.State;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewTypeTest {

    @Test
    void catchChangesInTheNumberOfAlertStates()
    {
        Assertions.assertEquals(5, State.values().length);
        assertEquals(
                Arrays.asList("GOOD", "BAD", "WARN", "UNKNOWN", "MISSING"),
                Arrays.stream(State.values())
                        .map(State::name)
                        .collect(Collectors.toList())
        );
    }
}