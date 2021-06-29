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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.Monitoring;
import net.opentsdb.horizon.alerts.OutputWriter;
import net.opentsdb.horizon.alerts.http.impl.AthensAuthProvider;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.model.tsdb.Datum;
import net.opentsdb.horizon.alerts.model.tsdb.IMetric;
import net.opentsdb.horizon.alerts.model.tsdb.Tags;
import net.opentsdb.horizon.alerts.model.tsdb.YmsStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CollectorWriterClient implements Runnable, OutputWriter {


    private final ScheduledExecutorService scheduledExecutorService;

    private static String collectorUrlFormat = "https://%s";

    private static String pathFormat = "%s/yms/V2/sendMessage?namespace=%s";

    private final BlockingQueue<YmsStatusEvent> blockingQueue;

    private long time = System.currentTimeMillis();

    private final String collectorUrl;

    private final int sizeToFlush;
    private final long timeToFlushInMs;
    private ObjectMapper objectMapper = new ObjectMapper();
    private final int index;
    private final String type;
    private AlertHttpsClient alertHttpsClient ;

    CollectorWriterClient(String collectorHostPort,
                    String type,
                    int sizeToFlush,
                    int timeToFlushInSecs,
                    int flushFrequencyInMs,
                    int blockingQueueSizeMax,
                    int index) {
        this.collectorUrl = String.format(collectorUrlFormat,collectorHostPort);
        this.sizeToFlush = sizeToFlush;
        this.timeToFlushInMs = timeToFlushInSecs*1000;
        this.index = index;
        this.type = type;
        this.blockingQueue = new LinkedBlockingQueue<>(blockingQueueSizeMax);
        scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
        //System.out.println("Flush freq: "+ flushFrequencyInMs);
        scheduledExecutorService.scheduleAtFixedRate(this::run,10000,
                flushFrequencyInMs, TimeUnit.MILLISECONDS);

        alertHttpsClient = new AlertHttpsClient();
        alertHttpsClient.setAuthProvider(new AthensAuthProvider());

    }

    public void run() {
        try {
            final long currentTimeMillis = System.currentTimeMillis();
            log.debug("Started run of collector push: {} {}", blockingQueue.size(), currentTimeMillis);

            if ((blockingQueue.size() == 0)) {
                return;
            }

            log.debug("Collectors will push: {} {} {} {}",
                    (blockingQueue.size() >= sizeToFlush || ((currentTimeMillis - time) > timeToFlushInMs)),
                    timeToFlushInMs, (currentTimeMillis - time),
                    time);

            while (blockingQueue.size() >= sizeToFlush
                    ||
                    (System.currentTimeMillis() - time) > timeToFlushInMs) {

                Monitoring.get().gaugeCollectorQueueSize(blockingQueue.size());
                Map<String, StringBuilder> builderMap = new HashMap<>();

                for (int i = 0; i < sizeToFlush; i++) {

                    final YmsStatusEvent take = blockingQueue.poll();

                    if (Objects.nonNull(take)) {


                        final Datum datum = take.getDatum();

                        final Tags tags = datum.getTags();

                        final Map<String, IMetric> metrics1 = datum.getMetrics();

                        final ObjectNode globalNode = objectMapper.createObjectNode();

                        final ObjectNode tagNode = objectMapper.createObjectNode();

                        final ObjectNode metricsNode = objectMapper.createObjectNode();

                        for (String key : tags.getDimensions().keySet()) {
                            tagNode.put(key, tags.getDimensions().get(key));
                        }

                        for (Map.Entry<String, IMetric> entry : metrics1.entrySet()) {
                            metricsNode.put(entry.getKey(),
                                    String.valueOf(entry.getValue().getValue()));
                        }


                        globalNode.put("application", datum.getApplication());
                        globalNode.put("timestamp", datum.getTimestamp());
                        globalNode.set("dimensions", tagNode);
                        globalNode.set("metrics", metricsNode);

                        if (!Strings.isNullOrEmpty(datum.getStatus_msg())) {
                            globalNode.put("status_msg", datum.getStatus_msg());
                        }

                        final String line = globalNode.toString();
                        log.debug("lin to post in collector : {}", line);
                        final String namespace = datum.getCluster();
                        final StringBuilder stringBuilder = builderMap.computeIfAbsent(namespace, n -> new StringBuilder());

                        if (stringBuilder.length() != 0) {
                            stringBuilder.append('\n');

                        }
                        stringBuilder.append(line);
                        Monitoring.get().incStatusesToBePosted(namespace, type, index );
                    } else {
                        break;
                    }
                }

                for (String namespace : builderMap.keySet()) {
                    final String payload = builderMap.get(namespace).toString();
                    try {

                        post(namespace, payload);
                    } catch (Exception e) {
                        log.error("Error posting to {} {}", namespace, payload, e);
                    }
                }
                time = System.currentTimeMillis();

            }

            return;

        } catch (Throwable t) {
            log.error("Error running flush to collector");
        }
    }

    private void post(String namespace, String toString) throws UnsupportedEncodingException, AlertException {

        final String finalUrl = String.format(pathFormat, collectorUrl, namespace);

        log.debug("Posting status payload {} to namespace {}", toString, finalUrl);

        HttpPost httpPost = new HttpPost(finalUrl);

        httpPost.setHeader("Content-type", "application/json");

        httpPost.setEntity(new StringEntity(toString));
        final long currentTimeMillis1 = System.currentTimeMillis();
        final CloseableHttpResponse execute = alertHttpsClient.execute(httpPost);
        final long currentTimeMillis2 = System.currentTimeMillis();

        final int statusCode = execute.getStatusLine().getStatusCode();

        EntityUtils.consumeQuietly(execute.getEntity());

        if (statusCode == 200) {
            Monitoring.get().reportCollector2xx(1, namespace, type, index );
            Monitoring.get().reportCollectorPostLatency(
                    (currentTimeMillis2 - currentTimeMillis1), namespace, type, index );
            Monitoring.get().reportCollectorSuccesfullyBytesPosted(
                    toString.getBytes().length, namespace, type, index );
        } else if (statusCode == 500 || statusCode == 503 || statusCode == 502 || statusCode == 504) {
            Monitoring.get().reportCollector5xx(1, namespace, type, index );
            Monitoring.get().reportCollector5xxFailedBytesPosted(
                    toString.getBytes().length, namespace, type, index );
        } else if (statusCode == 400 || statusCode == 403 || statusCode == 401 || statusCode == 404) {
            Monitoring.get().reportCollector4xx(1, namespace, type, index );
            Monitoring.get().reportCollector4xxFailedBytesPosted(
                    toString.getBytes().length, namespace, type, index );
        }


    }


    @Override
    public void sendAlertEvent(AlertEventBag alertEventBag) {
        //Do nothing for now
    }

    @Override
    public void sendStatusEvent(YmsStatusEvent ymsStatusEvent) {
        if(time == -1) {
            time = System.currentTimeMillis();
        }
        log.debug("Adding {} to collector queue",ymsStatusEvent.toString());
        final boolean offer = blockingQueue.offer(ymsStatusEvent);

        if(!offer) {
            log.debug("Dropping event as queue is full: {}", ymsStatusEvent.toString());
            Monitoring.get().incDroppedCollectorEvent(ymsStatusEvent.getDatum().getCluster(), type, index);
        }

    }

    public static class Builder {

        private String collectorHostPort;
        private int sizeToFlush;
        private int timeToFlushInSecs;
        private int flushFrequencyInMs;
        private int blockingQueueSizeMax;
        private int index;
        private String type;

        public Builder withCollectorHostPort(String collectorHostPort) {
            this.collectorHostPort = collectorHostPort;
            return this;
        }

        public Builder ofType(String type) {
            this.type = type;
            return this;
        }

        public Builder sizeToFlush(int sizeToFlush) {
            this.sizeToFlush = sizeToFlush;
            return this;
        }

        public Builder timeToFlushInSecs(int timeToFlushInSecs) {
            this.timeToFlushInSecs = timeToFlushInSecs;
            return this;
        }

        public Builder flushFrequencyInMs(int flushFrequencyInMs) {
            this.flushFrequencyInMs = flushFrequencyInMs;
            return this;
        }

        public Builder blockingQueueSizeMax(int blockingQueueSizeMax) {
            this.blockingQueueSizeMax = blockingQueueSizeMax;
            return this;
        }

        public Builder withIndex(int index) {
            this.index = index;
            return this;
        }

        public CollectorWriterClient build() {

            return new CollectorWriterClient(
               this.collectorHostPort,
               this.type,
               this.sizeToFlush,
               this.timeToFlushInSecs,
               this.flushFrequencyInMs,
               this.blockingQueueSizeMax,
               this.index
            );

        }

        public static Builder create() {
            return new Builder();
        }
    }
}
