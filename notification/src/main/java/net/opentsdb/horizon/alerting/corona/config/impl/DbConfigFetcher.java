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

package net.opentsdb.horizon.alerting.corona.config.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import net.opentsdb.horizon.alerting.corona.config.AbstractConfigFetcher;
import net.opentsdb.horizon.alerting.corona.model.Parser;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.AbstractAlertConfig;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.ContactsParser;
import net.opentsdb.horizon.alerting.corona.model.namespace.NamespaceListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class DbConfigFetcher<C extends AbstractAlertConfig>
        extends AbstractConfigFetcher<C>
        implements AutoCloseable
{

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(DbConfigFetcher.class);

    private static final String PATH_NAMESPACE = "api/v1/namespace";

    private static final String PATH_CONTACTS = "api/v1/namespace/%s/contact";

    private static final String PATH_ALERTS = "api/v1/namespace/%s/alert";

    /* ------------ Static Methods ------------ */

    public static URL getURL(String url)
    {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Bad url", e);
        }
    }

    public static URI getURI(URL url, String path)
    {
        try {
            return new URL(url, path).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static URI getURI(URL url, String path, String... parameters)
    {
        if (parameters.length % 2 != 0) {
            throw new IllegalArgumentException("Not even number of parameters");
        }

        try {
            URIBuilder builder = new URIBuilder(getURI(url, path));
            for (int i = 0; i < parameters.length / 2; i++) {
                builder.addParameter(parameters[2 * i], parameters[2 * i + 1]);
            }
            return builder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /* ------------ Fields ------------ */

    private final CloseableHttpClient client;

    private final URL baseUrl;

    /* ------------ Constructor ------------ */

    DbConfigFetcher(
            final Parser<List<C>> configListParser,
            final Parser<List<String>> namespaceListParser,
            final Parser<Contacts> contactsParser,
            final CloseableHttpClient client,
            final String baseUrl)
    {
        super(configListParser, namespaceListParser, contactsParser);

        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(baseUrl, "baseUrl cannot be null");

        this.client = client;
        this.baseUrl = getURL(baseUrl);
    }

    /* ------------ Methods ------------ */

    private <O> Optional<O> doGet(final HttpGet httpGet,
                                  final Function<InputStream, O> handler)
    {
        try (final CloseableHttpResponse resp = client.execute(httpGet)) {
            final int statusCode = resp.getStatusLine().getStatusCode();
            // TODO: Check all success codes from ConfigDB and revisit.
            if (statusCode == 200) {
                O toReturn = handler.apply(resp.getEntity().getContent());
                return Optional.ofNullable(toReturn);
            }
            LOG.error("GET: status_code={}, uri={}, body=<<{}>>",
                    statusCode,
                    httpGet.getURI(),
                    IOUtils.toString(
                            resp.getEntity().getContent(),
                            StandardCharsets.UTF_8
                    )
            );
        } catch (IOException e) {
            LOG.error("GET: uri={}", httpGet.getURI(), e);
        }
        return Optional.empty();
    }

    private HttpGet buildGetNamespacesRequest()
    {
        final HttpGet httpGet =
                new HttpGet(getURI(baseUrl, PATH_NAMESPACE));
        httpGet.addHeader("Accept", "application/json");

        return httpGet;
    }

    @Override
    public Optional<List<String>> getNamespaces()
    {
        final HttpGet httpGet = buildGetNamespacesRequest();
        return doGet(httpGet, this::parseNamespaces);
    }

    private HttpGet buildGetContactsRequest(final String namespace)
    {
        final String path = String.format(PATH_CONTACTS, namespace);

        final HttpGet httpGet = new HttpGet(getURI(baseUrl, path));
        httpGet.addHeader("Accept", "application/json");

        return httpGet;
    }

    @Override
    public Optional<Contacts> getContacts(final String namespace)
    {
        final HttpGet httpGet = buildGetContactsRequest(namespace);
        return doGet(httpGet, this::parseContacts);
    }

    @Override
    public Optional<Contacts> getContacts(final String namespace,
                                          final long alertId)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<C> getAlertConfig(final long alertId)
    {
        throw new UnsupportedOperationException();
    }

    private HttpGet buildGetAlertConfigsRequest(final String namespace)
    {
        final String path = String.format(PATH_ALERTS, namespace);

        final HttpGet httpGet = new HttpGet(
                getURI(baseUrl, path, "definition", "true")
        );
        httpGet.addHeader("Accept", "application/json");

        return httpGet;
    }

    @Override
    public Optional<List<C>> getAlertConfigs(final String namespace)
    {
        final HttpGet httpGet = buildGetAlertConfigsRequest(namespace);
        return doGet(httpGet, this::parseAlertConfigs);
    }

    @Override
    public void close() throws IOException
    {
        client.close();
    }

    /* ------------ Builder ------------ */

    public static class Builder<C extends AbstractAlertConfig> {

        private Parser<List<C>> configListParser;

        private Parser<List<String>> namespaceListParser;

        private Parser<Contacts> contactsParser;

        private CloseableHttpClient client;

        private String baseUrl;

        public Builder()
        {
            this.namespaceListParser = new NamespaceListParser();
            this.contactsParser = new ContactsParser();
        }

        public Builder<C> setConfigListParser(Parser<List<C>> configListParser)
        {
            this.configListParser = configListParser;
            return this;
        }

        public Builder<C> setNamespaceListParser(
                final Parser<List<String>> namespaceListParser)
        {
            this.namespaceListParser = namespaceListParser;
            return this;
        }

        public Builder<C> setContactsParser(
                final Parser<Contacts> contactsParser)
        {
            this.contactsParser = contactsParser;
            return this;
        }

        public Builder<C> setClient(CloseableHttpClient client)
        {
            this.client = client;
            return this;
        }

        public Builder<C> setBaseUrl(String baseUrl)
        {
            this.baseUrl = baseUrl;
            return this;
        }

        public DbConfigFetcher<C> build()
        {
            return new DbConfigFetcher<>(
                    configListParser,
                    namespaceListParser,
                    contactsParser,
                    client,
                    baseUrl
            );
        }
    }

    public static <C extends AbstractAlertConfig> Builder<C> builder()
    {
        return new Builder<>();
    }
}
