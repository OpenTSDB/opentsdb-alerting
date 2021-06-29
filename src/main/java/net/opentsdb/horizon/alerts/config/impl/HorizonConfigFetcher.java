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

import com.fasterxml.jackson.databind.JsonNode;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.Monitoring;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.AlertConfigFetcher;
import net.opentsdb.horizon.alerts.config.NamespaceFetcher;
import net.opentsdb.horizon.alerts.http.AlertHttpsClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TODO: Use the refactored class HorizonFetcher.
 */
public class HorizonConfigFetcher implements AlertConfigFetcher {

    private final AlertHttpsClient alertHttpsClient;

    private final String alertUrl;

    private final NamespaceFetcher namespaceFetcher;

    private final int mirrorid;

    private String alertFetchPath = "api/v1/namespace/%s/alert";

    private Map<Long, AlertConfig> fetchedConfigs = new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(HorizonConfigFetcher.class);

    private boolean first = true;


    private ScheduledExecutorService service = Executors.newScheduledThreadPool(1);


    private HorizonConfigFetcher(final NamespaceFetcher namespaceFetcher,
                         final AlertHttpsClient alertHttpsClient,
                         final String endpoint,
                         final int mirrorid) {
        this.alertHttpsClient = alertHttpsClient;

        //this.alertHttpsClient.setAuthProvider(AuthProviders.getAuthProvider(environmentConfig.getConfigDbAuthProvider()));
        this.namespaceFetcher = namespaceFetcher;
        this.mirrorid = mirrorid;

        alertUrl= AlertUtils.getURL(endpoint,alertFetchPath);
        //namespaceUrl  = AlertUtils.getURL(environmentConfig.getConfigDbEndpoint(),namespaceFetchPath);
    }

    @Override
    public Map<Long, AlertConfig> getAlertConfig() {

        LOG.info("Fetching configs from DB");

        if(first) {
            this.run();
            final ScheduledFuture<?> scheduledFuture = service.scheduleAtFixedRate(this::run, 30, 30, TimeUnit.SECONDS);
            first = false;
        }

        return Collections.unmodifiableMap(new HashMap(fetchedConfigs));
    }

    public void run() {
        List<String> namespaceList = namespaceFetcher.getNamespaces();
        LOG.debug("Fetching configs from DB namespace list : {}", namespaceList);
        if(namespaceList.isEmpty()) {
            LOG.error("Received no namespaces for config fetch");
            return;
        }

        final Map<Long, AlertConfig> collect = namespaceList.stream()
                .map(namespace -> getAlerts(String.format(alertUrl, namespace)))
                .filter(list -> !list.isEmpty())
                .flatMap(list -> list.stream())
                .filter(Objects::nonNull)
                .filter(str -> !str.isEmpty())
                .map(response -> {
                    LOG.debug("Alert Response : "+response);
                    try {
                        final AlertConfig alertConfig = AlertUtils.loadConfig(response);
                        if (alertConfig.isValid()) {
                            return alertConfig;
                        } else {
                            LOG.error("Invalid alert config received ID: " + alertConfig.getAlertId());
                        }
                        return alertConfig;
                    } catch (Throwable e) {
                        LOG.info("Error parsing alert config: " + response, e);
                    }
                    return null;
                }).filter(Objects::nonNull)
                //.filter(alertConfig -> alertConfig.getAlertId() == 339 || alertConfig.getAlertId() == 340 )
                .collect(Collectors.toMap(AlertConfig::getAlertId, Function.identity()));

        if(!collect.isEmpty()) {
            fetchedConfigs = collect;
        }
        LOG.debug("Fetched the alert configs: "+ collect);
    }

    public List<String> getAlerts(String alertUrl) {
        final List<String> alerts = new ArrayList<>();

        try {
            URIBuilder uriBuilder = new URIBuilder(alertUrl);
            uriBuilder.addParameter("definition","true");
            HttpGet alertsHttpGet = new HttpGet(uriBuilder.build());
            alertsHttpGet.addHeader("accept","application/json");
            long start = System.nanoTime();
            CloseableHttpResponse alertExecute = alertHttpsClient.execute(alertsHttpGet);
            long end = System.nanoTime();
            Monitoring.get().timeConfigFetchTime(end-start,mirrorid);
            String alertResponse = EntityUtils.toString(alertExecute.getEntity());
            if(alertExecute.getStatusLine().getStatusCode() == 200) {
                //LOG.info("response: "+ alertResponse);
                JsonNode root = AlertUtils.parseJsonTree(alertResponse);

                root.elements()
                        .forEachRemaining(node -> alerts.add(node.toString()));
            } else {
                LOG.error("Got error response fetching alerts " +
                        "from config DB, Status Code: "
                        +alertExecute.getStatusLine().getStatusCode()
                + '\n' + alertResponse);
            }
        } catch (Exception e) {
            LOG.error("Error fetching alerts from config DB:",e);

        }
        if(alerts.size() != 0 ) {
            LOG.info("Alert URL : {} Returned alerts : {}", alertUrl, alerts.size());
        }
        return alerts;
    }

    public static class Builder {

        private AlertHttpsClient _alertHttpsClient;

        private String _endpoint;

        private NamespaceFetcher _namespaceFetcher;

        private int _mirrorid;

        public Builder(){

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

        public Builder withNamespaceFetcher(final NamespaceFetcher namespaceFetcher) {
            this._namespaceFetcher = namespaceFetcher;
            return this;
        }

        public Builder withMirrorId(final int mirrorId) {
            this._mirrorid = mirrorId;
            return this;
        }

        // TODO: inform relationships
        public HorizonConfigFetcher build() {

            return new HorizonConfigFetcher(
                    this._namespaceFetcher,
                    this._alertHttpsClient,
                    this._endpoint,
                    this._mirrorid
            );

        }
    }

    public static Builder builder() {
        return Builder.create();
    }
}
