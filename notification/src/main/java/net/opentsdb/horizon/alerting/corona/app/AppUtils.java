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

package net.opentsdb.horizon.alerting.corona.app;

import java.util.Properties;

public class AppUtils {

    /**
     * Property name to set amount of time for {@link com.oath.auth.KeyRefresher}
     * to wait until the key and certificate files are generated.
     * <p>
     * https://github.com/yahoo/athenz/blob/c4dc89b31fda501af45c20b33db620a077079744/libs/java/cert_refresher/src/main/java/com/oath/auth/Utils.java#L56
     */
    public static final String P_ATHENZ_KEY_CERT_WAIT_TIME =
            "athenz.cert_refresher.key_wait_time";

    /**
     * @param timeoutMin timeout in minutes
     * @return true if the timeout was set, false if property
     * has already been set.
     */
    public static boolean trySetAthenzKeyCertWaitTimeMin(final int timeoutMin)
    {
        final Properties systemProperties = System.getProperties();
        if (systemProperties.containsKey(P_ATHENZ_KEY_CERT_WAIT_TIME)) {
            // Do not override
            return false;
        }
        systemProperties.setProperty(P_ATHENZ_KEY_CERT_WAIT_TIME,
                Integer.toString(timeoutMin));
        return true;
    }

}
