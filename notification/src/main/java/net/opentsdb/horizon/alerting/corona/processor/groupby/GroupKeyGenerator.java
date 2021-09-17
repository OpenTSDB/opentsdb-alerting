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

package net.opentsdb.horizon.alerting.corona.processor.groupby;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import net.opentsdb.horizon.alerting.corona.component.Pair;
import net.opentsdb.horizon.alerting.corona.config.MetadataProvider;
import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;

/**
 * Generates a pair of {@code GroupKey} and the {@code Alert} and submits
 * it downstream.
 * <p>
 * The class depends on correct behaviour of the given {@link MetadataProvider}.
 * In case grouping rules cannot be retrieved, the current resolution strategy
 * is to put the alert in a bucket with empty grouping rules.
 * <p>
 * Change the {@link #handleStranded(Alert)} implementation if different
 * strategy is desired, e.g. feeding the alert back to Kafka.
 * <p>
 * Note: Alerts with <b>empty grouping rules are put in the same group</b>.
 */
public class GroupKeyGenerator
        extends ChainableProcessor<Alert, Pair<GroupKey, Alert>>
{

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(GroupKeyGenerator.class);

    /* ------------ Fields ------------ */

    private final MetadataProvider metadataProvider;

    /* ------------ Constructor ------------ */

    private GroupKeyGenerator(final Builder<?> builder)
    {
        super(builder);
        Objects.requireNonNull(builder.metadataProvider,
                "metadataProvider cannot be null");
        this.metadataProvider = builder.metadataProvider;
    }

    /* ------------ Methods ------------ */

    /**
     * Gets tag values for the given tag keys.
     * <p>
     * Open question: what if the tag key is not in the [tags].
     * Currently, the corresponding assigned value is `null`.
     *
     * @param keys array of tag keys
     * @param tags tag key-value map
     * @return array of corresponding tag values
     */
    private String[] getValues(final String[] keys,
                               final Map<String, String> tags)
    {
        final String[] values = new String[keys.length];
        if (tags == null) {
            return values;
        }

        for (int i = 0; i < keys.length; i++) {
            // TODO: Maybe use tags.getOrDefault(keys[i], "_null_");
            values[i] = tags.get(keys[i]);
        }

        return values;
    }

    /**
     * Generates a pair of {@link GroupKey} and {@link Alert}.
     * <p>
     * In the resulting {@code GroupKey} the keys are sorted.
     *
     * @param alert alert for which group is generated
     * @param rules list of group tag keys
     * @return a pair of {@code GroupKey} and {@code Alert}
     */
    private Pair<GroupKey, Alert> generatePair(final Alert alert,
                                               final List<String> rules)
    {
        final String[] keys = rules.stream()
                .sorted()
                .toArray(String[]::new);
        final String[] values = getValues(keys, alert.getTags());

        return new Pair<>(
                GroupKey.builder()
                        .setNamespace(alert.getNamespace())
                        .setAlertId(alert.getId())
                        .setAlertType(alert.getType())
                        .setKeys(keys)
                        .setValues(values)
                        .build(),
                alert
        );
    }

    /**
     * Handles an alert for which grouping rules could not be retrieved.
     * <p>
     * Current strategy is to generate a {@code GroupKey} with empty grouping
     * rules.
     * <p>
     * Change this method to define another resolution strategy, e.g. write
     * back to Kafka.
     *
     * @param alert alert stranded alert
     * @return optional pair
     */
    private Optional<Pair<GroupKey, Alert>> handleStranded(final Alert alert)
    {
        AppMonitor.get().countGroupKeyGeneratorStranded(alert.getId());
        LOG.error("Missing grouping rules for alert: alert_id={}, alert={}",
                alert.getId(), alert);

        final Pair<GroupKey, Alert> pair = generatePair(alert, Collections.emptyList());
        return Optional.of(pair);
    }

    /**
     * Generates a pair of {@link GroupKey} and {@link Alert} and submits
     * it downstream.
     * <p>
     * If {@link #metadataProvider} failed to provide grouping rules, then
     * the alert is handled according to {@link #handleStranded(Alert)}.
     *
     * @param alert alert to generate a {@code GroupKey} for.
     */
    @Override
    public void process(final Alert alert)
    {
        LOG.trace("Got alert: alert_id={}, alert={}", alert.getId(), alert);

        final Optional<List<String>> rules =
                metadataProvider.getGroupingRules(alert.getId());

        final Pair<GroupKey, Alert> pair;
        if (rules.isPresent()) {
            pair = generatePair(alert, rules.get());
        } else {
            final Optional<Pair<GroupKey, Alert>> optional =
                    handleStranded(alert);
            if (optional.isPresent()) {
                pair = optional.get();
            } else {
                return;
            }
        }

        LOG.trace("Generated key for alert: alert_id={}, key={}", alert.getId(), pair.getKey());
        submit(pair);
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends ChainableProcessor.Builder<Pair<GroupKey, Alert>, B>
    {

        private MetadataProvider metadataProvider;

        public B setMetadataProvider(final MetadataProvider metadataProvider)
        {
            this.metadataProvider = metadataProvider;
            return self();
        }

        /**
         * Build the {@link GroupKeyGenerator}.
         *
         * @return {@link GroupKeyGenerator}
         */
        public GroupKeyGenerator build()
        {
            return new GroupKeyGenerator(this);
        }
    }

    private static class BuilderImpl extends Builder<BuilderImpl> {

        @Override
        protected BuilderImpl self()
        {
            return this;
        }
    }

    public static Builder<?> builder()
    {
        return new BuilderImpl();
    }
}