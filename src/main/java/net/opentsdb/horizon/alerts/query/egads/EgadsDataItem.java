/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.query.egads;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.SortedMap;
import java.util.function.Supplier;

import net.opentsdb.horizon.core.builder.CoreBuilder;

/**
 * Response data interface for tags-wise combined 'data' entries.
 */
public interface EgadsDataItem {

    /**
     * Get timestamps for any kind of values, e.g. observed, bounds.
     *
     * @return array of timestamps in seconds, never null.
     */
    long[] getTimestampsSec();

    /**
     * Get last timestamp from the {@link #getTimestampsSec()} array.
     *
     * @return last timestamp in seconds.
     */
    default long getLastTimestampSec() {
        long[] timestamps = getTimestampsSec();
        return timestamps[timestamps.length - 1];
    }

    /**
     * Get observed values.
     *
     * @return array of observed values with the same length as
     * {@link #getTimestampsSec()}.
     */
    double[] getObservedValues();

    default double getLastObservedValue() {
        return getLast(this::getObservedValues);
    }

    /**
     * Get predicted values.
     *
     * @return array of predicted values with the same length as
     * {@link #getTimestampsSec()}.
     */
    double[] getPredictedValues();

    default double getLastPredictedValue() {
        return getLast(this::getPredictedValues);
    }

    /**
     * Get OpenTSDB-evaluated alerts in sorted order.
     *
     * @return sorted alerts, never null.
     */
    EgadsAlert[] getAlerts();

    /**
     * @return the alert for the last timestamp.
     */
    default Optional<EgadsAlert> getLastAlert() {
        EgadsAlert[] egadsAlerts = getAlerts();
        if (egadsAlerts == null || egadsAlerts.length == 0) {
            return Optional.empty();
        }

        final EgadsAlert lastAlert = egadsAlerts[egadsAlerts.length - 1];
        if (lastAlert.getTimestampSec() == getLastTimestampSec()) {
            return Optional.of(lastAlert);
        }
        return Optional.empty();
    }

    // Lower Warn

    Optional<double[]> getLowerWarnValues();

    default boolean hasLowerWarnValues() {
        return getLowerWarnValues().isPresent();
    }

    default OptionalDouble getLastLowerWarnValue() {
        return getOptionalLast(this::getLowerWarnValues);
    }

    // Lower Bad

    Optional<double[]> getLowerBadValues();

    default boolean hasLowerBadValues() {
        return getLowerBadValues().isPresent();
    }

    default OptionalDouble getLastLowerBadValue() {
        return getOptionalLast(this::getLowerBadValues);
    }

    // Upper Warn

    Optional<double[]> getUpperWarnValues();

    default boolean hasUpperWarnValues() {
        return getUpperWarnValues().isPresent();
    }

    default OptionalDouble getLastUpperWarnValue() {
        return getOptionalLast(this::getUpperWarnValues);
    }

    // Upper Bad

    Optional<double[]> getUpperBadValues();

    default boolean hasUpperBadValues() {
        return getUpperBadValues().isPresent();
    }

    default OptionalDouble getLastUpperBadValue() {
        return getOptionalLast(this::getUpperBadValues);
    }

    SortedMap<String, String> getTags();

    /**
     * Get last value from the array.
     *
     * @param doublesSupplier supplier of not-null, not-empty double arrays
     * @return last value.
     */
    static double getLast(Supplier<double[]> doublesSupplier) {
        final double[] vals = doublesSupplier.get();
        return vals[vals.length - 1];
    }

    static OptionalDouble getOptionalLast(Supplier<Optional<double[]>> doublesSupplier) {
        final Optional<double[]> maybeVals = doublesSupplier.get();
        if (maybeVals.isPresent()) {
            return OptionalDouble.of(getLast(maybeVals::get));
        }
        return OptionalDouble.empty();
    }

    interface Builder<B extends Builder<B>>
            extends CoreBuilder<B, EgadsDataItem> {

        B setTimestampsSec(long[] timestampsSec);

        B setObservedValues(double[] observedValues);

        B setPredictedValues(double[] predictedValues);

        B setAlerts(EgadsAlert[] alerts);

        B setUpperWarnValues(double[] upperBoundValues);

        B setUpperBadValues(double[] upperBoundValues);

        B setLowerWarnValues(double[] lowerBoundValues);

        B setLowerBadValues(double[] lowerBoundValues);

        B setTags(SortedMap<String, String> tags);
    }

}
