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
import java.util.Map;

import net.opentsdb.horizon.alerting.corona.model.alert.impl.PeriodOverPeriodAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.PeriodOverPeriodMessageKitView;

public class PeriodOverPeriodAlertEmailFormatter
        extends AbstractEmailFormatter<
        PeriodOverPeriodAlert,
        PeriodOverPeriodAlertView,
        PeriodOverPeriodMessageKitView> {

    private static final Logger LOG =
            LoggerFactory.getLogger(PeriodOverPeriodAlertEmailFormatter.class);

    private static final PeriodOverPeriodGraphPlotter PLOTTER =
            new PeriodOverPeriodGraphPlotter();

    public PeriodOverPeriodAlertEmailFormatter(String debugPrefix) {
        super(debugPrefix, "templates/email-period-over-period-alert.html");
    }

    @Override
    protected Map<String, byte[]> generateImages(
            final PeriodOverPeriodMessageKitView messageKit) {
        final Map<String, byte[]> images = new HashMap<>();
        messageKit.getAllViews().forEach(view -> {
            String cid = EMPTY_STRING;
            final byte[] img = generateImage(view);
            if (img != null) {
                cid = String.valueOf(Arrays.hashCode(img));
                images.put(cid, img);
            }
            view.addProperty("cid", cid);
        });

        return images;
    }

    private byte[] generateImage(final PeriodOverPeriodAlertView view) {
        try {
            return PLOTTER.plotPNG(view);
        } catch (Exception e) {
            LOG.error("Failed to plot image {}", e.getMessage());
        }
        return null;
    }
}
