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

package net.opentsdb.horizon.alerting.corona.processor.emitter.view.impl;

import java.sql.Date;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import net.opentsdb.horizon.alerting.corona.model.alert.Event;
import net.opentsdb.horizon.alerting.corona.model.alert.State;
import net.opentsdb.horizon.alerting.corona.model.alert.impl.EventAlert;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;

public class EventAlertView extends AlertView {

    /* ------------ Fields ------------ */

    private final EventAlert alert;

    /* ------------ Constructors ------------ */

    public EventAlertView(final EventAlert alert)
    {
        super();
        Objects.requireNonNull(alert, "alert cannot be null");
        this.alert = alert;
    }

    /* ------------ Methods ------------ */


    public String getDataNamespace()
    {
        return alert.getDataNamespace();
    }

    public String getFilterQuery()
    {
        return alert.getFilterQuery();
    }

    public int getThreshold()
    {
        return alert.getThreshold();
    }

    public int getWindowSizeSec()
    {
        return alert.getWindowSizeSec();
    }

    public int getCount()
    {
        return alert.getCount();
    }

    public Event getEvent()
    {
        return alert.getEvent();
    }

    @Override
    public String getNamespace()
    {
        return alert.getNamespace();
    }

    @Override
    public long[] getTimestampsSec()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getTimestampMs()
    {
        return alert.getTimestampSec() * 1_000L;
    }

    @Override
    public String getStateFrom()
    {
        return Views.of(alert.getStateFrom());
    }

    @Override
    public String getStateTo()
    {
        return Views.of(alert.getState());
    }

    @Override
    public boolean isSnoozed()
    {
        return alert.isSnoozed();
    }

    @Override
    public SortedMap<String, String> getSortedTags()
    {
        final Map<String, String> tags = alert.getTags();
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySortedMap();
        }

        return Collections.unmodifiableSortedMap(new TreeMap<>(tags));
    }

    @Override
    public Map<String, Object> getProperties()
    {
        return Collections.emptyMap();
    }

    @Override
    public boolean showGraph()
    {
        return false;
    }

    @Override
    public String getHumanTimestamp()
    {
        return DATE_FORMAT.format(new Date(getTimestampMs()));
    }

    @Override
    public boolean isRecovery()
    {
        return alert.getState() == State.GOOD;
    }

    private String getComparator()
    {
        final int count = alert.getCount();
        final int threshold = alert.getThreshold();

        if (count < threshold) {
            return "less than";
        } else if (count == threshold) {
            return "equal to";
        } else {
            return "greater than";
        }
    }

    @Override
    public String getDescription(final String emphasisStart,
                                 final String emphasisStop)
    {
        return String.format(
                "Number of events in %s%s%s filtered by `%s`" +
                        " has been %s%s %d%s in the last %s%d minutes%s",
                emphasisStart,
                alert.getDataNamespace(),
                emphasisStop,
                alert.getFilterQuery(),
                emphasisStart,
                getComparator(),
                getThreshold(),
                emphasisStop,
                emphasisStart,
                alert.getWindowSizeSec() / 60,
                emphasisStop
        );
    }

    @Override
    public boolean equals(final Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        EventAlertView that = (EventAlertView) o;
        return Objects.equals(alert, that.alert);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), alert);
    }

    @Override
    public String toString()
    {
        return "EventAlertView{" +
                "alert=" + alert +
                '}';
    }
}
