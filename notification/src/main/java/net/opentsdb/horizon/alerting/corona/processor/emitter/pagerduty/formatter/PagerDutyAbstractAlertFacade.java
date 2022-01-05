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

package net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty.formatter;

import com.google.common.base.Joiner;
import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.PagerDutyContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.PagerDutyMeta;
import net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty.PagerDutyEvent;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import org.apache.commons.codec.digest.DigestUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class PagerDutyAbstractAlertFacade<
        A extends Alert,
        V extends AlertView,
        M extends MessageKitView<A, V>>
        implements PagerDutyEvent
{
    /* ------------ Constants ------------ */

    /* ------------ Fields ------------ */

    protected M messageKitView;
    protected V alertView;
    protected final PagerDutyMeta meta;
    protected final PagerDutyContact contact;

    protected Payload payload;

    /* ------------ Constructor ------------ */

    public PagerDutyAbstractAlertFacade(final M messageKitView,
                                        final V alertView,
                                        final PagerDutyMeta meta,
                                        final PagerDutyContact contact)
    {
        Objects.requireNonNull(messageKitView, "messageKitView cannot be null");
        Objects.requireNonNull(alertView, "alertView cannot be null");
        this.messageKitView = messageKitView;
        this.alertView = alertView;
        this.meta = meta;
        this.contact = contact;

        this.payload = new Payload() {
            @Override
            public String getSummary() {
                return messageKitView.interpolateSubject(alertView);
            }

            @Override
            public String getSource() {
                final SortedMap<String, String> tags = alertView.getSortedTags();
                if (tags.containsKey("host")) {
                    return tags.get("host");
                } else if (tags.containsKey("hostgroup")) {
                    return tags.get("hostgroup");
                } else if (tags.containsKey("InstanceId")) {
                    return tags.get("InstanceId");
                }
                return getAlertSpecificSource();
            }

            @Override
            public Severity getSeverity() {
                switch (alertView.getType()) {
                    case BAD:
                    case MISSING:
                        return Severity.ERROR;
                    case WARN:
                        return Severity.WARNING;
                    case RECOVERY:
                    default:
                        return Severity.INFO;
                }
            }

            @Override
            public String getTimestamp() {
                SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                return sf.format(alertView.getTimestampMs());
            }

            @Override
            public HashMap<String, Object> getCustomDetails() {
                final HashMap<String, Object> params = new HashMap<>();
                params.put("type", getAlertType());
                params.put("body", buildDescription(messageKitView));
                params.put("details", alertView.getDescription());
                params.put("tags", alertView.getSortedTags());
                return params;
            }
        };
    }

    @Override
    public EventAction getEventAction() {
        if (alertView.isRecovery()) {
            return EventAction.RESOLVE;
        }
        return EventAction.TRIGGER;
    }

    @Override
    public String getRoutingKey() {
        return contact.getRoutingKey();
    }

    @Override
    public String getDedupKey() {
        return buildDedupKey(messageKitView);
    }

    abstract protected String getAlertSpecificSource();
    abstract protected String getAlertType();

    @Override
    public List<Map<String, String>> getLinks() {
        Map<String, String> link1 = new HashMap<>();
        link1.put("text", "OpenTSDB View Details");
        link1.put("href", Views.get().alertSplunkUrl(
            messageKitView.getAlertId(),
            alertView.getTimestampMs()
        ));

        Map<String, String> link2 = new HashMap<>();
        link2.put("text", "OpenTSDB Modify Alert");
        link2.put("href", Views.get().alertViewUrl(messageKitView.getAlertId()));

        List<Map<String, String>> links = new ArrayList<>();
        links.add(link1);
        links.add(link2);

        return links;
    }

    @Override
    public Payload getPayload() {
        return payload;
    }

    public static String buildDedupKey(final MessageKitView<?, ?> view)
    {
        final String[] keys = view.getGroupKeys();
        final String[] values = view.getGroupValues();
        final SortedMap<String, String> groupTags = new TreeMap<>();
        for (int i = 0; i < keys.length; ++i) {
            if (Objects.isNull(values[i])) {
                groupTags.put(keys[i], "null");
            } else {
                groupTags.put(keys[i], values[i]);
            }
        }

        final StringBuilder toHash = new StringBuilder();
        toHash.append(view.getAlertId()).append('|')
                .append(view.getNamespace()).append('|');
        Joiner.on(';').withKeyValueSeparator("=").appendTo(toHash,groupTags);
        return DigestUtils.sha256Hex(toHash.toString());
    }

    private String buildDescription(final MessageKitView<?, ?> view)
    {
        final String[] keys = view.getGroupKeys();
        final String[] values = view.getGroupValues();

        final String groupString;
        if (keys == null || keys.length == 0) {
            groupString = "Grouped on the alert level.";
        } else {
            groupString = "Grouped by:\n" +
                    IntStream.range(0, keys.length)
                            .mapToObj(i -> keys[i]
                                    + ":"
                                    + values[i]
                                    + "")
                            .collect(Collectors.joining(", "));
        }

        return view.getGroupInterpolatedBody()
                + "\n\n"
                + groupString;
    }

}
