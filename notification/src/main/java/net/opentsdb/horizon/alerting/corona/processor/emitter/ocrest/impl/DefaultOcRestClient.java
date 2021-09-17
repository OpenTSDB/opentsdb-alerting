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

package net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.OcRestClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.OcRestEvent;
import net.opentsdb.horizon.alerting.corona.processor.emitter.ocrest.OcRestSendFailedException;
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
import java.util.Objects;
import java.util.function.Supplier;

public class DefaultOcRestClient implements OcRestClient {

    private static final Logger LOG =
            LoggerFactory.getLogger(DefaultOcRestClient.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CloseableHttpClient client;

    private final String endpoint;

    private final Supplier<String> authTokenProvider;

    public DefaultOcRestClient(final DefaultBuilder builder)
    {
        Objects.requireNonNull(builder.client, "client cannot be null");
        Objects.requireNonNull(builder.endpoint, "endpoint cannot be null");
        Objects.requireNonNull(builder.authTokenProvider, "authTokenProvider cannot be null");
        this.client = builder.client;
        this.endpoint = builder.endpoint;
        this.authTokenProvider = builder.authTokenProvider;
    }

    @Override
    public void send(final OcRestEvent event)
    {
        final HttpPost request = buildRequest(event);
        doSend(request);
    }

    private HttpPost buildRequest(final OcRestEvent event)
    {
        final HttpPost request = new HttpPost(endpoint);
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.addHeader("auth_token", authTokenProvider.get());

        try {
            final String jsonBody = MAPPER.writeValueAsString(event);
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
                throw new OcRestSendFailedException("[" + statusCode + "] " + body);
            }
        } catch (IOException e) {
            throw new OcRestSendFailedException(e);
        }
    }

    public interface Builder extends OcRestClient.Builder<Builder> {

        Builder setHttpClient(CloseableHttpClient httpClient);

        Builder setMoogEndpoint(String endpoint);

        Builder setAuthTokenProvider(Supplier<String> authTokenProvider);

    }

    private static final class DefaultBuilder implements Builder {

        private CloseableHttpClient client;
        private String endpoint;
        private Supplier<String> authTokenProvider;

        @Override
        public Builder setHttpClient(CloseableHttpClient httpClient)
        {
            this.client = httpClient;
            return this;
        }

        @Override
        public Builder setMoogEndpoint(String endpoint)
        {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public Builder setAuthTokenProvider(Supplier<String> authTokenProvider)
        {
            this.authTokenProvider = authTokenProvider;
            return this;
        }

        @Override
        public OcRestClient build()
        {
            return new DefaultOcRestClient(this);
        }
    }

    public static Builder builder()
    {
        return new DefaultBuilder();
    }
}
