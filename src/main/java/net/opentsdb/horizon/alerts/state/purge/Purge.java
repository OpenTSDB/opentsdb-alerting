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

package net.opentsdb.horizon.alerts.state.purge;

import net.opentsdb.horizon.alerts.state.AlertStateStore;

import java.util.ArrayList;
import java.util.List;

public class Purge {

    private final List<PurgePolicy> policies = new ArrayList<>();

    public Purge(List<PurgePolicy> policies) {
        this.policies.addAll(policies);
    }

    public void purge(AlertStateStore alertStateStore, boolean firstPurge) {
        for(PurgePolicy policy : policies) {
            policy.purge(alertStateStore, firstPurge);
        }
    }

    public static class PurgeBuilder {

        private final List<PurgePolicy> policiesLocal = new ArrayList<>();

        public PurgeBuilder addPolicy(PurgePolicy purgePolicy) {
            policiesLocal.add(purgePolicy);
            return this;
        }

        public static PurgeBuilder create() {
            return new PurgeBuilder();
        }

        public Purge build() {
            return new Purge(policiesLocal);
        }

    }

}
