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

package net.opentsdb.horizon.alerting.corona.processor.emitter.slack.formatter;

import java.util.TreeMap;

import com.google.inject.Inject;
import net.opentsdb.horizon.alerting.corona.model.alert.Event;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.EventAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackBuilders;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventMessageKitView;

public class SlackEventAlertFormatter
        extends SlackAbstractFormatter<EventAlert, EventAlertView, EventMessageKitView>
{

    @Inject
    public SlackEventAlertFormatter(final SlackBuilders buildersFactory)
    {
        super(buildersFactory);
    }

    @Override
    protected String generateAttachmentText(final EventAlertView view)
    {
        final String header = "Count: *" + view.getCount() + "*  " +
                "_Tags:_ " + formatTags(view.getSortedTags());

        final Event event = view.getEvent();
        if (event == null) {
            return header + "\n" +
                    "*Last Event:* [none]";
        } else {
            return header + "\n" +
                    "*Last Event:*\n" +
                    "  _Title_: " + event.getTitle() + "\n" +
                    "  _Tags_: " + formatTags(new TreeMap<>(event.getTags())) + "\n" +
                    "  _Properties_: namespace:*" + event.getNamespace() +
                    "*  source:*" + event.getSource() + "*" + "\n" +
                    "  _Message_: " + event.getMessage();
        }
    }
}
