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

package net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.WebhookClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.WebhookEvent;
import net.opentsdb.horizon.alerting.corona.processor.emitter.webhook.WebhookFailedException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class DefaultWebhookClient implements WebhookClient {

    private static final Logger LOG =
            LoggerFactory.getLogger(DefaultWebhookClient.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CloseableHttpClient client;

    public DefaultWebhookClient(final DefaultBuilder builder)
    {
        Objects.requireNonNull(builder.client, "client cannot be null");
        this.client = builder.client;
    }

    @Override
    public void send(final List<WebhookEvent> events, String endpoint)
    {
        final HttpPost request = buildRequest(events, endpoint);
        doSend(request);
    }

    private HttpPost buildRequest(final List<WebhookEvent> events, String endpoint)
    {
        final HttpPost request = new HttpPost(endpoint);
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        try {
            final String jsonBody = MAPPER.writeValueAsString(events);
            LOG.debug("Request: payload={}", jsonBody);
            request.setEntity(new StringEntity(jsonBody));
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return request;
    }

    private void doSend(final HttpPost post)
    {
        try (CloseableHttpResponse response = client.execute(post)) {
            final int statusCode = response.getStatusLine().getStatusCode();
            final String body = IOUtils.toString(
                    response.getEntity().getContent(),
                    StandardCharsets.UTF_8
            );
            LOG.debug("Response: status_code={}, body={}", statusCode, body);

            if (statusCode != HttpStatus.SC_OK) {
                throw new WebhookFailedException("[" + statusCode + "] " + body);
            }
        } catch (IOException e) {
            throw new WebhookFailedException(e);
        }
    }

    public interface Builder extends WebhookClient.Builder<Builder> {
        Builder setHttpClient(CloseableHttpClient httpClient);
    }

    private static final class DefaultBuilder implements Builder {
        private CloseableHttpClient client;

        @Override
        public Builder setHttpClient(CloseableHttpClient httpClient)
        {
            this.client = httpClient;
            return this;
        }

        @Override
        public WebhookClient build()
        {
            return new DefaultWebhookClient(this);
        }
    }

    public static Builder builder()
    {
        return new DefaultBuilder();
    }
}

