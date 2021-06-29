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

package net.opentsdb.horizon.alerts.query.egads;

import java.util.List;
import java.util.Optional;

import net.opentsdb.horizon.core.builder.CoreBuilder;

public interface EgadsResponse {

    long getStartTimeSec();

    long getEndTimeSec();

    Optional<String> getMetricName();

    List<EgadsDataItem> getDataItems();

    interface Builder<B extends Builder<B>> extends CoreBuilder<B, EgadsResponse> {

        B setStartTimeSec(long startTimeSec);

        B setEndTimeSec(long endTimeSec);

        /**
         * Set the metric name.
         *
         * @param metricName name, can be null.
         * @return builder
         */
        B setMetricName(String metricName);

        B setDataItems(List<EgadsDataItem> dataItems);

    }

}
