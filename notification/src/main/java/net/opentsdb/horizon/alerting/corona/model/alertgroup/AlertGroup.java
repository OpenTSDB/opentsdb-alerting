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

package net.opentsdb.horizon.alerting.corona.model.alertgroup;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;

@Getter
public class AlertGroup {

    /* ------------ Fields ------------ */

    private GroupKey groupKey;

    /**
     * @return mutable list of alerts.
     */
    private List<Alert> alerts;

    /* ------------ Constructor ------------ */

    public AlertGroup(final GroupKey groupKey, final List<Alert> alerts)
    {
        Objects.requireNonNull(groupKey, "groupKey cannot be null");
        Objects.requireNonNull(alerts, "alerts cannot be null");
        this.groupKey = groupKey;
        this.alerts = alerts;
    }

    /* ------------ Methods ------------ */

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AlertGroup that = (AlertGroup) o;
        return Objects.equals(groupKey, that.groupKey) &&
                Objects.equals(alerts, that.alerts);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(groupKey, alerts);
    }

    @Override
    public String toString()
    {
        return "AlertGroup{" +
                "groupKey=" + groupKey +
                ", alerts=" + alerts +
                '}';
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private GroupKey key;

        private List<Alert> alerts;

        public Builder setGroupKey(GroupKey key)
        {
            this.key = key;
            return this;
        }

        public Builder setAlerts(List<Alert> alerts)
        {
            this.alerts = alerts;
            return this;
        }

        public Builder setAlerts(Alert... alerts)
        {
            this.alerts = Arrays.asList(alerts);
            return this;
        }

        public AlertGroup build()
        {
            return new AlertGroup(key, alerts);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
