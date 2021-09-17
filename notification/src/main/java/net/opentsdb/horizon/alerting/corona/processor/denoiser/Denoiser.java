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

package net.opentsdb.horizon.alerting.corona.processor.denoiser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;

/**
 * The purpose of this class is to fight against alert processing lags.
 * Imagine a situation when alert grouping was stopped for some reason
 * (we had instances where engineers killed containers and didn't trigger
 * their restart). Once we start the grouping again, alerts from different
 * evaluation windows are going to be bundled in the same group. It might
 * happen that the alerts are flapping and there some alerts that can cancel
 * each other.
 *
 * For example, we have a series of alerts for the _same tag set_ having
 * evaluations:
 * WARN   WARN   BAD   BAD   RECOVERY    WARN   BAD
 * ^-------------------------------------^      ^
 * |                                            This is still valuable.
 * Do you think people care about these?
 * It is considered noise at this time.
 *
 * This class, should drop every alert, except for the last one (in the timeline order).
 */
public class Denoiser extends ChainableProcessor<MessageKit, MessageKit> {

    private static final Logger LOG = LoggerFactory.getLogger(Denoiser.class);

    private final String emitterType;

    protected Denoiser(Builder<?> builder) {
        super(builder);
        Objects.requireNonNull(builder.emitterType, "emitterType cannot be null");
        this.emitterType = builder.emitterType;
    }

    @Override
    public void process(MessageKit messageKit) {
        AlertGroup alertGroup = messageKit.getAlertGroup();
        List<Alert> alerts = alertGroup.getAlerts();
        if (alerts == null || alerts.isEmpty()) {
            // Do nothing.
            return;
        }

        // Separate fresh alerts from the ones to be discarded.
        List<Alert> discarded = new ArrayList<>(alerts.size());
        List<Alert> kept = new ArrayList<>(alerts.stream()
                .collect(Collectors.toMap(Alert::getTags, Function.identity(), (a1, a2) -> {
                    // Merge policy: keep the latest alert.
                    final Alert keep, discard;
                    if (a1.getTimestampSec() < a2.getTimestampSec()) {
                        keep = a2;
                        discard = a1;
                    } else {
                        keep = a1;
                        discard = a2;
                    }
                    discarded.add(discard);
                    return keep;
                }))
                .values());

        if (discarded.isEmpty()) {
            // Nothing was discarded, we are ok with the old message kit.
            submit(messageKit);
        } else {
            LOG.info("Discarding stale alerts: emitter={}, alert_id={}, group_key={}, discarded_alerts={}",
                    emitterType, messageKit.getAlertId(), alertGroup.getGroupKey(), discarded);
            AppMonitor.get().countOldAlertsDiscarded(discarded.size(),
                    emitterType, messageKit.getNamespace(), messageKit.getAlertId());

            // Construct a new message kit containing up-to-date alerts only.
            MessageKit newMessageKit = MessageKit.builder()
                    .setAlertGroup(AlertGroup.builder()
                            .setGroupKey(alertGroup.getGroupKey())
                            .setAlerts(kept)
                            .build())
                    .setContacts(messageKit.getContacts())
                    .setMeta(messageKit.getMeta())
                    .setType(messageKit.getType())
                    .build();
            submit(newMessageKit);
        }
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends ChainableProcessor.Builder<MessageKit, B> {

        private String emitterType;

        public B setEmitterType(String emitterType) {
            this.emitterType = emitterType;
            return self();
        }

        public Denoiser build() {
            return new Denoiser(this);
        }
    }

    private static class BuilderImpl extends Builder<BuilderImpl> {

        @Override
        protected BuilderImpl self() {
            return this;
        }
    }

    public static Builder<?> builder() {
        return new BuilderImpl();
    }
}
