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

package net.opentsdb.horizon.alerting.corona.component.http;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Objects;

import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import net.opentsdb.horizon.alerting.Builder;
import org.apache.http.HttpHost;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

import com.oath.auth.KeyRefresher;
import com.oath.auth.Utils;

@NotThreadSafe
public class CloseableHttpClientBuilder
        implements Builder<CloseableHttpClientBuilder, CloseableHttpClient> {

    /* ----------- Constants ----------- */

    private static final String[] PROTOCOL = new String[]{"TLSv1.2"};

    private static final List NON_RETRIABLE_CLASSES =
            Collections.unmodifiableList(Arrays.asList(
                    ConnectException.class,
                    UnknownHostException.class,
                    SSLException.class
            ));

    /* ----------- Static Methods ----------- */

    public static CloseableHttpClientBuilder create()
    {
        return new CloseableHttpClientBuilder();
    }

    /* ----------- Fields ----------- */

    private String trustStorePath;

    private String trustStorePassword;

    private String certificatePath;

    private String privateKeyPath;

    private boolean tlsEnabled = true;

    private boolean insecureSkipVerify = false;

    private int retryMax = 3;

    private int maxConnTotal = 10;

    private int maxConnPerRoute = 5;

    private int connectTimeoutMs = 1_000;

    private int socketTimeoutMs = 500;

    private int connectionRequestTimeoutMs = 500;

    private HttpHost proxy;

    private CredentialsProvider credsProvider;

    /* ----------- Constructor ----------- */

    private CloseableHttpClientBuilder()
    {

    }

    /* ----------- Methods ----------- */

    public CloseableHttpClientBuilder setTrustStorePath(String path)
    {
        trustStorePath = path;
        return this;
    }

    public CloseableHttpClientBuilder setTrustStorePassword(String password)
    {
        trustStorePassword = password;
        return this;
    }

    public CloseableHttpClientBuilder setCertificatePath(String path)
    {
        certificatePath = path;
        return this;
    }

    public CloseableHttpClientBuilder setPrivateKeyPath(String path)
    {
        privateKeyPath = path;
        return this;
    }

    public CloseableHttpClientBuilder setTLSEnabled(boolean tlsEnabled)
    {
        this.tlsEnabled = tlsEnabled;
        return this;
    }

    public CloseableHttpClientBuilder setInsecureSkipVerify(boolean insecureSkipVerify)
    {
        this.insecureSkipVerify = insecureSkipVerify;
        return this;
    }

    public CloseableHttpClientBuilder setRetryMax(int retryMax)
    {
        this.retryMax = retryMax;
        return this;
    }

    public CloseableHttpClientBuilder setMaxConnTotal(int maxConnTotal)
    {
        this.maxConnTotal = maxConnTotal;
        return this;
    }

    public CloseableHttpClientBuilder setMaxConnPerRoute(int maxConnPerRoute)
    {
        this.maxConnPerRoute = maxConnPerRoute;
        return this;
    }

    public CloseableHttpClientBuilder setConnectTimeoutMs(int connectTimeoutMs)
    {
        this.connectTimeoutMs = connectTimeoutMs;
        return this;
    }

    public CloseableHttpClientBuilder setSocketTimeoutMs(int socketTimeoutMs)
    {
        this.socketTimeoutMs = socketTimeoutMs;
        return this;
    }

    public CloseableHttpClientBuilder setConnectionRequestTimeoutMs(
            int connectionRequestTimeoutMs)
    {
        this.connectionRequestTimeoutMs = connectionRequestTimeoutMs;
        return this;
    }

    public CloseableHttpClientBuilder setProxy(HttpHost proxyHost)
    {
        this.proxy = proxyHost;
        return this;
    }

    public CloseableHttpClientBuilder setProxy(String proxyHost, int proxyPort)
    {
        this.proxy = new HttpHost(proxyHost, proxyPort);
        return this;
    }

    public CloseableHttpClientBuilder setCredentialsProvider(CredentialsProvider credsProvider)
    {
        this.credsProvider = credsProvider;
        return this;
    }

    private void validate()
    {
        if (!tlsEnabled) {
            return;
        }
        Objects.requireNonNull(trustStorePath, "trustStorePath cannot be null");
        Objects.requireNonNull(trustStorePassword, "trustStorePassword cannot be null");
        Objects.requireNonNull(certificatePath, "certificatePath cannot be null");
        Objects.requireNonNull(privateKeyPath, "privateKeyPath cannot be null");
    }

    private SSLContext buildSSLContext()
    {
        final KeyRefresher keyRefresher;
        try {
            keyRefresher =
                    Utils.generateKeyRefresher(
                            trustStorePath,
                            trustStorePassword,
                            certificatePath,
                            privateKeyPath
                    );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        keyRefresher.startup();


        try {
            return Utils.buildSSLContext(
                    keyRefresher.getKeyManagerProxy(),
                    keyRefresher.getTrustManagerProxy()
            );
        } catch (Exception e) {
            keyRefresher.shutdown();
            throw new RuntimeException(e);
        }
    }

    private SSLConnectionSocketFactory buildSSLSocketFactory()
    {
        final SSLContext sslContext = buildSSLContext();
        final HostnameVerifier verifier = insecureSkipVerify
                ? new NoopHostnameVerifier()
                : null;

        return new SSLConnectionSocketFactory(
                sslContext,
                PROTOCOL,
                null,
                verifier
        );
    }

    public CloseableHttpClient build()
    {
        validate();

        @SuppressWarnings("unchecked")
        final DefaultHttpRequestRetryHandler retryHandler =
                new DefaultHttpRequestRetryHandler(
                        retryMax, true, NON_RETRIABLE_CLASSES
                ) {};

        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMs)
                .setSocketTimeout(socketTimeoutMs)
                .setConnectionRequestTimeout(connectionRequestTimeoutMs)
                .setProxy(proxy)
                .build();

        final SSLConnectionSocketFactory socketFactory =
                tlsEnabled ? buildSSLSocketFactory() : null;

        return org.apache.http.impl.client.HttpClientBuilder.create()
                .setRetryHandler(retryHandler)
                .setMaxConnTotal(maxConnTotal)
                .setMaxConnPerRoute(maxConnPerRoute)
                .setDefaultCredentialsProvider(credsProvider)
                .setDefaultRequestConfig(requestConfig)
                .setSSLSocketFactory(socketFactory)
                .build();
    }
}

