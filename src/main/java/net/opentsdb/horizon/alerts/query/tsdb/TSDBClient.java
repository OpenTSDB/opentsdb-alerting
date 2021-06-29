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

package net.opentsdb.horizon.alerts.query.tsdb;

import net.opentsdb.horizon.alerts.AlertException;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.http.AlertHttpsClient;
import net.opentsdb.horizon.alerts.http.AuthProviders;

import io.undertow.util.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class TSDBClient {

    private String tsdbHost = null;

    private String tsdbQueryPath = "/api/query/graph";

    private static final Logger LOG = LoggerFactory.getLogger(TSDBClient.class);

    private AlertHttpsClient alertHttpsClient = null;

    public TSDBClient(String tsdbHost, String authProvider) {
        this.tsdbHost = tsdbHost;
        alertHttpsClient = new AlertHttpsClient();
        alertHttpsClient.setAuthProvider(AuthProviders.getAuthProvider(authProvider));
    }

    public String getResponse(String query, String queryPath, long alertId) throws AlertException {
        LOG.debug("In get response");

        HttpPost httpPost = new HttpPost(AlertUtils.getURL(tsdbHost, queryPath));

        httpPost.setHeader("Content-type", "application/json");
        httpPost.setHeader("X-Corona-AlertId", Long.toString(alertId));

        StringEntity entity = new StringEntity(query, StandardCharsets.UTF_8.name());

        httpPost.setEntity(entity);
        try {
            LOG.debug("Sending query for client: " + AlertUtils.getURL(tsdbHost, queryPath));
            final CloseableHttpResponse execute = alertHttpsClient.execute(httpPost);

            final int statusCode = execute.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 204) {
                LOG.debug("Sent query for client: " + query);
                return EntityUtils.toString(execute.getEntity());
            }
            final String s = EntityUtils.toString(execute.getEntity());
            LOG.error("Response {} while running tsd query: {}", execute.getStatusLine().toString() + '\n' + s, query);
            throw new AlertException("Error running tsdb query: " + execute.getStatusLine().toString() + '\n' + s + '\n' +
                    " error for query : " + query);

        } catch (Exception e) {
            LOG.error("Exception running tsd query: ", e);
            throw new AlertException("Error running tsdb query", e);
        }
    }

    public String getResponse(String query, long alertId) throws AlertException {
        return getResponse(query, tsdbQueryPath, alertId);
    }


    public static void main(String args[]) {

        String file = "src/main/resources/proc.json";

        EnvironmentConfig environmentConfig = new EnvironmentConfig();

        String s;
        try {

            TSDBClient tsdbClient = new TSDBClient(environmentConfig.getTsdbEndpoint(), null);

            s = FileUtils.readFile(new FileInputStream(file));

            final String response = tsdbClient.getResponse(s, -1);

            LOG.info(response);
        } catch (Exception e) {
            LOG.error("TSDB exception: ", e);
        }


    }


}
