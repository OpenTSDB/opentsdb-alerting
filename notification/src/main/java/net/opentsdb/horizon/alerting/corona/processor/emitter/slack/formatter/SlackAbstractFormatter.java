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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackBuilders;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackColor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackRequest;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.ViewType;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import org.apache.commons.lang.StringUtils;

abstract class SlackAbstractFormatter<
        A extends Alert,
        V extends AlertView,
        M extends MessageKitView<A, V>
        >
        implements Formatter<MessageKit, List<SlackRequest>>
{

    private static final int MAX_ATTACHMENTS = 3;

    private static final ViewType[] TO_STATE_ORDER =
            new ViewType[]{
                    ViewType.BAD,
                    ViewType.WARN,
                    ViewType.RECOVERY,
                    ViewType.MISSING,
            };

    private static final String[] FROM_STATE_ORDER =
            new String[]{
                    Views.of(State.GOOD),
                    Views.of(State.BAD),
                    Views.of(State.WARN),
                    Views.of(State.MISSING),
            };

    private static final String[] MARKDOWN_IN_TEXT = new String[]{"text"};

    private final SlackBuilders buildersFactory;

    protected SlackAbstractFormatter(final SlackBuilders buildersFactory)
    {
        Objects.requireNonNull(buildersFactory, "buildersFactory cannot be null");
        this.buildersFactory = buildersFactory;
    }

    @Override
    public List<SlackRequest> format(final MessageKit mk)
    {
        @SuppressWarnings("unchecked")
        final M mkView = (M) Views.of(mk);

        return Arrays.stream(TO_STATE_ORDER)
                .map(viewType -> {
                    final Map<String, List<V>> viewsPerTransition =
                            mkView.getViews(viewType).stream()
                                    .collect(Collectors.groupingBy(
                                            V::getStateFrom,
                                            Collectors.toList()
                                    ));

                    return Arrays.stream(FROM_STATE_ORDER)
                            .map(viewsPerTransition::get)
                            .filter(Objects::nonNull)
                            .map(views -> buildRequest(mkView, viewType, views))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toList());
                })
                .collect(ArrayList::new, List::addAll, List::addAll);
    }

    private Optional<SlackRequest> buildRequest(final M mkView,
                                                final ViewType type,
                                                final List<V> views)
    {
        if (views == null || views.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                buildersFactory.createRequestBuilder()
                        .setText(escape(generateText(mkView, views)))
                        .setAttachments(generateAttachments(mkView, type, views))
                        .build()
        );
    }

    private String generateText(final M mkView,
                                final List<V> views)
    {
        final V sampleView = views.get(0);
        final String description = sampleView.getDescription("*", "*");
        final String fromState = StringUtils.capitalize(sampleView.getStateFrom());
        final String toState = StringUtils.capitalize(sampleView.getStateTo());
        final int showCount = Math.min(views.size(), MAX_ATTACHMENTS);
        final String groupedBy =
                generateGroupedByString(
                        mkView.getGroupKeys(),
                        mkView.getGroupValues()
                );

        return "" +
                // [<namespace>] <subject>\n
                // => [AlephD] Datadog agent UP\n
                "*[" + mkView.getNamespace() + "] " + mkView.getGroupInterpolatedSubject() + "*\n" +

                // <description>
                // => AlephD.AWS-ElasticMapReduce.IsIdle.avg >= 1.000000 at all times in the last 360 minutes
                "_" + description + "_\n" +

                // <count> <from> *<type>*, showing <show count>. Grouped by [<group keys>]
                // 96 Good → Bad, showing 3. Grouped by [alert_id].
                "*" + views.size() + "* " + fromState + " -> *" + toState + "*, " +
                "showing " + showCount + ". " +
                "Grouped by " + groupedBy + ".";
    }

    private String generateGroupedByString(final String[] keys,
                                           final String[] vals)
    {
        if (keys.length == 0) {
            return "[alert_id]";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < keys.length; i++) {
            sb.append(keys[i])
                    .append(":*")
                    .append(vals[i])
                    .append("*");
            if (i != keys.length - 1) {
                sb.append("  ");
            }
        }
        sb.append("]");

        return sb.toString();
    }

    private List<SlackRequest.Attachment> generateAttachments(
            final M mkView,
            final ViewType viewType,
            final List<V> views)
    {
        // Alert -> attachments mapper.
        final Function<V, SlackRequest.Attachment> mapper = v ->
                buildersFactory.createAttachmentBuilder()
                        .setMarkdownIn(MARKDOWN_IN_TEXT)
                        .setColor(colorFrom(viewType))
                        .setText(escape(generateAttachmentText(v)))
                        .build();

        // Footer.
        final long alertId = mkView.getAlertId();
        final long alertTsMs = views.get(0).getTimestampMs();
        final String splunkUrl = Views.alertSplunkUrl(alertId, alertTsMs);
        final String editUrl = Views.alertEditUrl(alertId);
        final String footerText = escape(mkView.getGroupInterpolatedBody());

        final SlackRequest.Attachment footer =
                buildersFactory.createAttachmentBuilder()
                        .setMarkdownIn(MARKDOWN_IN_TEXT)
                        .setColor(SlackColor.NEUTRAL)
                        .setText("<" + splunkUrl + "|View details> | " +
                                "<" + editUrl + "|Modify alert>\n" +
                                footerText
                        )
                        .build();

        return Stream
                .concat(
                        views.stream()
                                .limit(MAX_ATTACHMENTS)
                                .map(mapper),
                        Stream.of(footer)
                )
                .collect(Collectors.toList());
    }

    protected abstract String generateAttachmentText(V view);

    protected static String formatTags(final SortedMap<String, String> tags)
    {
        if (tags == null || tags.isEmpty()) {
            return "[no tags]";
        }

        return tags.entrySet().stream()
                .map(e -> e.getKey() + ":*" + e.getValue() + "*")
                .collect(Collectors.joining("  "));
    }

    /**
     * Escape Slack control symbols.
     * https://api.slack.com/docs/message-formatting#how_to_escape_characters
     *
     * @param text text to be escaped
     * @return escaped string.
     */
    static String escape(final String text)
    {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    protected static SlackColor colorFrom(final ViewType viewType) {
        switch (viewType) {
            case BAD:
                return SlackColor.DANGER;
            case WARN:
                return SlackColor.WARNING;
            case MISSING:
                return SlackColor.GRAY;
            case RECOVERY:
                return SlackColor.GOOD;
            default:
                return SlackColor.BLACK;
        }
    }
}
