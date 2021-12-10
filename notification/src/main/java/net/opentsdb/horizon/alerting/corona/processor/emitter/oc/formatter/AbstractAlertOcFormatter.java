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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OcMeta;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcSeverity;
import net.opentsdb.horizon.alerting.corona.model.metadata.OcTier;
import net.opentsdb.horizon.alerting.corona.processor.emitter.oc.AlertHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.oc.OcCommand;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;

public abstract class AbstractAlertOcFormatter<
        A extends Alert,
        V extends AlertView,
        M extends MessageKitView<A, V>
        >
        implements Formatter<MessageKit, List<OcCommand>>
{

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(AbstractAlertOcFormatter.class);

    private static final OcSeverity DEFAULT_SEVERITY = OcSeverity.SEV_3;

    private static final OcTier DEFAULT_TIER = OcTier.TIER_1;

    private static Joiner.MapJoiner SPACE_TAG_JOINER =
            Joiner.on(" ").withKeyValueSeparator(":");

    /* ------------ Fields ------------ */

    /**
     * The colo from which we emit alerts: `us-west, us-east`
     */
    private final String colo;

    /**
     * The host emitting alerts. Since we run from a container,
     * we have to pass it as a parameter.
     */
    private final String host;

    /* ------------ Constructor ------------ */

    protected AbstractAlertOcFormatter(final String colo,
                                       final String host)
    {
        Objects.requireNonNull(colo, "colo cannot be null");
        Objects.requireNonNull(host, "host cannot be null");
        this.colo = colo;
        this.host = host;
    }

    /* ------------ Abstract Methods ------------ */

    protected abstract StringBuilder buildAppDescriptionPrefix(
            final StringBuilder sb,
            final V view);

    protected abstract String buildMessageSnippet(final M messageKitView,
                                                  final V view);

    /* ------------ Methods ------------ */

    protected String sanitize(final String line)
    {
        if (line == null) {
            return null;
        }
        return line.replaceAll("[|:\n\r]", " ");
    }

    private String buildAppDescription(final V view)
    {
        // Format:
        // '<view prefix>: <spaced tag pairs>
        final StringBuilder sb = new StringBuilder();
        buildAppDescriptionPrefix(sb, view).append(": ");
        SPACE_TAG_JOINER.appendTo(sb, view.getSortedTags());
        return sb.toString();
    }

    private String getAlertHash(final M messageKitView, final V view)
    {
        return AlertHasher.hash(messageKitView, view);
    }

    private int getSeverity(final OcMeta meta)
    {
        OcSeverity severity = meta.getOcSeverity();
        if (severity == OcSeverity.NOT_SET) {
            severity = DEFAULT_SEVERITY;
        }

        OcTier tier = meta.getOcTier();
        if (tier == OcTier.NOT_SET) {
            tier = DEFAULT_TIER;
        }

        if (tier == OcTier.TIER_1) {
            return severity.getId();
        }

        return severity.getId() * 10 + tier.getId();
    }

    private String getHostTag(final V view)
    {
        return view.getSortedTags().getOrDefault("host", "");
    }

    private OcCommand formatOne(final M messageKitView,
                                final V view,
                                final OcMeta meta)
    {
        final OcCommand.Message message = new OcCommand.Message()
                .setAppDescription(
                        buildAppDescription(view),
                        sanitize(messageKitView.interpolateFlattenSubject(view)),
                        sanitize(messageKitView.interpolateFlattenBody(view))
                )
                .setHostTag(getHostTag(view))
                .setMessageSnippet(
                        buildMessageSnippet(messageKitView, view)
                )
                .setDashboardLink(
                        Views.get().alertViewUrl(messageKitView.getAlertId())
                )
                .setAlertHash(getAlertHash(messageKitView, view))
                .setRecovery(view.isRecovery() ? 0 : 1)
                .setRunbookIds(meta.getRunbookId());

        return OcCommand.builder()
                .setCheckId("||OPENTSDB")
                .setColo(colo)
                .setHost(host)
                .setProperty(view.getNamespace())
                .setSeverity(getSeverity(meta))
                .setMessage(message)
                .build();
    }

    @Override
    public List<OcCommand> format(final MessageKit messageKit)
    {
        @SuppressWarnings("unchecked")
        final M messageKitView = (M) Views.of(messageKit);
        final OcMeta meta = (OcMeta) messageKit.getMeta();

        final List<OcCommand> commands = new ArrayList<>();
        for (V view : messageKitView.getAllViews()) {
            try {
                commands.add(formatOne(messageKitView, view, meta));
            } catch (Exception e) {
                LOG.error("Failed to format: view={}", view);
            }
        }
        return commands;
    }
}
