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

package net.opentsdb.horizon.alerting.corona.processor.emitter.oc.formatter;

import net.opentsdb.horizon.alerting.corona.model.alert.impl.HealthCheckAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthCheckAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthChekMessageKitView;

public class HealthCheckAlertOcFormatter
        extends AbstractAlertOcFormatter<
        HealthCheckAlert,
        HealthCheckAlertView,
        HealthChekMessageKitView
        > {

    /* ------------ Constructor ------------ */

    public HealthCheckAlertOcFormatter(final String colo, final String host) {
        super(colo, host);
    }

    /* ------------ Methods ------------ */

    @Override
    protected StringBuilder buildAppDescriptionPrefix(
            final StringBuilder sb,
            final HealthCheckAlertView view) {
        // Format: '<data-namespace>.<application>'
        return sb.append(view.getDataNamespace())
                .append(".")
                .append(view.getApplication());
    }

    @Override
    protected String buildMessageSnippet(
            final HealthChekMessageKitView messageKitView,
            final HealthCheckAlertView view) {
        if (view.getStatusMessage() != null) {
            return String.format(
                    "%s.%s -> %s",
                    view.getDataNamespace(),
                    view.getApplication(),
                    limitTo256(sanitize(view.getStatusMessage()))
            );
        } else {
            return String.format(
                    "%s.%s -> %s",
                    view.getDataNamespace(),
                    view.getApplication(),
                    view.getDescription()
            );
        }
    }

    private static String limitTo256(String original) {
        if (original == null || original.length() <= 256) {
            return original;
        }
        return original.substring(0, 256 - 3) + "...";
    }
}
