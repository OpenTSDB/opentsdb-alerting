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

package net.opentsdb.horizon.alerts.kafka;

import kafka.utils.VerifiableProperties;

public class KafkaHashBasedPartitioner implements kafka.producer.Partitioner {


    public KafkaHashBasedPartitioner(VerifiableProperties vb) {

    }

    @Override
    public int partition(Object key, int numOfPartitions) {

        int partition = 0;
        String stringKey = (String) key;
        partition = (int)(Math.abs(Long.parseLong(stringKey) %numOfPartitions));
        //The above is Long.parseLong to allow Long values (where Integer.parseInt would fail)
        return partition;
    }
}
