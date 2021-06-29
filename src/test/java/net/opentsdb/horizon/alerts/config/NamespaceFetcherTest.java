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

package net.opentsdb.horizon.alerts.config;

import com.beust.jcommander.internal.Lists;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class NamespaceFetcherTest {

    @Test
    public void test_getNamespaces_static() {
        List<String> namespaces = Lists.newArrayList("NS", "Argus");
        NamespaceFetcher namespaceFetcher = NamespaceFetchers.getStaticNamespaceFetcher(namespaces);
        assertEquals(Lists.newArrayList("NS", "Argus"), namespaceFetcher.getNamespaces());
    }

}
