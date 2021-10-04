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

package net.opentsdb.horizon.alerting.corona.processor.emitter.prism.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.emitter.prism.PrismClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.prism.PrismSendFailedException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

public class DefaultPrismClient implements PrismClient {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultPrismClient.class);
    private static final int DEFAULT_REQUEST_RETRY_TIMES = 3;
    private static final long BACKOFF_BASE_MS = 50L;

    private static boolean shouldRetryStatusCode(int statusCode) {
        switch (statusCode) {
            case HttpStatus.SC_REQUEST_TIMEOUT:
            case 429 /* Too Many Requests */:
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
            case HttpStatus.SC_BAD_GATEWAY:
            case HttpStatus.SC_SERVICE_UNAVAILABLE:
            case HttpStatus.SC_GATEWAY_TIMEOUT:
                return true;
        }
        return false;
    }

    private final CloseableHttpClient client;
    private final String endpoint;
    private final int requestRetryTimes;

    public DefaultPrismClient(final DefaultBuilder builder) {
        Objects.requireNonNull(builder.client, "client cannot be null");
        Objects.requireNonNull(builder.endpoint, "endpoint cannot be null");
        this.client = builder.client;
        this.endpoint = builder.endpoint;
        this.requestRetryTimes = builder.requestRetryTimes <= 0 ?
                DEFAULT_REQUEST_RETRY_TIMES : builder.requestRetryTimes;
    }

    @Override
    public void send(final String payload) {
        final long requestId = Instant.now().getEpochSecond();
        LOG.debug("Request; request_id={}, payload=<<{}>>", requestId, payload);

        sendWithRetries(requestId, payload, requestRetryTimes);
    }

    void sendWithRetries(final long requestId, final String payload, final int retryCount) {
        for (int i = 0; i <= retryCount; ++i) {
            final boolean lastAttempt = i == retryCount;
            final HttpPost request = buildRequest(payload);
            try (CloseableHttpResponse response = client.execute(request)) {
                final int statusCode = response.getStatusLine().getStatusCode();
                if (lastAttempt || !shouldRetryStatusCode(statusCode)) {
                    final String body = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                    LOG.debug("Response: request_id={}, status_code={}, body={}", requestId, statusCode, body);
                    if (statusCode / 100 != 2) {
                        throw new PrismSendFailedException("[" + statusCode + "] " + body);
                    }
                    return;
                }
            } catch (IOException e) {
                if (lastAttempt) {
                    // This was our last attempt.
                    throw new PrismSendFailedException(e);
                }
            }
            try {
                Thread.sleep(BACKOFF_BASE_MS * (long) Math.pow(2, i));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
            }
        }
    }

    private HttpPost buildRequest(final String payload) {
        final HttpPost request = new HttpPost(endpoint);
        try {
            request.setEntity(new StringEntity(payload));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return request;
    }

    public interface Builder extends PrismClient.Builder<Builder> {

        Builder setHttpClient(CloseableHttpClient httpClient);

        Builder setEndpoint(String endpoint);

        Builder setRequestRetryTimes(int requestRetryTimes);

    }

    private static final class DefaultBuilder implements Builder {

        private CloseableHttpClient client;
        private String endpoint;
        private int requestRetryTimes;

        @Override
        public Builder setHttpClient(CloseableHttpClient httpClient) {
            this.client = httpClient;
            return this;
        }

        @Override
        public Builder setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public Builder setRequestRetryTimes(int requestRetryTimes) {
            this.requestRetryTimes = requestRetryTimes;
            return this;
        }

        @Override
        public PrismClient build() {
            return new DefaultPrismClient(this);
        }
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }
}
