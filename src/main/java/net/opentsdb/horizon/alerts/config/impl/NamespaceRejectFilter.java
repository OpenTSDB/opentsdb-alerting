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
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class NamespaceRejectFilter implements NamespaceFetcher {

    private final Supplier<Optional<List<String>>> namespacesToRejectSupplier;

    private static final List<String> emptyList = Collections.EMPTY_LIST;

    private final NamespaceFetcher seedNamespaceFetcher;

    private NamespaceRejectFilter(final Supplier<Optional<List<String>>> namespacesToReject,
                                  final NamespaceFetcher seedNamespaceFetcher) {
        this.namespacesToRejectSupplier = namespacesToReject;
        this.seedNamespaceFetcher = seedNamespaceFetcher;
    }

    @Override
    public List<String> getNamespaces() {

        final List<String> rejectList = Collections.unmodifiableList(
                namespacesToRejectSupplier
                .get()
                .orElse(emptyList));

        return Collections.unmodifiableList(
                seedNamespaceFetcher.getNamespaces()
                    .stream()
                    .filter(namespace -> !rejectList.contains(namespace))
                    .collect(Collectors.toList())
        );
    }

    public static class Builder {

        private Supplier<Optional<List<String>>> _namespacesToReject;

        private NamespaceFetcher _seedNamespaceFetcher;

        private Builder() {

        }
        public Builder withNamsespaceRejectList(final Supplier<Optional<List<String>>> namespacesToReject) {
            this._namespacesToReject = namespacesToReject;
            return this;
        }

        public Builder seedNamespaceFetcher(final NamespaceFetcher seedNamespaceFetcher) {
            this._seedNamespaceFetcher = seedNamespaceFetcher;
            return this;
        }

        public static Builder create() {
            return new Builder();
        }

        public NamespaceRejectFilter build() {
            return new NamespaceRejectFilter(
                    this._namespacesToReject,
                    this._seedNamespaceFetcher
            );
        }
    }
}
