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

package net.opentsdb.horizon.alerts.http.impl;

import com.oath.auth.KeyRefresher;
import com.oath.auth.Utils;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.http.AuthProviderForAlertClient;

import javax.net.ssl.SSLContext;

import java.util.Map;

public class AthensAuthProvider implements AuthProviderForAlertClient {

    private EnvironmentConfig environmentConfig = new EnvironmentConfig();

    private static class WRAPPER {
        private static volatile SSLContext sslContext = null;
    }
    
    @Override
    public SSLContext getSSLContext() throws Exception {

        if(WRAPPER.sslContext == null) {

            synchronized (AthensAuthProvider.class) {

                if(WRAPPER.sslContext == null) {

                    KeyRefresher keyRefresher =
                            Utils.generateKeyRefresher(
                                    environmentConfig.getTrustStorePath(),
                                    environmentConfig.getTrustStorePassword(),
                                    environmentConfig.getAthenzCert(),
                                    environmentConfig.getAthenzKey());

                    keyRefresher.startup();

                    WRAPPER.sslContext = Utils.buildSSLContext(keyRefresher.getKeyManagerProxy(),
                            keyRefresher.getTrustManagerProxy());

                }
            }
        }
        return WRAPPER.sslContext;
    }

    @Override
    public Map<String, String> getCookies() {
        return null;
    }
}
