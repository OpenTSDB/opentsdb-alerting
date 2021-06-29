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

import net.opentsdb.horizon.alerts.AlertException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class AlertHttpsClient {

    private CloseableHttpClient closeableHttpClient = null;

    private static final int DEFAULT_TIMEOUT_MS = 60_000;

    private static final String TLS_V2 = "TLSv1.2";

    private static final Logger LOG = LoggerFactory.getLogger(AlertHttpsClient.class);

    private AuthProviderForAlertClient authProviderForAlertClient;

    private CloseableHttpClient createClient() throws Exception {

        final SSLConnectionSocketFactory factory;
        if(authProviderForAlertClient != null && authProviderForAlertClient.getSSLContext() != null) {

            factory = new SSLConnectionSocketFactory(authProviderForAlertClient.getSSLContext()
                    , new String[]{TLS_V2}, null,
                    (HostnameVerifier) null);

        } else {
            factory = null;
        }

        List<Class<? extends IOException>> nonRetriableClasses = Arrays.asList(ConnectException.class, UnknownHostException.class, SSLException.class);
        DefaultHttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(3, true, nonRetriableClasses) {};

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(DEFAULT_TIMEOUT_MS)
                .setSocketTimeout(DEFAULT_TIMEOUT_MS)
                .setConnectionRequestTimeout(DEFAULT_TIMEOUT_MS)
                .build();

        if(factory != null) {

            return HttpClientBuilder.create()
                    .setRetryHandler(retryHandler)
                    .setMaxConnTotal(1)
                    .setMaxConnPerRoute(1)
                    .setDefaultRequestConfig(requestConfig)
                    .setSSLSocketFactory(factory).build();
        } else {

            return HttpClientBuilder.create()
                    .setRetryHandler(retryHandler)
                    .setMaxConnTotal(1)
                    .setMaxConnPerRoute(1)
                    .setDefaultRequestConfig(requestConfig)
                    .build();
        }



    }

    public static AlertHttpsClient create(
            AuthProviderForAlertClient authProviderForAlertClient) {
         AlertHttpsClient alertHttpsClient = new AlertHttpsClient();
         alertHttpsClient.setAuthProvider(authProviderForAlertClient);
         return alertHttpsClient;
    }

    public void setAuthProvider(AuthProviderForAlertClient authProvider) {
        this.authProviderForAlertClient = authProvider;
    }

    public CloseableHttpResponse execute(HttpUriRequest httpPost) throws AlertException {

        if (closeableHttpClient == null) {
            try {
                closeableHttpClient = createClient();
                LOG.info("Created client");
            } catch (Exception e) {
                throw new AlertException("Error creating httpclient:", e);
            }
        }

        try {
            return closeableHttpClient.execute(httpPost);
        } catch (ClientProtocolException e) {
            LOG.error("Exception running http post: ", e);
            throw new AlertException("Error running http post", e);
        } catch (IOException e) {
            LOG.error("Exception running http post: ", e);
            throw new AlertException("Error running http post", e);
        }


    }

}
