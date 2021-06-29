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

import java.util.Iterator;

import net.opentsdb.horizon.alerts.state.AlertStateEntry;

import com.esotericsoftware.kryo.io.Output;

public class BatchSerializer implements Serializer {

    private static final int DEFAULT_BATCH_SIZE = 5;

    private final int batchSize;

    private final Iterator<AlertStateEntry> it;

    private int totalCount;

    public BatchSerializer(final int batchSize,
                           final Iterable<AlertStateEntry> stateStore) {
        this.batchSize = batchSize <= 0 ? Integer.MAX_VALUE : batchSize;
        this.it = stateStore.iterator();
        this.totalCount = 0;
    }

    public BatchSerializer(final Iterable<AlertStateEntry> stateStore) {
        this(DEFAULT_BATCH_SIZE, stateStore);
    }

    @Override
    public boolean hasHeader() {
        return false;
    }

    @Override
    public void writeHeader(Output output) {

    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public void writeNext(Output output) {
        int count = 0;
        while (it.hasNext()) {
            if (count >= batchSize) {
                break;
            }
            count++;
            totalCount++;
            output.writeBoolean(true);
            AlertStateEntrySerDe.write(output, it.next());
        }

        // Close the batch.
        output.writeBoolean(false);
    }

    @Override
    public boolean hasFooter() {
        return true;
    }

    @Override
    public void writeFooter(Output output) {
        output.writeInt(totalCount);
    }

    @Override
    public int getSerializedCount() {return totalCount;}
}
