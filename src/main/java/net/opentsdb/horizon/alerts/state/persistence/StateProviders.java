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

package net.opentsdb.horizon.alerts.state.persistence;

public class StateProviders {

    //
    // TODO: Remove, this is a plug to make things work.
    //       Have no idea how to plug it in properly.
    //

    private static volatile StateProvider DEFAULT;

    private static final StateProvider NOOP = new NoopStateProvider();

    public static void initialize(final StateProvider stateProvider) {
        DEFAULT = stateProvider;
    }

    public static StateProvider getDefault() {
        if (DEFAULT == null) {
            return NOOP;
        }
        return DEFAULT;
    }
}
