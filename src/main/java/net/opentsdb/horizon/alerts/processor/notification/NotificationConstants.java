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

package net.opentsdb.horizon.alerts.processor.notification;

import java.util.Random;

public class NotificationConstants {


    public static final String EMAIL_ALIAS = "%s-Alert";

    public static final Random random = new Random();

    public static final String DEFAULT_GROUP = String.valueOf(random.nextLong());

    public static final String DEFAULT_GROUP_SUFFIX = "";

    public static final String recoverySubject = "[RECOVERED] [%s] %s %s";

    public static final String otherSubject = "[%s] %s %s";

    public static final String compareLine = "The following groups were %s " +
            "the threshold %s " +
            "in the last " +
            "%s minutes from %s," +
            "putting them in a [%s] state";

    public static final String SUBSTITUTE_START = "\\{\\{";

    public static final String SUBSTITUTE_END = "\\}\\}";

    public static final String COMPARE_SUBSTITUTE_START = "{{";

    public static final String COMPARE_SUBSTITUTE_END = "}}";

    public static final String NON_GRP_REPLACE = "%s %s"+"s";

}
