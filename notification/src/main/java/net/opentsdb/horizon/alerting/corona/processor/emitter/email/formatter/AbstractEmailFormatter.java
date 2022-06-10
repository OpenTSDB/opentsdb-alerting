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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import org.apache.commons.io.IOUtils;

import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.email.EmailMessage;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;

public abstract class AbstractEmailFormatter<
        A extends Alert,
        V extends AlertView,
        M extends MessageKitView<A, V>
        >
        implements Formatter<MessageKit, EmailMessage>
{

    @Getter
    private static class Parameters {

        private final Map<String, Object> params = new HashMap<>();

        private Parameters setWebuiLink(final String webuiLink)
        {
            params.put("webuiLink", webuiLink);
            return this;
        }

        private Parameters setDetailsLink(final String detailsLink)
        {
            params.put("detailsLink", detailsLink);
            return this;
        }

        private Parameters setEditLink(final String editLink)
        {
            params.put("editLink", editLink);
            return this;
        }

        private Parameters setTotal(final int total)
        {
            params.put("total", total);
            return this;
        }

        private Parameters setBad(final int bad)
        {
            params.put("bad", bad);
            return this;
        }

        private Parameters setWarn(final int warn)
        {
            params.put("warn", warn);
            return this;
        }

        private Parameters setMissing(final int missing)
        {
            params.put("missing", missing);
            return this;
        }

        private Parameters setRecovery(final int recovery)
        {
            params.put("recovery", recovery);
            return this;
        }

        private Parameters setTimestampMs(final long timestampMs)
        {
            params.put("timestampMs", timestampMs);
            return this;
        }

        private Parameters setBody(final String body)
        {
            params.put("body", body);
            return this;
        }

        private Parameters setNamespace(final String namespace)
        {
            params.put("namespace", namespace);
            return this;
        }

        /**
         * @param viewMap ViewType name -> list of views. "bad", "good", etc.
         * @param <V>     view class
         * @return self.
         */
        private <V extends AlertView> Parameters setViewMap(
                final Map<String, List<V>> viewMap)
        {
            params.put("viewMap", viewMap);
            return this;
        }

        private Map<String, Object> toMap()
        {
            return params;
        }
    }

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(AbstractEmailFormatter.class);

    private static final String P_FROM_ALIAS = "%s%s-Alert";

    private static final String P_FROM =
            "%s-alert-do-not-reply@opentsdb.net";

    private static final String P_SUBJECT = "%s[%s] %s %s";

    protected static final String EMPTY_STRING = "";

    private static final String LOGO = "logo.png";

    /* ------------ Static Methods ------------ */

    private static byte[] getLogo()
    {
        try (final InputStream is =
                     HealthCheckAlertEmailFormatter.class
                             .getClassLoader()
                             .getResourceAsStream(LOGO)
        ) {
            return IOUtils.toByteArray(is);
        } catch (Exception e) {
            LOG.error("Failed to load Logo from the class path", e);
            return new byte[0];
        }
    }

    /* ------------ Fields ------------ */

    private final String debugPrefix;

    private final ThreadLocal<StringWriter> localWriter;

    private final PebbleTemplate template;

    private final byte[] logoBytes;

    /* ------------ Constructor ------------ */

    public AbstractEmailFormatter(final String debugPrefix,
                                  final String templatePath)
    {
        this.debugPrefix = debugPrefix == null ? EMPTY_STRING : debugPrefix;
        this.localWriter = ThreadLocal.withInitial(StringWriter::new);
        final ClasspathLoader loader = new ClasspathLoader();
        final PebbleEngine engine = new PebbleEngine.Builder()
                .loader(loader)
                .build();
        this.template = engine.getTemplate(templatePath);
        this.logoBytes = getLogo();
    }

    /* ------------ Abstract Methods ------------ */

    protected abstract Map<String, byte[]> generateImages(final M messageKit);

    /* ------------ Methods ------------ */

    private Map<String, byte[]> getImages(final M messageKit)
    {
        final Map<String, byte[]> images = generateImages(messageKit);
        if (images == null) {
            return new HashMap<>();
        }
        return images;
    }

    private String buildSubject(final int numAlerts,
                                final String alertSubject,
                                final String[] groupValues)
    {
        final String valuesStr =
                groupValues != null && groupValues.length > 0
                        ? Arrays.toString(groupValues)
                        : EMPTY_STRING;
        return String.format(
                P_SUBJECT,
                debugPrefix,
                numAlerts,
                alertSubject,
                valuesStr
        );
    }

    private String render(final Parameters params) throws IOException
    {
        final StringWriter writer = localWriter.get();
        writer.getBuffer().setLength(0);

        template.evaluate(writer, params.toMap());
        return writer.toString();
    }

    @Override
    public EmailMessage format(final MessageKit messageKit)
    {
        @SuppressWarnings("unchecked")
        final M view = (M) Views.of(messageKit);

        final long alertId = view.getAlertId();
        final String namespace = view.getNamespace();
        final String subject =
                buildSubject(
                        view.getAlertsTotal(),
                        view.getGroupInterpolatedSubject(),
                        view.getGroupValues()
                );
        final long sampleAlertTimestampMs =
                view.getAllViews().get(0).getTimestampMs();

        final Parameters params = new Parameters()
                .setWebuiLink(
                        Views.get().horizonUrl()
                )
                .setDetailsLink(
                        Views.get().alertSplunkUrl(alertId, sampleAlertTimestampMs)
                )
                .setEditLink(Views.get().alertEditUrl(alertId))
                .setTimestampMs(System.currentTimeMillis())
                .setBody(view.getGroupInterpolatedBody())
                .setNamespace(namespace)
                .setTotal(view.getAlertsTotal())
                .setBad(view.getBadCount())
                .setWarn(view.getWarnCount())
                .setMissing(view.getMissingCount())
                .setRecovery(view.getRecoveryCount())
                .setViewMap(view
                        .getViewsByType().entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                v -> Views.of(v.getKey()),
                                Map.Entry::getValue
                        ))
                );

        final Map<String, byte[]> images = getImages(view);
        if (logoBytes.length != 0) {
            images.put("logo", logoBytes);
        }

        try {
            return EmailMessage.builder()
                    .setSubject(subject)
                    .setFrom(
                            String.format(P_FROM, namespace)
                    )
                    .setFromAlias(
                            String.format(P_FROM_ALIAS, debugPrefix, namespace)
                    )
                    .setBody(render(params))
                    .setImages(images)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
