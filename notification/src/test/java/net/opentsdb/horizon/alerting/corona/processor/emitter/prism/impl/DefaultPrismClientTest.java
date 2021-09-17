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

import mockit.Expectations;
import mockit.Injectable;
import net.opentsdb.horizon.alerting.corona.processor.emitter.prism.PrismClient;
import net.opentsdb.horizon.alerting.corona.processor.emitter.prism.PrismSendFailedException;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultPrismClientTest {

    @Injectable
    CloseableHttpClient httpClient;

    @Injectable
    CloseableHttpResponse httpResponse;

    @Injectable
    StatusLine statusLine;

    @Test
    void retriesAreMadeOn6StatusCodes() throws IOException {
        PrismClient prismClient = DefaultPrismClient.builder()
                .setHttpClient(httpClient)
                .setEndpoint("localhost")
                .setRequestRetryTimes(7)
                .build();

            new Expectations() {{
                httpClient.execute((HttpUriRequest) any);
                times = 7;
                result = httpResponse;
                httpResponse.getEntity();
                result = new StringEntity("Response payload");
                httpResponse.getStatusLine();
                result = statusLine;
                statusLine.getStatusCode();
                returns(HttpStatus.SC_REQUEST_TIMEOUT,
                        429 /* Too Many Requests */,
                        HttpStatus.SC_INTERNAL_SERVER_ERROR,
                        HttpStatus.SC_BAD_GATEWAY,
                        HttpStatus.SC_SERVICE_UNAVAILABLE,
                        HttpStatus.SC_GATEWAY_TIMEOUT);
                result = 200;
            }};
        prismClient.send("test payload");
    }

    @Test
    void retriesAreNotMadeOnNonRetriableStatusCodes() throws IOException {
        PrismClient prismClient = DefaultPrismClient.builder()
                .setHttpClient(httpClient)
                .setEndpoint("localhost")
                .setRequestRetryTimes(7)
                .build();

        new Expectations() {{
            httpClient.execute((HttpUriRequest) any);
            times = 1;
            result = httpResponse;
            httpResponse.getEntity();
            result = new StringEntity("Response payload");
            httpResponse.getStatusLine();
            result = statusLine;
            statusLine.getStatusCode();
            result = HttpStatus.SC_BAD_REQUEST;
        }};
        try {
            prismClient.send("test payload");
        } catch (PrismSendFailedException e) {
            assertEquals("[400] Response payload", e.getMessage());
        }
    }

    @Test
    void exceptionIsThrownWhenWeGiveUpRetries() throws IOException {
        PrismClient prismClient = DefaultPrismClient.builder()
                .setHttpClient(httpClient)
                .setEndpoint("localhost")
                .setRequestRetryTimes(2)
                .build();

        new Expectations() {{
            httpClient.execute((HttpUriRequest) any);
            times = 3;
            result = httpResponse;
            httpResponse.getEntity();
            result = new StringEntity("Response payload");
            httpResponse.getStatusLine();
            result = statusLine;
            statusLine.getStatusCode();
            returns(HttpStatus.SC_REQUEST_TIMEOUT,
                    429 /* Too Many Requests */);
            result = HttpStatus.SC_GATEWAY_TIMEOUT;
        }};
        try {
            prismClient.send("test payload");
        } catch (PrismSendFailedException e) {
            assertEquals("[504] Response payload", e.getMessage());
        }
    }
}