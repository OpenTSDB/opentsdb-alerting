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

import net.opentsdb.horizon.alerts.config.NamespaceFetcher;

import java.util.Collections;
import java.util.List;

public class StaticNamespaceFetcher implements NamespaceFetcher {

    private final List<String> namespaces;

    private StaticNamespaceFetcher(final List<String> namespaces) {
        this.namespaces = Collections.unmodifiableList(namespaces);
    }

    @Override
    public List<String> getNamespaces() {

        return Collections.unmodifiableList(namespaces);
    }

    public static StaticNamespaceFetcher create(final List<String> namespaces) {

        return new StaticNamespaceFetcher(namespaces);
    }
}
