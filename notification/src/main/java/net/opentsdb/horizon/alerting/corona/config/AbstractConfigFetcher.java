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

package net.opentsdb.horizon.alerting.corona.config;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.opentsdb.horizon.alerting.corona.model.Parser;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.AbstractAlertConfig;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;

public abstract class AbstractConfigFetcher<C extends AbstractAlertConfig>
        implements ConfigFetcher<C>
{

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Parser<List<C>> alertConfigListParser;

    private final Parser<List<String>> namespaceListParser;

    private final Parser<Contacts> contactsParser;

    protected AbstractConfigFetcher(
            final Parser<List<C>> alertConfigListParser,
            final Parser<List<String>> namespaceListParser,
            final Parser<Contacts> contactsParser)
    {
        Objects.requireNonNull(
                alertConfigListParser, "alertConfigListParser cannot be null");
        Objects.requireNonNull(
                namespaceListParser, "namespaceListParser cannot be null");
        Objects.requireNonNull(
                contactsParser, "contactsParser cannot be null");

        this.alertConfigListParser = alertConfigListParser;
        this.namespaceListParser = namespaceListParser;
        this.contactsParser = contactsParser;
    }

    private JsonNode readTree(final InputStream is)
    {
        try {
            return OBJECT_MAPPER.readTree(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse namespaces from an input stream representing a JSON object.
     * <p>
     * The caller is responsible for closing the stream.
     *
     * @param is input stream to be parsed
     * @return list of namespaces
     */
    protected List<String> parseNamespaces(final InputStream is)
    {
        final JsonNode root = readTree(is);

        final List<String> namespaces = namespaceListParser.parse(root);
        return Collections.unmodifiableList(namespaces);
    }

    /**
     * Parse alert configurations from an input stream representing a
     * JSON object.
     * <p>
     * The caller is responsible for closing the stream.
     *
     * @param is input stream to be parsed
     * @return list of alert configurations
     */
    protected List<C> parseAlertConfigs(final InputStream is)
    {
        final JsonNode root = readTree(is);

        final List<C> alerts = alertConfigListParser.parse(root);
        return Collections.unmodifiableList(alerts);
    }

    /**
     * Parse contacts from an input stream representing a JSON object.
     * <p>
     * The caller is responsible for closing the stream.
     *
     * @param is input stream to be parsed
     * @return list of namespaces
     */
    protected Contacts parseContacts(final InputStream is)
    {
        final JsonNode root = readTree(is);

        return contactsParser.parse(root);
    }
}
