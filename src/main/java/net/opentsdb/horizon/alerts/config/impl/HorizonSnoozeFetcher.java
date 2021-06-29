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

import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.model.Snooze;
import net.opentsdb.horizon.alerts.config.SnoozeFetcher;

import java.io.IOException;
import java.util.Map;

import net.opentsdb.horizon.alerts.AlertUtils;

public class HorizonSnoozeFetcher implements SnoozeFetcher {

    private static HorizonFetcher<Snooze> snoozeHorizonFetcher;

    public HorizonSnoozeFetcher(){
        init(new EnvironmentConfig());
    }

    private synchronized void init(EnvironmentConfig environmentConfig) {
        if(snoozeHorizonFetcher == null) {
            final HorizonFetcher.Builder<Snooze> snoozeBuilder = new HorizonFetcher.Builder<>();


            final HorizonFetcher.Builder<Snooze> builder = snoozeBuilder
                    .withAuthProvider(environmentConfig.getConfigDbAuthProvider())
                    .withFetchPath("snooze")
                    .withEndpoint(environmentConfig.getConfigDbEndpoint())
                    .withMirrorId(environmentConfig.getMirrorId())
                    .configFromString(string ->
                    {
                        try {
                            return AlertUtils.jsonMapper.readValue(string, Snooze.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    })
                    .idFromConfig(Snooze::getId);

            if(environmentConfig.inTestEnv()) {
                builder.inTest();
            }
            snoozeHorizonFetcher = builder.build();
            snoozeHorizonFetcher.init();

        }

    }


    @Override
    public Map<Long, Snooze> getSnoozeConfig() {

        return snoozeHorizonFetcher.getConfig();
    }
}
