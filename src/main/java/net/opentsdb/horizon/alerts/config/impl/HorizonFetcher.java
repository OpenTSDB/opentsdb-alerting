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
import net.opentsdb.horizon.alerts.http.AlertHttpsClient;
import net.opentsdb.horizon.alerts.http.AuthProviders;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * For fetching a config of type C from horizon.
 * Final cause builders with inheritance are broken.
 * @param <C>
 */
@Getter
@Slf4j
public final class HorizonFetcher<C> {

    private final AlertHttpsClient alertHttpsClient;

    private final String configUrl;

    private String namespaceFetchPath = "api/v1/namespace";

    private final String namespaceUrl;

    private Map<Long, C> fetchedConfigs = new ConcurrentHashMap<>();

    private final int mirrorId;

    private boolean first = true;

    private final boolean inTestEnv;

    private final Set<String> namespacesFromResponse = new HashSet<>();

    private final Map<String,String> qParams;

    private final Function<String, C> stringCFunction;

    private final Function<C,Long> cLongFunction;

    private ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    private HorizonFetcher(Builder builder) {
        alertHttpsClient = new AlertHttpsClient();

        alertHttpsClient.setAuthProvider(
                AuthProviders.getAuthProvider(builder.authProvider));

        configUrl = AlertUtils.getURL(builder.endpoint, "api/v1/namespace/%s/"+builder.fetchPath);
        namespaceUrl = AlertUtils.getURL(builder.endpoint, namespaceFetchPath);
        mirrorId = builder.mirrorId;
        inTestEnv = builder.inTestEnv;
        qParams = Collections.unmodifiableMap(builder.qParams);
        stringCFunction = builder.stringCFunction;
        cLongFunction = builder.cLongFunction;
    }

    public synchronized void init() {
        if(first) {
            final long epochSecond = Instant.now().getEpochSecond();
            this.run();
            final long epochSecondEnd = Instant.now().getEpochSecond();
            log.info("First config run ended: time: "+ (epochSecondEnd - epochSecond));
            final ScheduledFuture<?> scheduledFuture = service.
                    scheduleAtFixedRate(this::run, 30, 30, TimeUnit.SECONDS);
            first = false;
        }
    }

    public Map<Long,C> getConfig() {

        return Collections.unmodifiableMap(new HashMap<>(fetchedConfigs));
    }

    public void run() {
        List<String> namespaceList = getNamespaceList();
        log.debug("Fetching configs from DB namespace list : "+ namespaceList);
        if(!namespaceList.isEmpty()) {
            namespacesFromResponse.addAll(namespaceList);
        } else {
            return;
        }

        fetchConfig(namespaceList);
    }

    private void fetchConfig(List<String> namespaceList) {

        final Map<Long, C> collect = namespaceList.stream()
                .map(namespace -> getConfigFromService(String.format(configUrl, namespace)))
                .filter(list -> !list.isEmpty())
                .flatMap(list -> list.stream())
                .filter(Objects::nonNull)
                .filter(str -> !str.isEmpty())
                .map(stringCFunction).filter(Objects::nonNull)
                .collect(Collectors.toMap(cLongFunction, Function.identity()));

        if(!collect.isEmpty()) {
            fetchedConfigs = collect;
        }

    }

    private List<String> getConfigFromService(String configUrl) {
        final List<String> config = new ArrayList<>();

        try {
            log.debug("Running "+  configUrl);
            final URIBuilder uriBuilder = new URIBuilder(configUrl);
            qParams.entrySet()
                    .forEach(entry -> uriBuilder.addParameter(entry.getKey(), entry.getValue()));
            HttpGet alertsHttpGet = new HttpGet(uriBuilder.build());
            alertsHttpGet.addHeader("accept","application/json");
            long start = System.nanoTime();
            CloseableHttpResponse alertExecute = alertHttpsClient.execute(alertsHttpGet);
            long end = System.nanoTime();
            Monitoring.get().timeConfigFetchTime(end-start,mirrorId);
            String response = EntityUtils.toString(alertExecute.getEntity());
            if(alertExecute.getStatusLine().getStatusCode() == 200) {
                //log.info("response: "+ alertResponse);
                JsonNode root = AlertUtils.parseJsonTree(response);

                root.elements()
                        .forEachRemaining(node -> config.add(node.toString()));
            } else {
                log.error("Got error response fetching  " +
                        "from config DB, Status Code: " + " url : "+ configUrl
                        +alertExecute.getStatusLine().getStatusCode()
                        + '\n' + response);
            }
        } catch (Exception e) {
            log.error("Error fetching from config DB:"+ " url : "+ configUrl,e);

        }
        if(config.size() != 0 ) {
            log.info("URL : {} Returned config : {}", configUrl, config.size());
        }
        return config;
    }

    public List<String> getNamespaceList() {

        if(inTestEnv) {
            List<String> sts = new ArrayList<>();
            sts.add("NS");
            return sts;
        }
        HttpGet namespaceHttpGet = new HttpGet(namespaceUrl);
        namespaceHttpGet.addHeader("accept","application/json");
        List<String> namespaces = new ArrayList<>();
        try {
            CloseableHttpResponse namespaceExecute = alertHttpsClient.execute(namespaceHttpGet);
            String response = EntityUtils.toString(namespaceExecute.getEntity());
            if(namespaceExecute.getStatusLine().getStatusCode() == 200) {
                namespaces = AlertUtils.getNamespacesFromResponse(response);

                if(!namespaces.isEmpty()) {
                    log.info("Fetched namespaces: "+namespaces);
                }

            } else {
                log.error("Error fetching namespaces: "
                        +namespaceExecute.getStatusLine().getStatusCode() +'\n'+response);
                log.error("Working with: "+namespacesFromResponse);
            }

        } catch (Exception e) {
            log.error("Error fetching namespaces " +
                    "from config DB: "+namespaceUrl,e);
        }

        return namespaces;
    }

    public static final class Builder<C> {

        private String fetchPath ;

        private String authProvider;

        private int mirrorId;

        private String endpoint;

        private boolean inTestEnv;

        private Function<String, C> stringCFunction;

        private Function<C,Long> cLongFunction;


        private Map<String,String> qParams = new HashMap<>();

        public Builder<C> withFetchPath(String fetchPath) {
            this.fetchPath = fetchPath;
            return this;
        }

        public Builder<C> withAuthProvider(String authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public Builder<C> withMirrorId(int mirrorId) {
            this.mirrorId = mirrorId;
            return this;
        }

        public Builder<C> withEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder<C> inTest() {
            this.inTestEnv = true;
            return this;
        }

        public Builder<C> configFromString(Function<String, C> stringCFunction) {
            this.stringCFunction = stringCFunction;
            return this;
        }

        public Builder<C> idFromConfig(Function<C, Long> cLongFunction) {
            this.cLongFunction = cLongFunction;
            return this;
        }

        public HorizonFetcher<C> build() {

            return new HorizonFetcher<>(this);
        }
    }

}
