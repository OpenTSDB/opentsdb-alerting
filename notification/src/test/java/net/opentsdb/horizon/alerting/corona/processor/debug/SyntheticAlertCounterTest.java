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

package net.opentsdb.horizon.alerting.corona.processor.debug;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import mockit.Mocked;
import mockit.Verifications;
import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.Comparator;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alert.WindowSampler;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricSimpleAlert;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricSimpleAlert.Builder;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyntheticAlertCounterTest {

    @Mocked
    AppMonitor appMonitor;

    @Test
    void process() {
        final SyntheticAlertCounter<Alert> tested =
                new SyntheticAlertCounter<>(
                        Collections.singletonList("1234"),
                        appMonitor
                );

        final Builder<?> commonBuilder =
                SingleMetricSimpleAlert.builder()
                        .setTimestampSec(123L)
                        .setNamespace("OpenTSDB")
                        .setStateFrom(State.BAD)
                        .setState(State.GOOD)
                        .setComparator(Comparator.EQUALS)
                        .setSampler(WindowSampler.SUMMARY);

        tested.process(commonBuilder.setId(1).build());
        tested.process(commonBuilder.setId(1234).build());
        tested.process(commonBuilder.setId(2).build());
        tested.process(commonBuilder.setId(1234).build());
        tested.process(commonBuilder.setId(1234).build());
        tested.process(commonBuilder.setId(3).build());

        new Verifications() {{
            appMonitor.countSyntheticAlertReceived(1, 1234);
            appMonitor.countSyntheticAlertReceived(1, 1234);
            appMonitor.countSyntheticAlertReceived(1, 1234);
        }};
    }

    @Test
    void buildSyntheticAlertIDsMap() {
        Long2BooleanMap map;

        map = SyntheticAlertCounter.buildSyntheticAlertIDsMap(
                Collections.singletonList("3")
        );
        assertEquals(1, map.size());
        assertTrue(map.containsKey(3L));

        map = SyntheticAlertCounter.buildSyntheticAlertIDsMap(
                Lists.newArrayList("3", "", null, "42")
        );
        assertEquals(2, map.size());
        assertTrue(map.containsKey(3L));
        assertTrue(map.containsKey(42L));

        map = SyntheticAlertCounter.buildSyntheticAlertIDsMap(
                Collections.emptyList()
        );
        assertEquals(0, map.size());
    }
}