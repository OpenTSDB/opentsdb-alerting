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

package net.opentsdb.horizon.alerts.heartbeat;

import com.google.gson.Gson;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.util.Methods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**To maintain mirror set parity **/

public class HeartbeatServer {

    private final String hostname;

    private final String path;

    private final String reqPath = "/alertconfigs";

    private final int port;

    private Collection<? extends HeartbeatReadable> heartbeatReadable = new ArrayList<>();

    public static final String OK = "OK";

    private Gson gson = new Gson();

    public HeartbeatServer(String hostname,int port,String path) {
        this.hostname = hostname;
        this.port = port;
        this.path = path;
    }

    public void setHeartbeatReadable(Collection<? extends HeartbeatReadable> heartbeatReadable) {
        this.heartbeatReadable = heartbeatReadable;
    }

    public void start() {


        Undertow server = Undertow.builder().addHttpListener(port,
                hostname).setHandler(Handlers.path()
                                .addPrefixPath(path,httpServerExchange -> {
                                    if(httpServerExchange.getRequestMethod().equals(Methods.GET)) {
                                        // Optional health check
                                        httpServerExchange.setStatusCode(200);
                                        httpServerExchange.getResponseSender().send(OK);
                                    } else {
                                        httpServerExchange.setStatusCode(405);
                                        httpServerExchange.getResponseSender().send("Not allowed");
                                    }
                                })
                                .addPrefixPath(reqPath,httpServerExchange -> {
                                    if(httpServerExchange.getRequestMethod().equals(Methods.GET)) {
                                        // Optional health check
                                        httpServerExchange.setStatusCode(200);
                                        httpServerExchange.getResponseSender().send(getMonitorList());
                                    } else {
                                        httpServerExchange.setStatusCode(405);
                                        httpServerExchange.getResponseSender().send("Not allowed");
                                    }
                                })).build();


        server.start();
    }

    public String getMonitorList() {


        final List<Map<Long,String>> ids = new ArrayList<>();

        for(HeartbeatReadable h: heartbeatReadable) {
            final Map<Long, String> idToName = h.getIdToName();
            if(idToName != null) {
                ids.add(idToName);
            }
        }

        return gson.toJson(ids);


    }

    public static void main(String args[]) {
        HeartbeatServer heartbeatServer = new HeartbeatServer("localhost",5121,"/health");

        heartbeatServer.start();
    }

}
