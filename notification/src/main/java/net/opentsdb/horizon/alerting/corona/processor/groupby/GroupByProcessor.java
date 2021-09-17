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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.component.Pair;
import net.opentsdb.horizon.alerting.corona.processor.ChainableProcessor;
import net.opentsdb.horizon.alerting.corona.component.DaemonThreadFactory;

public class GroupByProcessor
        extends ChainableProcessor<Pair<GroupKey, Alert>, AlertGroup>
{

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(GroupByProcessor.class);

    /* ------------ Fields ------------ */

    private final long flushFrequencyMs;

    private final ScheduledExecutorService executor;

    private final GroupByState<GroupKey, Alert> groupbyState;

    /* ------------ Constructor ------------ */

    GroupByProcessor(final Builder<?> builder)
    {
        super(builder);
        if (builder.flushFrequencyMs <= 0) {
            throw new IllegalArgumentException(
                    "flushFrequencyMs cannot be <= 0, given "
                            + builder.flushFrequencyMs);
        }
        if (builder.numWorkers <= 0) {
            throw new IllegalArgumentException(
                    "numWorkers cannot be <= 0, given " + builder.numWorkers);
        }

        this.flushFrequencyMs = builder.flushFrequencyMs;
        this.executor =
                new ScheduledThreadPoolExecutor(
                        builder.numWorkers,
                        DaemonThreadFactory.INSTANCE
                );
        this.groupbyState = new GroupByState<>();
    }

    private List<AlertGroup> toAlertGroups(
            final Map<GroupKey, Queue<Alert>> groups)
    {
        return groups.entrySet().stream()
                .map(e ->
                        new AlertGroup(
                                e.getKey(),
                                new ArrayList<>(e.getValue())
                        )
                )
                .collect(Collectors.toList());
    }

    private void timedSubmit(final AlertGroup group)
    {
        final long start = System.currentTimeMillis();

        {
            submit(group);
        }

        final long end = System.currentTimeMillis();
        AppMonitor.get().timeGroupByFlushItemLatencyMs(end - start);
    }

    /**
     * Flushes groupby state.
     */
    private void flushState()
    {
        final long start = System.currentTimeMillis();

        final Map<GroupKey, Queue<Alert>> groupedAlerts =
                groupbyState.flush();
        final List<AlertGroup> alertGroups =
                toAlertGroups(groupedAlerts);
        for (final AlertGroup group : alertGroups) {
            try {
                timedSubmit(group);
                AppMonitor.get().gaugeGroupByAlertGroupSize(
                        group.getAlerts().size(),
                        group.getGroupKey().getNamespace(),
                        group.getGroupKey().getAlertId()
                );
            } catch (Exception e) {
                AppMonitor.get().countGroupBySubmitFailed();
                LOG.error("Failed to submit: alert_id={}, group_key={}, alerts={}",
                        group.getGroupKey().getAlertId(), group.getGroupKey(), group.getAlerts(), e);
            }
        }

        final long end = System.currentTimeMillis();
        AppMonitor.get().timeGroupByFlushTotalLatencyMs(end - start);
    }

    /**
     * Schedules state flushing tasks at the given interval with initial
     * delay calculated by the formula below.
     *
     * <pre>{@code
     *      flushFrequencyMs - (currentTimeMillis() % flushFrequencyMs)
     * }</pre>
     *
     * @return {@link ScheduledFuture}
     */
    public ScheduledFuture<?> start()
    {
        final long now = System.currentTimeMillis();
        final long delay = flushFrequencyMs - (now % flushFrequencyMs);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                this::flushState,
                delay,
                flushFrequencyMs,
                TimeUnit.MILLISECONDS
        );

        LOG.info("Started at fixed rate: delayMs = {}, periodMs = {}",
                delay, flushFrequencyMs);

        return future;
    }

    /**
     * Stops the executor.
     * <p>
     * Completion of already submitted jobs is not guaranteed.
     * <p>
     * TODO: Maybe use {@link ScheduledExecutorService#awaitTermination(long, TimeUnit)}.
     */
    public void stop()
    {
        executor.shutdown();
    }

    /**
     * Stops the executor and waits up to defined time for tasks to finish.
     *
     * @param timeoutSec maximum wait timeout in seconds.
     * @throws InterruptedException if interrupted while waiting.
     */
    public void stop(final long timeoutSec) throws InterruptedException
    {
        if (!executor.isShutdown()) {
            executor.shutdown();
            executor.awaitTermination(timeoutSec, TimeUnit.SECONDS);
        }
    }

    /**
     * @param pair pair of {@link GroupKey} and {@link Alert} added to the
     *             GroupBy state.
     */
    @Override
    public void process(final Pair<GroupKey, Alert> pair)
    {
        final long size = groupbyState.add(pair.getKey(), pair.getValue());
        AppMonitor.get().gaugeGroupByStateSize(size);
    }

    /* ------------ Builder ------------ */

    public abstract static class Builder<B extends Builder<B>>
            extends ChainableProcessor.Builder<AlertGroup, B>
    {

        private long flushFrequencyMs;

        private int numWorkers;

        /**
         * Sets frequency of state flushes. Initial delay is computed as
         * <code>
         * flushFrequencyMs - (currentTimeMillis() % flushFrequencyMs)
         * </code>
         *
         * @param flushFrequencyMs state flush frequency in ms
         * @return builder
         */
        public B setFlushFrequencyMs(final long flushFrequencyMs)
        {
            this.flushFrequencyMs = flushFrequencyMs;
            return self();
        }

        /**
         * Sets number of workers for the {@link ScheduledExecutorService}.
         *
         * @param numWorkers number of workers
         * @return builder
         */
        public B setNumWorkers(final int numWorkers)
        {
            this.numWorkers = numWorkers;
            return self();
        }

        /**
         * Build the {@link GroupByProcessor}.
         *
         * @return {@link GroupByProcessor}
         */
        public GroupByProcessor build()
        {
            return new GroupByProcessor(this);
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
