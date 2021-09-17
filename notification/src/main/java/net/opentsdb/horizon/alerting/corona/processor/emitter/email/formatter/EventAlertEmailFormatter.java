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

package net.opentsdb.horizon.alerting.corona.processor.emitter.email.formatter;

import java.util.Map;

import net.opentsdb.horizon.alerting.corona.model.alert.impl.EventAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventMessageKitView;

public class EventAlertEmailFormatter
        extends AbstractEmailFormatter<
        EventAlert,
        EventAlertView,
        EventMessageKitView>
{

    /* ------------ Constructors ------------ */

    public EventAlertEmailFormatter(final String debugPrefix)
    {
        super(debugPrefix, "templates/email-event-alert.html");
    }

    /* ------------ Methods ------------ */

    @Override
    protected Map<String, byte[]> generateImages(
            final EventMessageKitView messageKit)
    {
        return null;
    }
}
