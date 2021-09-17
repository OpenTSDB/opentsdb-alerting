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
import java.util.List;
import java.util.stream.Collectors;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackBuilders;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackColor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.api.SlackRequest;
import net.opentsdb.horizon.alerting.corona.processor.emitter.slack.client.impl.SlackBuildersImpl;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.ViewType;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;

public abstract class AbstractSlackFormatter<
        A extends Alert,
        V extends AlertView,
        M extends MessageKitView<A, V>
        >
        implements Formatter<MessageKit, SlackRequest>
{

    /* ------------ Constants ------------ */

    private static final ViewType[] VIEW_ORDER =
            new ViewType[]{
                    ViewType.BAD,
                    ViewType.WARN,
                    ViewType.MISSING,
                    ViewType.RECOVERY
            };

    private static final int MAX_ATTACHMENTS_PER_STATE = 3;

    private static final String COLOR_BLACK = "#000000";

    private static final String COLOR_GRAY = "#5c5b4b";

    private static final String COLOR_RED = "danger";

    private static final String COLOR_GREEN = "good";

    private static final String COLOR_YELLOW = "warning";

    private static final String[] TEXT_AND_FIELDS =
            new String[]{"text", "fields"};

    /* ------------ Static Methods ------------ */

    /**
     * Escape Slack control symbols.
     * https://api.slack.com/docs/message-formatting#how_to_escape_characters
     *
     * @param text text to be escaped
     * @return escaped string.
     */
    protected static String escapeControlSymbols(final String text)
    {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private final SlackBuilders buildersFactory =
            SlackBuildersImpl.instance();

    /* ------------ Methods ------------ */

    private SlackColor getColor(final ViewType viewType)
    {
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

    protected String buildTitle(final V view)
    {
        return String.format(
                "%s -> %s",
                view.getStateFrom(),
                view.getStateTo()
        );
    }

    protected String buildText(final V view)
    {
        return view.getDescription("*", "*");
    }

    protected String buildTags(final V view)
    {
        return view.getSortedTags().entrySet().stream()
                .map(e -> e.getKey()
                        + ":"
                        + "*" + e.getValue() + "*"
                )
                .collect(Collectors.joining(", "));
    }

    protected void addShortFields(
            final SlackRequest.Attachment.Builder<?> builder,
            final V view)
    {
        final String tags = buildTags(view);
        builder.addShortField("Tags", tags);
    }

    protected void addLongFields(
            final SlackRequest.Attachment.Builder<?> builder,
            final V view)
    {
        // Intentionally nothing.
    }

    private SlackRequest.Attachment buildAttachment(final ViewType viewType,
                                                    final V view)
    {
        final SlackRequest.Attachment.Builder<?> builder =
                buildersFactory.createAttachmentBuilder()
                        .setColor(getColor(viewType))
                        .setTitle(buildTitle(view))
                        .setText(escapeControlSymbols(buildText(view)))
                        .setTimestampSec(view.getTimestampMs() / 1000L)
                        .setMarkdownIn(TEXT_AND_FIELDS);

        addShortFields(builder, view);
        addLongFields(builder, view);

        return builder.build();
    }

    private List<SlackRequest.Attachment> buildAttachments(
            final M messageKitView)
    {
        final List<SlackRequest.Attachment> attachments = new ArrayList<>();

        for (ViewType viewType : VIEW_ORDER) {
            final List<V> views = messageKitView.getViews(viewType);

            final int n = Math.min(views.size(), MAX_ATTACHMENTS_PER_STATE);

            for (int i = 0; i < n; i++) {
                final SlackRequest.Attachment attachment =
                        buildAttachment(viewType, views.get(i));
                attachments.add(attachment);
            }
        }

        return attachments;
    }

    /**
     * Build state count line.
     *
     * <pre>{@code
     *  1 Bad, 5 Recovery // Do not show 0 counts.
     * }</pre>
     *
     * @param messageKitView message kit view instance
     * @return formatted state count line.
     */
    private String buildStateCountLine(
            final MessageKitView<?, ?> messageKitView)
    {
        final List<String> countTerms = new ArrayList<>(4);

        final int bad = messageKitView.getBadCount();
        final int warn = messageKitView.getWarnCount();
        final int missing = messageKitView.getMissingCount();
        final int recovery = messageKitView.getRecoveryCount();

        if (bad != 0) {
            countTerms.add("*" + bad + "* Bad");
        }
        if (warn != 0) {
            countTerms.add("*" + warn + "* Warn");
        }
        if (missing != 0) {
            countTerms.add("*" + missing + "* Missing");
        }
        if (recovery != 0) {
            countTerms.add("*" + recovery + "* Recovery");
        }

        return String.join(", ", countTerms);
    }

    /**
     * <pre>{@code
     *  <user's formatted subject, bold>
     *  <space>
     *  <user's formatted body>
     *
     *  Grouped By: tag:value, tag:value, tag: value.
     *  <space>
     *  1 Bad, 5 Recovery // Do not show 0 counts.
     *  <space>
     *  View details | Modify alert
     * }</pre>
     */
    private String buildText(final MessageKitView<?, ?> messageKitView)
    {
        final String template = ""
                + "*%s*\n" // subject
                + "\n"
                + "%s\n" // body
                + "\n"
                + "*Grouped By* > %s\n" // tag pairs
                + "\n"
                + "%s\n" // state counts line
                + "\n"
                + "<%s|View details> | <%s|Modify alert>";

        final String subject = messageKitView.getGroupInterpolatedSubject();
        final String body = messageKitView.getGroupInterpolatedBody();

        final String[] keys = messageKitView.getGroupKeys();
        final String[] vals = messageKitView.getGroupValues();

        final String tagsStr;
        if (keys.length > 0) {
            final StringBuilder tags = new StringBuilder();
            for (int i = 0; i < keys.length; i++) {
                tags.append(keys[i])
                        .append(": * ")
                        .append(vals[i])
                        .append("*");
                if (i != keys.length - 1) {
                    tags.append(", ");
                }
            }
            tagsStr = tags.toString();
        } else {
            // *Grouped By*: <alert>
            tagsStr = "<alert>";
        }

        final long sampleAlertTimestmpMs =
                messageKitView.getAllViews().get(0).getTimestampMs();

        return String.format(
                template,
                subject.trim(),
                body.trim(),
                tagsStr,
                buildStateCountLine(messageKitView),
                Views.alertSplunkUrl(
                        messageKitView.getAlertId(),
                        sampleAlertTimestmpMs
                ),
                Views.alertEditUrl(messageKitView.getAlertId())
        );
    }

    @Override
    public SlackRequest format(final MessageKit messageKit)
    {
        @SuppressWarnings("unchecked")
        final M messageKitView = (M) Views.of(messageKit);

        final List<SlackRequest.Attachment> attachments =
                buildAttachments(messageKitView);

        return buildersFactory.createRequestBuilder()
                .setUrl(Views.alertViewUrl(messageKitView.getAlertId()))
                .setText(buildText(messageKitView))
                .setAttachments(attachments)
                .build();
    }
}
