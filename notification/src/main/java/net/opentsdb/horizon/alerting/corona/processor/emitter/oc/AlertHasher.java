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

package net.opentsdb.horizon.alerting.corona.processor.emitter.oc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.base.Joiner;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.EventAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.HealthCheckAlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl.SingleMetricAlertView;
import org.apache.commons.codec.binary.Hex;

import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;

public class AlertHasher {

    private static Joiner.MapJoiner COMMA_TAG_JOINER =
            Joiner.on(",").withKeyValueSeparator(":");

    private static final MessageDigest MD5_HASHER;

    static {
        try {
            MD5_HASHER = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String hash(
            final MessageKitView<?, ?> messageKitView,
            final AlertView view)
    {
        final StringBuilder sb = new StringBuilder();
        appendViewPrefixForHash(sb, view);
        sb.append(messageKitView.getAlertId())
                .append(messageKitView.getNamespace());
        COMMA_TAG_JOINER.appendTo(sb, view.getSortedTags());

        final byte[] data =
                MD5_HASHER.digest(
                        sb.toString().getBytes(StandardCharsets.UTF_8)
                );

        return new String(Hex.encodeHex(data));
    }

    private static void appendViewPrefixForHash(
            final StringBuilder sb,
            final AlertView view)
    {
        if (view instanceof SingleMetricAlertView) {
            sb.append(((SingleMetricAlertView) view).getMetric());
        } else if (view instanceof EventAlertView) {
            sb.append(((EventAlertView) view).getDataNamespace());
        } else if (view instanceof HealthCheckAlertView) {
            final HealthCheckAlertView v = (HealthCheckAlertView) view;
            sb.append(v.getDataNamespace())
                    .append(":")
                    .append(v.getApplication());
        }
    }
}
