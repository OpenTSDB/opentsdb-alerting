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

import java.util.List;
import java.util.Optional;

import net.opentsdb.horizon.alerting.corona.model.alertconfig.AbstractAlertConfig;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.app.AlertProcessor;

/**
 * Provides very basic API based on the ConfigDB API. Implementations should
 * be dead simple with minimal functionality.
 * <p>
 * If the data cannot be retrieved for some reason, implementations must
 * return an empty optional. If the data is retrieved and empty, then an
 * optional with corresponding empty container must be returned.
 * <p>
 * TODO: Check if this still holds since 2019-04-12
 * All caching, validation logic should be done in the {@link MetadataProvider}
 * implementations, or built as a super class of a simple implementation.
 *
 * @param <C> type of alert configuration class which can be configured by
 *            the user. E.g. for the {@link AlertProcessor}
 *            only notification part is needed. Other implementations might
 *            require parsing of the TSDB query or threshold configuration.
 */
public interface ConfigFetcher<C extends AbstractAlertConfig> {

    /**
     * Get all namespaces.
     *
     * @return {@link Optional} with a list of namespaces; empty
     * {@link Optional} on error
     */
    Optional<List<String>> getNamespaces();

    /**
     * Get contacts for the given namespace.
     *
     * @param namespace namespace
     * @return {@link Optional} with contacts; empty {@link Optional} on error.
     */
    Optional<Contacts> getContacts(String namespace);

    /**
     * Get contacts for the given namespace and alert id
     *
     * @param namespace namespace
     * @param alertId   alert id
     * @return {@link Optional} with contacts; empty {@link Optional} on error.
     */
    Optional<Contacts> getContacts(String namespace, long alertId);

    /**
     * Get alert configurations for the given alert id.
     *
     * @param alertId alert id
     * @return {@link Optional} with an alert configuration; empty
     * {@link Optional} on error
     */
    Optional<C> getAlertConfig(long alertId);

    /**
     * Get alert configurations for the given namespace.
     *
     * @param namespace namespace
     * @return {@link Optional} with a list of alert configurations; empty
     * {@link Optional} on error
     */
    Optional<List<C>> getAlertConfigs(String namespace);

}
