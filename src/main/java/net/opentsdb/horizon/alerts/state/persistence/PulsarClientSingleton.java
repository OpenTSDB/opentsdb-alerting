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

package net.opentsdb.horizon.alerts.state.persistence;

import java.io.Closeable;
import java.util.Map;

import com.google.common.collect.Maps;
import net.opentsdb.horizon.alerts.EnvironmentConfig;

import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.impl.AuthenticationUtil;
import org.apache.pulsar.client.impl.auth.AuthenticationAthenz;

// TODO: Probably fine. But maybe it is possible to avoid singletons.
public enum PulsarClientSingleton implements Closeable {

    INSTANCE;

    private final PulsarClient pulsarClient;

    PulsarClientSingleton() {
        final EnvironmentConfig config = new EnvironmentConfig();
        this.pulsarClient = config.isPulsarStatePersistenceEnabled()
                ? createPulsarClient(config)
                : null;
    }

    private Map<String, String> getAthenzTokenParam(final EnvironmentConfig config) {
        Map<String, String> authParams = Maps.newHashMap();
        authParams.put("tenantDomain", config.getPulsarAthenzTenantDomain());
        authParams.put("tenantService", config.getPulsarAthenzTenantService());
        authParams.put("providerDomain", config.getPulsarAthenzProviderDomain());
        authParams.put("privateKey", config.getPulsarAthenzPrivateKey());
        authParams.put("keyId", config.getPulsarAthenzKeyId());
        return authParams;
    }

    private PulsarClient createPulsarClient(final EnvironmentConfig config) {
        final String brokerName = config.getPulsarBrokerName();
        Map authParams = null;
        String authClassName = null;
         if (config.isPulsarAthenzEnabled()) {
             String type = config.getPulsarAthenzType();
             if("tls".equals(type)) {
                 authParams = Maps.newHashMap();
                 authParams.put("privateKeyPath", config.getAthenzKey());
                 // TODO - other params.
                 authClassName = AuthenticationAthenz.class.getName();

             }else if("token".equals(config.getPulsarAthenzType())){
                 authParams = getAthenzTokenParam(config);
                 authClassName = AuthenticationAthenz.class.getName();
             }
         }

        Authentication auth = null;
        try {
            auth = AuthenticationUtil.create(authClassName, authParams);
        } catch (PulsarClientException.UnsupportedAuthenticationException e) {
            e.printStackTrace();
        }

        try {
            return PulsarClient.builder()
                    .serviceUrl(brokerName)
                    .authentication(auth)
                    .build();
        } catch (PulsarClientException e) {
            throw new RuntimeException(
                    "failed to create a Pulsar client: broker_name=" + brokerName, e);
        }
    }

    @Override
    public void close() {
        try {
            pulsarClient.close();
        } catch (PulsarClientException e) {
            throw new RuntimeException("failed to close a Pulsar client", e);
        }
    }

    public static PulsarClient get() {
        return INSTANCE.pulsarClient;
    }
}
