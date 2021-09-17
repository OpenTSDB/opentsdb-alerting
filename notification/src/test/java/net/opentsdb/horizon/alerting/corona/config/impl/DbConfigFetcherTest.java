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

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import net.opentsdb.horizon.alerting.corona.model.Parser;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.AbstractAlertConfig;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.Notification;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.Recipient;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.impl.NAlertConfig;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.impl.NAlertConfigListParser;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact.Type;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.ContactsParser;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.EmailContact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.WebhookContact;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.namespace.NamespaceListParser;
import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbConfigFetcherTest {

    final static String BASE_URL = "http://configdb.url";

    @Injectable
    Parser<List<NAlertConfig>> configListParser = new NAlertConfigListParser();

    @Injectable
    Parser<Contacts> contactsParser = new ContactsParser();

    @Injectable
    Parser<List<String>> namespacListParser = new NamespaceListParser();

    @Injectable
    CloseableHttpClient client;

    @Injectable
    String baseUrl = BASE_URL;

    @Test
    public void testGetURL()
    {
        final String url = "http://opentsdb.net/index?p=1";
        assertEquals(url, DbConfigFetcher.getURL(url).toString());
    }

    @Test
    public void testGetURI()
    {
        final String urlStr = "http://opentsdb.net:8080/index/?p=1";
        final URL url = DbConfigFetcher.getURL(urlStr);
        final String path = "/namespace/4";

        assertEquals("http://opentsdb.net:8080/namespace/4",
                DbConfigFetcher.getURI(url, path).toString());
    }

    @Test
    public void testGetURIWithQuery()
    {
        final String urlStr = "http://opentsdb.net:8080/index/?p=1";
        final URL url = DbConfigFetcher.getURL(urlStr);
        final String path = "/namespace/4";

        assertEquals(
                "http://opentsdb.net:8080/namespace/4?definition=true&query=simple",
                DbConfigFetcher
                        .getURI(
                                url, path,
                                "definition", "true",
                                "query", "simple"
                        )
                        .toString()
        );

        assertThrows(IllegalArgumentException.class, () -> {
            DbConfigFetcher.getURI(url, path, "definition");
        });

    }

    private DbConfigFetcher<NAlertConfig> getTestedInstance()
    {
        return DbConfigFetcher.<NAlertConfig>builder()
                .setConfigListParser(configListParser)
                .setContactsParser(contactsParser)
                .setNamespaceListParser(namespacListParser)
                .setClient(client)
                .setBaseUrl(baseUrl)
                .build();
    }

    private void assertHeaders(Header[] headers)
    {
        assertEquals(1, headers.length);
        assertEquals("Accept", headers[0].getName());
        assertEquals("application/json", headers[0].getValue());
    }

    @Test
    public void testGetNamespaces(
            @Injectable CloseableHttpResponse httpResponse,
            @Injectable StatusLine statusLine)
            throws IOException
    {
        new Expectations(client) {{
            client.execute((HttpGet) any);
            times = 1;

            httpResponse.getEntity();
            result = new StringEntity(loadResource("payloads/namespaces.json"));
            times = 1;

            statusLine.getStatusCode();
            result = 200;
        }};

        final Set<String> expected = new HashSet<>(Arrays.asList(
                "ns1", "ns2"
        ));

        // Test
        final Optional<List<String>> namespaces =
                getTestedInstance().getNamespaces();

        assertTrue(namespaces.isPresent());
        assertEquals(expected, new HashSet<>(namespaces.get()));

        new Verifications() {{
            final List<HttpGet> gets = new ArrayList<>();
            client.execute(withCapture(gets));

            assertEquals(1, gets.size());

            final HttpGet get = gets.get(0);
            assertEquals(
                    BASE_URL + "/api/v1/namespace",
                    get.getURI().toString());
            assertHeaders(get.getAllHeaders());
        }};
    }

    public String loadResource(final String name)
    {
        final String filepath = getClass().getClassLoader()
                .getResource(name)
                .getPath();
        try {
            return new String(Files.readAllBytes(Paths.get(filepath)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetContacts(
            @Mocked CloseableHttpResponse httpResponse,
            @Mocked StatusLine statusLine)
            throws IOException
    {
        final Contacts expected = Contacts.builder()
                .setEmailContacts(
                        EmailContact.builder()
                                .setName("bob@opentsdb.net")
                                .setEmail("bob@opentsdb.net")
                                .build(),
                        EmailContact.builder()
                                .setName("bar@opentsdb.net")
                                .setEmail("bar@opentsdb.net")
                                .build(),
                        EmailContact.builder()
                                .setName("foo@opentsdb.net")
                                .setEmail("foo@opentsdb.net")
                                .build()
                )
                .setWebhookContacts(
                        WebhookContact.builder()
                                .setName("ns http endpoint")
                                .setEndpoint("http://webhook.opentsdb.net")
                                .build()
                )
                .build();

        new Expectations(client) {{
            client.execute((HttpGet) any);
            times = 1;

            httpResponse.getEntity();
            result = new StringEntity(loadResource("payloads/contacts.json"));
            times = 1;

            statusLine.getStatusCode();
            result = 200;
        }};

        // Test
        final Optional<Contacts> contacts =
                getTestedInstance().getContacts("NS");

        assertTrue(contacts.isPresent());
        assertEquals(expected, contacts.get());

        new Verifications() {{
            final List<HttpGet> gets = new ArrayList<>();
            client.execute(withCapture(gets));

            assertEquals(1, gets.size());

            final HttpGet post = gets.get(0);
            assertEquals(
                    BASE_URL + "/api/v1/namespace/NS/contact",
                    post.getURI().toString()
            );
            assertHeaders(post.getAllHeaders());
        }};
    }

    @Test
    public void testGetAlertConfigs(
            @Mocked CloseableHttpResponse httpResponse,
            @Mocked StatusLine statusLine)
            throws IOException
    {
        new Expectations(client) {{
            client.execute((HttpGet) any);
            times = 1;

            httpResponse.getEntity();
            result = new StringEntity(loadResource("payloads/alertconfigs.json"));
            times = 1;

            statusLine.getStatusCode();
            result = 200;
        }};

        final NAlertConfig expected = NAlertConfig.builder()
                .setId(1)
                .setName("test 1")
                .setNamespace("NS")
                .setType(AbstractAlertConfig.Type.SIMPLE)
                .setEnabled(true)
                .setLabels(Arrays.asList("prod", "us-west-1"))
                .setGroupingRules(Arrays.asList("colo", "host"))
                .setNotification(Notification.builder()
                        .setBody("test")
                        .setSubject("test")
                        .setOcSeverity(OcSeverity.SEV_5)
                        .setOpsGeniePriority("")
                        .setRunbookId("")
                        .setTransitionsToNotify(Collections.singletonList("goodToBad"))
                        .setRecipients(
                                new HashMap<Type, List<Recipient>>() {{
                                    put(Type.EMAIL, Arrays.asList(
                                            new Recipient(0, "bob@opentsdb.net"))
                                    );
                                }}
                        )
                        .build())
                .build();

        // Test
        final Optional<List<NAlertConfig>> alertConfigs =
                getTestedInstance().getAlertConfigs("NS");

        assertTrue(alertConfigs.isPresent());
        assertEquals(alertConfigs.get().size(), 1);

        final NAlertConfig actual = alertConfigs.get().get(0);
        assertEquals(expected, actual);

        new Verifications() {{
            final List<HttpGet> gets = new ArrayList<>();
            client.execute(withCapture(gets));

            assertEquals(1, gets.size());

            final HttpGet post = gets.get(0);
            assertEquals(
                    BASE_URL + "/api/v1/namespace/NS/alert?definition=true",
                    post.getURI().toString());
            assertHeaders(post.getAllHeaders());
        }};
    }

    @Test
    public void testClose() throws IOException
    {
        // Test
        getTestedInstance().close();

        new Verifications() {{
            client.close();
            times = 1;
        }};
    }

    @Test
    public void testBuilder()
    {
        final Parser<List<NAlertConfig>> expConfigListParser =
                this.configListParser;
        final Parser<List<String>> expNamespaceListParser =
                this.namespacListParser;
        final Parser<Contacts> expContactMapParser = this.contactsParser;
        final CloseableHttpClient expClient = this.client;

        new MockUp<DbConfigFetcher<NAlertConfig>>() {
            @Mock
            public void $init(
                    Parser<List<NAlertConfig>> configListParser,
                    Parser<List<String>> namespaceListParser,
                    Parser<Map<Type, List<? extends Contact>>> contactMapParser,
                    CloseableHttpClient client,
                    String baseUrl)
            {
                assertEquals(expConfigListParser, configListParser);
                assertEquals(expNamespaceListParser, namespaceListParser);
                assertEquals(expContactMapParser, contactMapParser);
                assertEquals(expClient, client);
                assertEquals(BASE_URL, baseUrl);
            }
        };

        // Test
        DbConfigFetcher.<NAlertConfig>builder()
                .setConfigListParser(configListParser)
                .setNamespaceListParser(namespacListParser)
                .setContactsParser(contactsParser)
                .setBaseUrl(BASE_URL)
                .setClient(client)
                .build();
    }
}