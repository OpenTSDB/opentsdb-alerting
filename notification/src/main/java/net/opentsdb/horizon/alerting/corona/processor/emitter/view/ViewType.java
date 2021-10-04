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

/**
 * Alert view type is strongly related to the {@link State}.
 * <p>
 * The difference is that:
 * - {@link State#GOOD} corresponds to {@link ViewType#RECOVERY}.
 * - {@link ViewType#UNDEFINED} is used when state cannot be identified.
 */
public enum ViewType {

    BAD,
    WARN,
    RECOVERY,
    MISSING,
    UNKNOWN,
    UNDEFINED

}
