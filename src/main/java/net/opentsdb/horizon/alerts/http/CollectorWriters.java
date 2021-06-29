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

package net.opentsdb.horizon.alerts.http;

import net.opentsdb.horizon.alerts.EnvironmentConfig;


public class CollectorWriters {

    private static EnvironmentConfig environmentConfig = new EnvironmentConfig();

    private static class WRAPPER {

        private CollectorWriter collectorWriter;
    }

    private static final WRAPPER statusWrapper = new WRAPPER();

    private static final WRAPPER popWrapper = new WRAPPER();

    public static CollectorWriter getStatusCollectorWriter() {

        if(statusWrapper.collectorWriter == null) {
            init(statusWrapper,
                    "status",
                    (environmentConfig.getMaxStatusBacklogToCollector()/
                            environmentConfig.getNumCollectorClients()));
        }

        return statusWrapper.collectorWriter;

    }

    public static CollectorWriter getPopCollectorWriter() {

        if(popWrapper.collectorWriter == null) {

            init(popWrapper,
                    "pop",
                    (environmentConfig.getMaxPopBacklogToCollector()/
                            environmentConfig.getNumCollectorClients()));
        }

        return popWrapper.collectorWriter;
    }


    private static void init(WRAPPER wrapper, String type, int maxBacklog) {
        if(wrapper.collectorWriter == null) {
            synchronized (wrapper) {
                if(wrapper.collectorWriter == null) {

                    final int numCollectorClients
                            = environmentConfig.getNumCollectorClients();
                    CollectorWriterClient[] collectorWriterClients
                            = new CollectorWriterClient[numCollectorClients];

                    for(int i = 0; i < numCollectorClients; i++) {
                        collectorWriterClients[i]
                                = CollectorWriterClient.Builder.create()
                                .ofType(type)
                                .withCollectorHostPort(environmentConfig.getCollectorUrl())
                                .sizeToFlush(environmentConfig.getCollectorFlushSize())
                                .timeToFlushInSecs(environmentConfig.getCollectorBatchTimeout())
                                .flushFrequencyInMs(environmentConfig.getCollectorFlushFrequency())
                                .blockingQueueSizeMax(maxBacklog)
                                .withIndex(i)
                                .build();
                    }

                    wrapper.collectorWriter = new CollectorWriter(collectorWriterClients);
                }
            }
        }
    }


}
