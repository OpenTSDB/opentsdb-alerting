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

package net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.concurrent.ThreadSafe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackException;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackRequest;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

@ThreadSafe
public final class SlackClientImpl implements SlackClient {

    /* ------------ Constants ------------ */

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /* ------------ Fields ------------ */

    private final CloseableHttpClient client;

    /* ------------ Constructor ------------ */

    public SlackClientImpl(final CloseableHttpClient client)
    {
        Objects.requireNonNull(client, "client cannot be null");
        this.client = client;
    }

    /* ------------ Methods ------------ */

    @Override
    public SlackResponse send(final SlackRequest request,
                              final String endpoint)
    {
        final HttpPost httpReq = buildHttpRequest(request, endpoint);
        return doPost(httpReq)
                .map(SlackResponceImpl::error)
                .orElseGet(SlackResponceImpl::ok);
    }

    private HttpPost buildHttpRequest(final SlackRequest request,
                                      final String endpoint)
    {
        final HttpPost httpReq = new HttpPost(endpoint);
        httpReq.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        try {
            final String jsonBody = MAPPER.writeValueAsString(request);
            System.out.println(jsonBody);
            httpReq.setEntity(new StringEntity(jsonBody, "UTF-8"));
        } catch (JsonProcessingException e) {
            throw new SlackException("serialization failed", e);
        }

        return httpReq;
    }

    /**
     * https://api.slack.com/incoming-webhooks#handling_errors
     *
     * @param post POST request
     * @return optional containing error string on error, empty
     * on success.
     */
    private Optional<String> doPost(final HttpPost post)
    {
        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return Optional.empty();
            }

            final String errorMsg = IOUtils.toString(
                    response.getEntity().getContent(),
                    StandardCharsets.UTF_8
            );
            return Optional.of(errorMsg);
        } catch (IOException e) {
            throw new SlackException("read response failed", e);
        }
    }
}
