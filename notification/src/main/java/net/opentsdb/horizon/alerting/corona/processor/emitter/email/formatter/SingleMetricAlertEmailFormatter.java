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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.opentsdb.horizon.alerting.corona.model.alert.impl.SingleMetricAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.emitter.email.Plotter;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.ViewType;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricMessageKitView;

public class SingleMetricAlertEmailFormatter
        extends AbstractEmailFormatter<
        SingleMetricAlert,
        SingleMetricAlertView,
        SingleMetricMessageKitView
        >
{

    /* ------------ Constants ------------ */
    private static final Logger LOG =
            LoggerFactory.getLogger(SingleMetricAlertEmailFormatter.class);

    private static final Plotter PLOTTER = new Plotter();

    /* ------------ Constructor ------------ */

    public SingleMetricAlertEmailFormatter(final String debugPrefix)
    {
        super(debugPrefix, "templates/email-single-metric-alert.html");
    }

    /* ------------ Methods ------------ */

    private byte[] getImgBytes(final long[] timestampsSec,
                               final double[] values,
                               final double threshold)
    {
        return PLOTTER.plotPNG(timestampsSec, values, threshold);
    }

    private Optional<byte[]> generateImage(final SingleMetricAlertView view)
    {
        try {
            final byte[] img =
                    getImgBytes(
                            view.getTimestampsSec(),
                            view.getDisplayValues(),
                            view.getThreshold()
                    );
            return Optional.of(img);
        } catch (Exception e) {
            LOG.error("Failed to plot an image {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    protected Map<String, byte[]> generateImages(
            final SingleMetricMessageKitView messageKit)
    {
        final Map<ViewType, List<SingleMetricAlertView>> viewsByState =
                messageKit.getViewsByType();
        final Map<String, byte[]> images = new HashMap<>();

        viewsByState.forEach((unused, views) -> {
            views.forEach(view -> {
                final Optional<byte[]> optional = generateImage(view);
                final String cid;
                if (optional.isPresent()) {
                    final byte[] img = optional.get();
                    cid = String.valueOf(Arrays.hashCode(img));
                    images.put(cid, img);
                } else {
                    cid = EMPTY_STRING;
                }

                view.addProperty("cid", cid);
            });
        });

        return images;
    }

}
