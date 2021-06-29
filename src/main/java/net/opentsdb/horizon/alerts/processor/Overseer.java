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

package net.opentsdb.horizon.alerts.processor;

import net.opentsdb.horizon.alerts.AlertUtils;

/**
 * Class which contains alert state
 */
public class Overseer {


    private volatile long lastRuntimeSecs;

    private volatile long currentRunTimeSecs;

    private volatile int runFrequencyInSecs = 60;


    /**
     * Can build state into this later.
     */
    public Overseer(int runFrequencyInSecs, long lastRuntimeSecs) {
        this.runFrequencyInSecs = runFrequencyInSecs;

        this.lastRuntimeSecs = AlertUtils.getBatchTime(lastRuntimeSecs,runFrequencyInSecs);

        this.currentRunTimeSecs = this.lastRuntimeSecs + runFrequencyInSecs;

        // Set it to the previousBatch From Current time.
    }

    public long startNewRun() {

        if(!AlertUtils.isValidRunTimeSecs(lastRuntimeSecs)) {
            this.lastRuntimeSecs = AlertUtils.
                    getDefaultLastRunTimeSecs(runFrequencyInSecs);
        }

        this.currentRunTimeSecs = lastRuntimeSecs + runFrequencyInSecs;
        return this.currentRunTimeSecs;

    }

    public void endRun() {
        this.lastRuntimeSecs = this.currentRunTimeSecs;
    }

    public long getCurrentRunTimeSecs() {
        return this.currentRunTimeSecs;
    }

    public boolean isTimeToRun() {

        long currTimeInSecs = System.currentTimeMillis()/1000;

        if((currTimeInSecs - lastRuntimeSecs) > runFrequencyInSecs) {
            //System.out.println(currTimeInSecs + " "+ lastRuntimeSecs + " "+ runFrequencyInSecs);
            return true;
        }
        return false;
    }

    public long getLastRuntimeSecs() {
        return lastRuntimeSecs;
    }
}
