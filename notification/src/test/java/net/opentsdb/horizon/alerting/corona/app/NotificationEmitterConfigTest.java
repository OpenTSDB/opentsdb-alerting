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

package net.opentsdb.horizon.alerting.corona.app;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NotificationEmitterConfigTest {

    @Test
    public void testToString() {
        final Configuration cfg = new PropertiesConfiguration();
        cfg.addProperty("emitter.type", "email");

        cfg.addProperty("kafka.topic", "hot-topic");
        cfg.addProperty("kafka.group.id", "hot-group");
        cfg.addProperty("kafka.zookeeper.connect", "zookeeper.host");

        cfg.addProperty("monitoring.namespace", "skhegay");

        cfg.addProperty("oc.denied.namespaces", "OpenTSDB,Test2");

        final NotificationEmitterConfig config =
                new NotificationEmitterConfig(cfg);

        System.out.println(config);
    }

    @Test
    void getSyntheticAlertIDs() {
        NotificationEmitterConfig config;
        config = new NotificationEmitterConfig(new PropertiesConfiguration());
        assertEquals(Collections.singletonList("3214"), config.getSyntheticAlertIds());

        config = new NotificationEmitterConfig(new PropertiesConfiguration() {{
            addProperty("synthetic.alert.ids", "1,3,5");
        }});
        assertEquals(Arrays.asList("1", "3", "5"), config.getSyntheticAlertIds());
    }

    @Test
    void getOcDeniedNamespaces() {
        NotificationEmitterConfig config;
        config = new NotificationEmitterConfig(new PropertiesConfiguration());
        assertEquals(Collections.emptyList(), config.getOcDeniedNamespaces());

        config = new NotificationEmitterConfig(new PropertiesConfiguration() {{
            addProperty("oc.denied.namespaces", "OpenTSDB,Test2");
        }});
        assertEquals(Arrays.asList("OpenTSDB", "Test2"), config.getOcDeniedNamespaces());
    }
}
