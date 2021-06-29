/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.config.impl;

import com.beust.jcommander.internal.Lists;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.config.NamespaceFetcher;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class NamespaceRejectFilterTest {

    @Test
    public void test_GetNamespaces() {

        EnvironmentConfig.seedFromFile("src/test/resources/config2.properties");
        
        EnvironmentConfig environmentConfig = new EnvironmentConfig();

        NamespaceFetcher namespaceFetcher = StaticNamespaceFetcher.create(Lists.newArrayList("NS", "Test" ));

        NamespaceRejectFilter namespaceRejectFilter = NamespaceRejectFilter.Builder.create()
                .seedNamespaceFetcher(namespaceFetcher)
                .withNamsespaceRejectList(() -> environmentConfig.getNamespacesToReject())
                .build();
        assertEquals(namespaceRejectFilter.getNamespaces(), Lists.newArrayList("NS"));
    }
}
