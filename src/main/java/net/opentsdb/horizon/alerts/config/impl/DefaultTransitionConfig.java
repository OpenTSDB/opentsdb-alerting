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

package net.opentsdb.horizon.alerts.config.impl;

import com.fasterxml.jackson.databind.JsonNode;
import net.opentsdb.horizon.alerts.config.TransitionConfig;
import net.opentsdb.horizon.alerts.enums.AlertState;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Holder for the transitions
 * Will be updated
 */

public class DefaultTransitionConfig implements TransitionConfig {


    private final Map<String,String> transitions = new ConcurrentHashMap<>();
    private final String FORMATTED = "%sTo%s";

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private boolean notifyOnMissing;


    public DefaultTransitionConfig(Iterator<JsonNode> elements, boolean notifyOnMissing) {
        setTransitions(elements);
        this.notifyOnMissing = notifyOnMissing;
    }

    private void setTransitions(Iterator<JsonNode> elements) {
        readWriteLock.writeLock().lock();
        transitions.clear();
        while(elements.hasNext()) {
            final String transition = elements.next().asText();
            final String s = transition.toLowerCase();
            transitions.put(s,s);
        }
        readWriteLock.writeLock().unlock();
    }

    public boolean raiseAlert(AlertState oldState, AlertState newState) {

        if(newState == AlertState.MISSING ||
                (oldState == AlertState.MISSING && newState == AlertState.GOOD)) {
            if(notifyOnMissing) {
                return true;
            } else {
                return false;
            }
        }

        readWriteLock.readLock().lock();
        try {
            final String transition = String.format(FORMATTED,oldState.name(),newState.name()).toLowerCase();
            if(transitions.containsKey(transition)) {
                return true;
            }
            return false;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

}
