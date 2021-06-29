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

package net.opentsdb.horizon.alerts.heartbeat;

import net.opentsdb.horizon.alerts.EnvironmentConfig;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MirrorHealthCheck {



    private final EnvironmentConfig config = new EnvironmentConfig();

    private final String MIRROR_SERVER_URL = "http://%s:%s/%s";

    private volatile int EFFECTIVE_NUMBER_OF_MIRRORS = config.getNumberOfMirrors();

    private Map<String,byte[]> mirrorMap = new HashMap<>();

    private Map<String,Boolean> mirrorAvail = new HashMap<>();

    private final CloseableHttpClient closeableHttpClient;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private static final int TIMEOUT_MS = 10000;

    private static final Logger LOG = LoggerFactory.getLogger(MirrorHealthCheck.class);

    private static final int runfreq = 60;

    private static boolean inited = false;

    private final int INTERVALS_TO_CHANGE;

    private final int INTERVALS_TO_RECOVER;

    private final int OUT_FACTOR;

    private final int initailDelayInSecs = 1;

    private static final int secsInhour = 3600;

    private final long startTimeInSecsBucketed;

    private static class MIRROR_WRAPPER {
        private static MirrorHealthCheck mirrorHealthCheck = new MirrorHealthCheck();

    }

    private MirrorHealthCheck() {


        INTERVALS_TO_CHANGE = config.getIntervalsToChange();

        INTERVALS_TO_RECOVER = config.getIntervalsToRecover();

        OUT_FACTOR = config.getIntervalsToChange()/2;

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_MS)
                .setSocketTimeout(TIMEOUT_MS)
                .setConnectionRequestTimeout(TIMEOUT_MS)
                .build();
        closeableHttpClient = HttpClientBuilder.create()
                                .setDefaultRequestConfig(requestConfig)
                                .build();
        this.startTimeInSecsBucketed = getBucketedCurrTime();
    }

    private void start() {
        EFFECTIVE_NUMBER_OF_MIRRORS = config.getMirrors().length;
        final String[] mirrors = config.getMirrors();

        if(mirrors != null) {
            for (String mirror : mirrors) {
                //Ignore current host
                if(!mirror.equalsIgnoreCase(config.getBaseHost())) {
                    mirrorMap.put(mirror, new byte[runfreq]);
                    mirrorAvail.put(mirror, true);
                }
            }
        }

        executorService.scheduleAtFixedRate(this::checkActiveMirrors,initailDelayInSecs,runfreq,TimeUnit.SECONDS);
    }

    public void checkActiveMirrors() {
        LOG.info("Mirrors: {}",mirrorAvail);
        if(config.isMirroringEnabled()) {
            mirrorMap
                    .forEach((mirror, window) -> {

                        final long l = System.currentTimeMillis();
                        final long period = l - l % runfreq;

                        final int startIndex = (int) ((period - (period - period % secsInhour)) / runfreq);

                        if (checkMirrorHealth(mirror)) {
                            window[startIndex] = 0;
                            if (!mirrorAvail.get(mirror)) {
                                final int countOfFailures = getCountOfFailures(mirror,
                                                            window,
                                                            startIndex,
                                                            mirrorAvail,INTERVALS_TO_RECOVER);
                                if (countOfFailures == 0) {
                                    EFFECTIVE_NUMBER_OF_MIRRORS = EFFECTIVE_NUMBER_OF_MIRRORS + 1;
                                    LOG.info("Adding mirror: {} total: {}", mirror, EFFECTIVE_NUMBER_OF_MIRRORS);
                                    mirrorAvail.put(mirror, true);
                                }
                            }
                        } else {
                            window[startIndex] = 1;
                            if (mirrorAvail.get(mirror)) {
                                final long bucketedCurrTime = getBucketedCurrTime();
                                //Remove the failed mirrors on start up by default.
                                if (((bucketedCurrTime - startTimeInSecsBucketed) / runfreq) < (OUT_FACTOR +1)) {
                                    EFFECTIVE_NUMBER_OF_MIRRORS = EFFECTIVE_NUMBER_OF_MIRRORS - 1;
                                    LOG.info("Removing mirror: {} total: {}", mirror, EFFECTIVE_NUMBER_OF_MIRRORS);
                                    mirrorAvail.put(mirror, false);
                                }

                                final int countOfFailures = getCountOfFailures(mirror,
                                                                               window,
                                                                               startIndex, mirrorAvail,OUT_FACTOR);
                                //Remove mirror
                                if (countOfFailures >= OUT_FACTOR) {
                                    EFFECTIVE_NUMBER_OF_MIRRORS = EFFECTIVE_NUMBER_OF_MIRRORS - 1;
                                    LOG.info("Removing mirror: {} total: {}", mirror, EFFECTIVE_NUMBER_OF_MIRRORS);
                                    mirrorAvail.put(mirror, false);
                                }
                            }
                        }

                    });
        }

    }

    private int getCountOfFailures(final String mirror,
                                     final byte[] window,
                                     final int startIndex,
                                     final Map<String, Boolean> mirrorAvail,
                                     final int INTERVALS_TO_CHANGE) {

        int runningCount = 0;
        for(int i = 0,j = startIndex; i < INTERVALS_TO_CHANGE; i++) {

            runningCount += window[j];

            j = prevIndexCircular(window,j);

        }

        return runningCount;

    }

    private static int prevIndexCircular(byte[] longArr, int j) {

        int k = j -1;

        if(k < 0) {
            return (longArr.length +k);
        } else {
            return k;
        }

    }

    private boolean checkMirrorHealth(String mirror) {

        String getUrl = String.format(MIRROR_SERVER_URL,mirror,config.getPort(),config.getHeartbeatServerPath());

        HttpGet httpGet = new HttpGet(getUrl);

        try {
            final CloseableHttpResponse execute =  closeableHttpClient.execute(httpGet);
            final int code = execute.getStatusLine().getStatusCode();
            final String entity = EntityUtils.toString(execute.getEntity());
            if(code != 200) {
                LOG.error("Request to {} , failed with code {} and error {}",mirror, code,entity);
                return false;
            } else {
                if(entity.trim().equalsIgnoreCase(HeartbeatServer.OK)) {
                    return true;
                }
            }
        } catch (IOException e) {
            LOG.error("Exception while calling {} ",mirror,e);
        }

        return false;
    }


    public static int getEffectiveNumberOfMirrors() {
        return MIRROR_WRAPPER.mirrorHealthCheck.EFFECTIVE_NUMBER_OF_MIRRORS;
    }

    private static long getBucketedCurrTime() {
        final long l = System.currentTimeMillis() / 1000;
        return (l - l % runfreq);

    }


    public static synchronized void initMirrorHealthCheck() {
        if(inited) {
            return;
        } else {

            MIRROR_WRAPPER.mirrorHealthCheck.start();
            inited = true;
        }
    }




}
