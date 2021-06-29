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

package net.opentsdb.horizon.alerts.config.impl;

import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.config.NamespaceFetcher;
import net.opentsdb.horizon.alerts.http.AlertHttpsClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HorizonNamespaceFetcher implements NamespaceFetcher {

    private String namespaceFetchPath = "api/v1/namespace";
    private final String namespaceFetchUrl;

    private final AlertHttpsClient httpsClient;

    // Cache namespace list
    private final List<String> cachedNamespaces = new ArrayList<>();

    private static final Logger LOG = LoggerFactory.getLogger(HorizonNamespaceFetcher.class);

    private HorizonNamespaceFetcher(final AlertHttpsClient alertHttpsClient,
                                   final String endpoint) {
        this.httpsClient = alertHttpsClient;
        this.namespaceFetchUrl = AlertUtils.getURL(endpoint,namespaceFetchPath);

    }

    @Override
    public List<String> getNamespaces() {
        HttpGet namespaceHttpGet = new HttpGet(namespaceFetchUrl);
        namespaceHttpGet.addHeader("accept","application/json");
        try {
            CloseableHttpResponse namespaceExecute = httpsClient.execute(namespaceHttpGet);
            String response = EntityUtils.toString(namespaceExecute.getEntity());
            if(namespaceExecute.getStatusLine().getStatusCode() == 200) {
                final List<String> namespaces = AlertUtils.getNamespacesFromResponse(response);

                if(!namespaces.isEmpty()) {
                    LOG.info("Fetched namespaces: "+namespaces);
                    cachedNamespaces.clear();
                    cachedNamespaces.addAll(namespaces);
                }

            } else {
                LOG.error("Error fetching namespaces: "
                        +namespaceExecute.getStatusLine().getStatusCode() +'\n'+response);
                LOG.error("Working with: "+cachedNamespaces);
            }

        } catch (Exception e) {
            LOG.error("Error fetching namespaces " +
                    "from config DB: "+namespaceFetchUrl,e);
        }
        LOG.info("Returning {} namespaces", cachedNamespaces.size());
        return Collections.unmodifiableList(cachedNamespaces);
    }

    public static class Builder {

        private AlertHttpsClient _alertHttpsClient;

        private String _endpoint;

        private Builder () {

        }

        public static Builder create() {
            return new Builder();
        }

        public Builder withClient(final AlertHttpsClient alertHttpsClient) {
            this._alertHttpsClient = alertHttpsClient;
            return this;
        }

        public Builder withEndpoint(final String endpoint) {
            this._endpoint = endpoint;
            return this;
        }

        public HorizonNamespaceFetcher build() {
            return new HorizonNamespaceFetcher(this._alertHttpsClient,
                    this._endpoint);
        }
    }
}
