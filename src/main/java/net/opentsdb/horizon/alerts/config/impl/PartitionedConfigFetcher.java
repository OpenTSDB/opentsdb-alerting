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

import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.AlertConfigFetcher;
import lombok.Builder;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
@Builder
public class PartitionedConfigFetcher implements AlertConfigFetcher {


    private final AlertConfigFetcher alertConfigFetcher;

    private final int daemonid;
    private final int totalNumberOfDaemons;
    private final int mirrorid;
    private final int totalNumberOfMirrors;
    private final int mirrorSetId;
    private final int totalNumberMirrorSets;


    @Override
    public Map<Long, AlertConfig> getAlertConfig() {
        return alertConfigFetcher.getAlertConfig()
                .entrySet()
                .stream()
                .filter(entry -> inBucket(entry.getKey(),mirrorSetId,totalNumberMirrorSets))
                .filter(entry -> inBucket(entry.getKey(), mirrorid, totalNumberOfMirrors))
                .filter(entry -> inBucket(entry.getKey(),daemonid,totalNumberOfDaemons))
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }


    private boolean inBucket(long alertId,int bid,int totalBucketSets) {

        final long l = alertId % totalBucketSets;
        if(l == bid) {
            return true;
        } else {
            return false;
        }

    }
}
