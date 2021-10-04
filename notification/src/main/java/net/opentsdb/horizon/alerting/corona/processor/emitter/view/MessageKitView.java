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

package net.opentsdb.horizon.alerting.corona.processor.emitter.view;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.processor.emitter.Interpolator;

public abstract class MessageKitView<A extends Alert, V extends AlertView> {

    /* ------------ Fields ------------ */

    @Getter
    private final String namespace;

    @Getter
    private final long alertId;

    @Getter
    private final String subject;

    @Getter
    private final String body;

    @Getter
    private final String[] groupKeys;

    @Getter
    private final String[] groupValues;

    /**
     * Alert type to a list alert views mapping.
     *
     * @return mapping from alert type to a list of corresponding
     * alert views.
     */
    @Getter
    private final Map<ViewType, List<V>> viewsByType;

    @Getter
    private int alertsTotal;

    @Getter
    private final String groupInterpolatedSubject;

    @Getter
    private final String groupInterpolatedBody;

    /* ------------ Constructors ------------ */

    public MessageKitView(final MessageKit messageKit)
    {
        Objects.requireNonNull(messageKit, "messageKit cannot be null");

        final AlertGroup alertGroup = messageKit.getAlertGroup();
        final GroupKey groupKey = alertGroup.getGroupKey();

        @SuppressWarnings("unchecked")
        final List<A> alerts = (List<A>) alertGroup.getAlerts();

        this.namespace = groupKey.getNamespace();
        this.alertId = groupKey.getAlertId();
        this.subject = messageKit.getMeta().getSubject();
        this.body = messageKit.getMeta().getBody();
        this.groupKeys = groupKey.getKeys();
        this.groupValues = groupKey.getValues();
        this.viewsByType = viewsPerType(alerts);
        this.alertsTotal = alerts.size();
        this.groupInterpolatedSubject = Interpolator.tryInterpolate(subject, groupKeys, groupValues);
        this.groupInterpolatedBody = Interpolator.tryInterpolate(body, groupKeys, groupValues);
    }

    /* ------------ Abstract Methods ------------ */

    protected abstract V getViewOf(final A alert);

    public abstract String interpolateSubject(final V alertView);

    public abstract String interpolateBody(final V alertView);

    /* ------------ Methods ------------ */

    private ViewType stateToViewType(final State state)
    {
        switch (state) {
            case BAD:
                return ViewType.BAD;
            case WARN:
                return ViewType.WARN;
            case GOOD:
                return ViewType.RECOVERY;
            case MISSING:
                return ViewType.MISSING;
            case UNKNOWN:
                return ViewType.UNKNOWN;
            default:
                return ViewType.UNDEFINED;

        }
    }

    private int getSize(final ViewType viewType,
                        final Map<ViewType, List<V>> viewsByType)
    {
        final List<V> list = viewsByType.get(viewType);
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public List<V> getAllViews()
    {
        return viewsByType.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public List<V> getViews(final ViewType viewType)
    {
        final List<V> views = viewsByType.get(viewType);
        if (views == null) {
            return Collections.emptyList();
        }
        return views;
    }

    private Map<ViewType, List<V>> viewsPerType(final List<A> alerts)
    {
        return alerts.stream().collect(
                Collectors.groupingBy(
                        alert -> stateToViewType(alert.getState()),
                        Collectors.mapping(
                                this::getViewOf,
                                Collectors.toList()
                        )

                )
        );
    }

    public int getBadCount()
    {
        return getSize(ViewType.BAD, viewsByType);
    }

    public int getWarnCount()
    {
        return getSize(ViewType.WARN, viewsByType);
    }

    public int getRecoveryCount()
    {
        return getSize(ViewType.RECOVERY, viewsByType);
    }

    public int getMissingCount()
    {
        return getSize(ViewType.MISSING, viewsByType);
    }

    public int getUnknownCount()
    {
        return getSize(ViewType.UNKNOWN, viewsByType);
    }

    private String flatten(final String original)
    {
        return original.replaceAll("[\r\n]+", " ");
    }

    public String getFlattenedSubject()
    {
        return flatten(subject);
    }

    public String getFlattenedBody()
    {
        return flatten(body);
    }

    public String getFlattenedGroupInterpolatedSubject()
    {
        return flatten(groupInterpolatedSubject);
    }

    public String getFlattenedGroupInterpolatedBody()
    {
        return flatten(groupInterpolatedBody);
    }

    public String interpolateFlattenSubject(final V alertView)
    {
        return flatten(interpolateSubject(alertView));
    }

    public String interpolateFlattenBody(final V alertView)
    {
        return flatten(interpolateBody(alertView));
    }

    protected String interpolateSubject(final Map<String, String> tags)
    {
        return Interpolator.interpolate(subject, tags);
    }

    protected String interpolateBody(final Map<String, String> tags)
    {
        return Interpolator.interpolate(body, tags);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MessageKitView<?, ?> that = (MessageKitView<?, ?>) o;
        return alertId == that.alertId &&
                alertsTotal == that.alertsTotal &&
                Objects.equals(namespace, that.namespace) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(body, that.body) &&
                Arrays.equals(groupKeys, that.groupKeys) &&
                Arrays.equals(groupValues, that.groupValues) &&
                Objects.equals(viewsByType, that.viewsByType);
    }

    @Override
    public int hashCode()
    {
        int result =
                Objects.hash(
                        namespace,
                        alertId,
                        subject,
                        body,
                        viewsByType,
                        alertsTotal
                );
        result = 31 * result + Arrays.hashCode(groupKeys);
        result = 31 * result + Arrays.hashCode(groupValues);
        return result;
    }
}
