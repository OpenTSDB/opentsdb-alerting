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

package net.opentsdb.horizon.alerting.corona.component;

import java.util.Objects;

import lombok.Getter;

/**
 * A container for a pair of values.
 *
 * @param <K> key type
 * @param <V> value type
 * @author skhegay
 */
public class Pair<K, V> {

    @Getter
    private final K key;

    @Getter
    private final V value;

    public Pair(final K key, final V value)
    {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(key, pair.key) &&
                Objects.equals(value, pair.value);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(key, value);
    }

    @Override
    public String toString()
    {
        return "Pair{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
