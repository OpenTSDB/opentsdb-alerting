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

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.annotations.VisibleForTesting;

@ThreadSafe
class GroupByState<K, V> {

    private final ReadWriteLock lock;

    private Map<K, Queue<V>> groups;

    private final AtomicLong size;

    GroupByState()
    {
        this.lock = new ReentrantReadWriteLock();
        this.groups = new ConcurrentHashMap<>();
        this.size = new AtomicLong();
    }

    /**
     * Adds new entry to the state.
     *
     * @param key   key
     * @param value value
     * @return current state size.
     */
    long add(final K key, final V value)
    {
        final Lock readLock = lock.readLock();
        readLock.lock();
        try {
            groups.computeIfAbsent(
                    key,
                    newKey -> new ConcurrentLinkedQueue<>()
            ).add(value);
            return size.incrementAndGet();
        } finally {
            readLock.unlock();
        }
    }

    @VisibleForTesting
    protected void reset()
    {
        final Lock writeLock = lock.writeLock();
        writeLock.lock();

        try {
            groups.clear();
        } finally {
            writeLock.unlock();
        }
    }

    Map<K, Queue<V>> flush()
    {
        final Lock writeLock = lock.writeLock();
        writeLock.lock();

        try {
            // TODO: Maybe create a new ConcurrentHashMap instead of copying.
            final Map<K, Queue<V>> toReturn = new HashMap<>(groups);
            reset();
            size.set(0);
            return toReturn;
        } finally {
            writeLock.unlock();
        }
    }
}

