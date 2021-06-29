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

package net.opentsdb.horizon.alerts.state.persistence;

import com.esotericsoftware.kryo.io.Output;

interface Serializer {

    /**
     * Indicates if {@link #writeHeader(Output)} should be called.
     *
     * @return true if a header should be written.
     */
    boolean hasHeader();

    /**
     * Should write information only needed for initialization
     * of the alert state store.
     *
     * @param output output buffer.
     */
    void writeHeader(Output output);

    /**
     * Indicates if {@link #writeNext(Output)} should be called.
     *
     * @return true if a serialization is not complete.
     */
    boolean hasNext();

    /**
     * Should write state information or chunks of it.
     *
     * @param output output buffer.
     */
    void writeNext(Output output);

    /**
     * Indicates if {@link #writeFooter(Output)} should be called.
     *
     * @return true if a footer should be written.
     */
    boolean hasFooter();

    /**
     * Should write information only needed for finalizing the
     * alert state store.
     *
     * @param output output buffer.
     */
    void writeFooter(Output output);

    /**
     * Count of items serialized
     * @return
     */
    int getSerializedCount();
}
