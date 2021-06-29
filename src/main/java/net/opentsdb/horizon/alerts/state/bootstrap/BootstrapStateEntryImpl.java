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

package net.opentsdb.horizon.alerts.state.bootstrap;

import java.util.Objects;

import net.opentsdb.horizon.alerts.state.AlertStateStore;

class BootstrapStateEntryImpl implements BootstrapStateEntry {

    private final long alertId;

    private final long completeStamp;

    private final AlertStateStore complete;

    private final long incompleteStamp;

    private final AlertStateStore incomplete;

    BootstrapStateEntryImpl(final long alertId,
                            final long completeStamp,
                            final AlertStateStore complete,
                            final long incompleteStamp,
                            final AlertStateStore incomplete) {
        this.alertId = alertId;
        this.completeStamp = completeStamp;
        this.complete = complete;
        this.incompleteStamp = incompleteStamp;
        this.incomplete = incomplete;
    }

    @Override
    public long getAlertId() {
        return alertId;
    }

    @Override
    public boolean hasComplete() {
        return complete != NO_STATE;
    }

    @Override
    public AlertStateStore getComplete() {
        return complete;
    }

    @Override
    public long getCompleteRunStamp() {
        return completeStamp;
    }

    @Override
    public boolean hasIncomplete() {
        return incomplete != NO_STATE;
    }

    @Override
    public AlertStateStore getIncomplete() {
        return incomplete;
    }

    @Override
    public long getIncompleteRunStamp() {
        return incompleteStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BootstrapStateEntryImpl that = (BootstrapStateEntryImpl) o;
        return alertId == that.alertId &&
                completeStamp == that.completeStamp &&
                incompleteStamp == that.incompleteStamp &&
                Objects.equals(complete, that.complete) &&
                Objects.equals(incomplete, that.incomplete);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                alertId,
                completeStamp,
                complete,
                incompleteStamp,
                incomplete
        );
    }

    @Override
    public String toString() {
        return "BootstrapStateEntryImpl{" +
                "alertId=" + alertId +
                ", completeStamp=" + completeStamp +
                ", complete=" + complete +
                ", incompleteStamp=" + incompleteStamp +
                ", incomplete=" + incomplete +
                '}';
    }
}
