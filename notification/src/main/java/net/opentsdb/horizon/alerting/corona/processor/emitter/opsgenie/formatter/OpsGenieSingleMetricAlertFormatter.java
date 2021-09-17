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

package net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie.formatter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Set;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OpsGenieMeta;
import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie.OpsGenieAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.ViewType;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import org.apache.commons.lang.StringUtils;

public class OpsGenieSingleMetricAlertFormatter<
        A extends Alert,
        V extends AlertView,
        M extends MessageKitView<A, V>
        >
        implements Formatter<MessageKit, OpsGenieAlert>
{

    /* ------------ Constants ------------ */

    private static final String EMPTY_STRING = "";

    private static final String CORONA_HORIZON_ALL = "corona_horizon_all";

    /* ------------ Fields ------------ */

    private final String user;

    private final String source;

    private final PebbleTemplate noteTemplate;

    private final StringWriter writer;

    /* ------------ Constructor ------------ */

    public OpsGenieSingleMetricAlertFormatter(final String user,
                                              final String source)
    {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(source, "source cannot be null");
        this.user = user;
        this.source = source;

        final ClasspathLoader loader = new ClasspathLoader();
        final PebbleEngine engine = new PebbleEngine.Builder()
                .loader(loader)
                .build();
        this.noteTemplate = engine.getTemplate("templates/opsgenie-single-metric-alert.html");
        this.writer = new StringWriter();
    }

    /* ------------ Methods ------------ */

    private String buildDescription(final MessageKitView<?, ?> view)
    {
        final String[] keys = view.getGroupKeys();
        final String[] values = view.getGroupValues();

        final String groupString;
        if (keys == null || keys.length == 0) {
            groupString = "Grouped by: <br/>[<strong>alert_id</strong>].";
        } else {
            groupString = "Grouped by: <br/>" + "[" +
                    IntStream.range(0, keys.length)
                            .mapToObj(i -> keys[i]
                                    + ":<strong>"
                                    + values[i]
                                    + "</strong>")
                            .collect(Collectors.joining(", ")) + "].";
        }

        return view.getGroupInterpolatedBody()
                + "<br/><br/>"
                + groupString;
    }

    private void appendAlertDescriptions(final StringBuilder sb,
                                         final ViewType type,
                                         final List<V> views,
                                         final String transitionString,
                                         final String thresholdMessage)
    {
        if (views == null || views.size() == 0) {
            return;
        }

        final Map<String, Object> params = new HashMap<>();

        if (type == ViewType.RECOVERY) {
            // want message to print '4 Recovered', not '4 Recovery'
            params.put("type", "Recovered");
        } else {
            params.put("type", StringUtils.capitalize(this.typeToStr(type)));
        }

        params.put("count", views.size());
        params.put("views", views);
        params.put("transitions", transitionString);
        params.put("thresholdMessage", thresholdMessage);

        writer.getBuffer().setLength(0);
        try {
            noteTemplate.evaluate(writer, params);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sb.append(writer.toString());
    }

    private String buildNote(final M messageKitView,
                             final ViewType... types)
    {
        if (types == null || types.length == 0) {
            return EMPTY_STRING;
        }
        final StringBuilder sb = new StringBuilder();

        // ex: {warn=[good], bad=[good], good=[warn, bad]}
        Map<ViewType, Set<String>> transitionMap;
        transitionMap =
                messageKitView.getAllViews().stream()
                        .collect(Collectors.groupingBy(
                                AlertView::getType,
                                Collectors.mapping(AlertView::getStateFrom, Collectors.toSet())));

        // ex: {warn=visitors.count >= 999.000000 at least once in the last 0 minutes}
        Map<ViewType, String> messageMap = new HashMap();
        for (V view : messageKitView.getAllViews()) {
            if (!messageMap.containsKey(view.getType())) {
                messageMap.put(view.getType(), view.getDescription());
            }
        }

        for (ViewType type : types) {
            if (transitionMap.containsKey(type)) {
                appendAlertDescriptions(
                        sb,
                        type,
                        messageKitView.getViews(type),
                        this.transitionMapToString(
                                this.typeToStr(type),
                                transitionMap.get(type)
                        ),
                        messageMap.get(type)
                );
            }
        }

        return sb.toString();
    }

    String typeToStr(ViewType type)
    {
        if (type == ViewType.RECOVERY) {
            return "good";
        } else {
            return type.toString().toLowerCase();
        }
    }

    // ex: Bad → Good, Warn → Good
    private String transitionMapToString(String endTransition, Set<String> fromTransitions)
    {
        StringBuilder sb = new StringBuilder();
        for (String fromTransition : fromTransitions) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(StringUtils.capitalize(fromTransition))
                    .append(" → ")
                    .append(StringUtils.capitalize(endTransition));
        }
        return sb.toString();
    }

    /**
     * Checks if the alert can be closed.
     * <p>
     * The conditions entail:
     * - Alert group has only one alert
     * - The alert is a recovery alert
     * - Alert tags has the same cardinality as the group key list
     * - Tags match
     * - None of tag values are null.
     * <p>
     * Note: There is a special case when alert has `corona_horizon_all` tag
     * attached, meaning that the query is a group by everything, hence only
     * one time series.
     * <p>
     * TODO: Add more complicated logic for a group with many alerts for
     * the same time series. Such case is possible when results from two
     * consecutive evaluations got into one group because of timing.
     *
     * @param group alert group
     * @param key   group key
     * @return true if can be closed, false if cannot.
     */
    private boolean canBeClosed(final AlertGroup group, final GroupKey key)
    {
        final List<Alert> alerts = group.getAlerts();
        if (alerts == null || alerts.size() != 1) {
            return false;
        }

        final Alert alert = alerts.get(0);
        if (alert.getState() != State.GOOD) {
            return false;
        }

        final Map<String, String> tags = alert.getTags();
        if (tags == null) {
            return false;
        }

        // Special case, when the query has a groupby by all tags.
        //
        if (tags.size() == 1
                && CORONA_HORIZON_ALL.equals(tags.get(CORONA_HORIZON_ALL))) {
            return true;
        }

        // The rest follows the logic in the method description.
        //
        final String[] keys = key.getKeys();
        final String[] vals = key.getValues();

        if (keys == null
                || vals == null
                || keys.length != vals.length
                || tags.size() != keys.length) {
            return false;
        }

        for (int i = 0; i < keys.length; i++) {
            final String k = keys[i];
            final String v = vals[i];
            if (k == null || v == null || !v.equals(tags.get(k))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public OpsGenieAlert format(final MessageKit messageKit)
    {
        @SuppressWarnings("unchecked")
        final M messageKitView = (M) Views.of(messageKit);
        final OpsGenieMeta meta = (OpsGenieMeta) messageKit.getMeta();

        final AlertGroup alertGroup = messageKit.getAlertGroup();
        final GroupKey groupKey = alertGroup.getGroupKey();
        final boolean canBeClosed = canBeClosed(alertGroup, groupKey);

        final long sampleAlertTimestampMs =
                alertGroup.getAlerts().get(0).getTimestampSec() * 1_000L;

        return OpsGenieAlert.builder()
                .setNamespace(messageKitView.getNamespace())
                .setAlias(AbstractOpsGenieFormatter.buildAlias(messageKitView))
                .setUser(user)
                .setSource(source)
                .setPriority(meta.getOpsGeniePriority())
                .addVisibleToTeams("Test_moog_1")
                .setMessage(messageKitView.getGroupInterpolatedSubject())
                .setDescription(buildDescription(messageKitView))
                .setGeneralNote(
                        buildNote(
                                messageKitView,
                                ViewType.BAD,
                                ViewType.WARN,
                                ViewType.MISSING
                        )
                )
                .setRecoveryNote(
                        buildNote(messageKitView, ViewType.RECOVERY)
                )
                .setTags(OpsGenieTagFormatter.formatTags(
                        meta.getLabels(),
                        meta.getOpsGenieTags(),
                        messageKitView.getGroupKeys(),
                        messageKitView.getGroupValues()
                ))
                .setCanBeClosed(canBeClosed)
                .setIncludeRecoveryNote(canBeClosed)
                .addDetail(
                        "OpenTSDB View Details",
                        Views.alertSplunkUrl(
                                messageKitView.getAlertId(),
                                sampleAlertTimestampMs
                        )
                )
                .addDetail(
                        "OpenTSDB Modify Alert",
                        Views.alertViewUrl(messageKitView.getAlertId())
                )
                .build();
    }
}
