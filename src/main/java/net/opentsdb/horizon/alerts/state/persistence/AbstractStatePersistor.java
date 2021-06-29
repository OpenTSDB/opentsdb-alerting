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

import net.opentsdb.horizon.alerts.Monitoring;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.state.AlertStateEntry;

import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStatePersistor implements StatePersistor {

    public static final byte VERSION = (byte) 1;

    @Override
    public void persist(final AlertConfig config,
                        final Iterable<AlertStateEntry> stateStore,
                        final long runStampSec) {

        /*
            TODO: Taking a risk here - will reuse the string array.
        */
        final String[] tagsNamespaceAlertIdConfigType =
                Monitoring.getTagsNamespaceAlertIdConfigType(config);

        final long persistStartTime = System.currentTimeMillis();

        final long alertId = config.getAlertId();
        log.info("id: {} Starting persistence of state", alertId);

        final String namespace = config.getNamespace();
        final Output output = new Output(1024);

        // Save routing prefix.
        final byte[] prefix = getPrefix(output, namespace, alertId, runStampSec);

        // TODO: This is a quick hack to make the serializer thread-safe.
        //       A concrete instance should not be initialized here.
        //       Make it thread local to safe on objects?
        final Serializer serializer = new BatchSerializer(stateStore);

        // Header

        byte[] payload = getHeaderPayload(serializer, prefix, output);
        persist(payload);

        long payloadPersistDeltaSum = 0l;
        long payloadPersistCount = 0l;
        long payloadSize = 0l;
        // State
        while (serializer.hasNext()) {
            payload = getStatePayload(serializer, prefix, output);
            final long start = System.currentTimeMillis();
            persist(payload);
            final long end = System.currentTimeMillis();
            payloadPersistDeltaSum += (end - start);
            payloadPersistCount++;
            payloadSize += payload.length;
        }

        // Footer
        payload = getFooterPayload(serializer, prefix, output);
        persist(payload);
        final long persistEndTime = System.currentTimeMillis();

        reportMetrics(
                payloadPersistDeltaSum,
                payloadPersistCount,
                serializer.getSerializedCount(),
                payloadSize,
                (persistEndTime - persistStartTime),
                tagsNamespaceAlertIdConfigType
        );

        log.info("id: {} Finished persistence of state", alertId);
    }

    private void reportMetrics(final long payloadPersistDeltaSum,
                               final long payloadPersistCount,
                               final long serializedCount,
                               final long payloadSize,
                               final long persistTimeMs,
                               final String[] tagsNamespaceAlertIdConfigType) {
        Monitoring.get()
                .statePersistencePayloadTimeMs(
                        payloadPersistDeltaSum,
                        tagsNamespaceAlertIdConfigType
                );

        Monitoring.get()
                .statePersistencePayloadCount(
                        payloadPersistCount,
                        tagsNamespaceAlertIdConfigType
                );

        Monitoring.get()
                .statePersistenceSerializedCount(
                    serializedCount,
                    tagsNamespaceAlertIdConfigType
                );

        Monitoring.get()
                .statePersistencePayloadSize(
                        payloadSize,
                        tagsNamespaceAlertIdConfigType
                );

        Monitoring.get()
                .statePersistenceTotalTimeMs(
                        persistTimeMs,
                        tagsNamespaceAlertIdConfigType
                );
    }

    private byte[] getPrefix(final Output output,
                             final String namespace,
                             final long alertId,
                             final long runStampSec) {
        output.writeByte(VERSION);
        output.writeLong(alertId);
        output.writeLong(runStampSec);
        output.writeString(namespace);
        return output.toBytes();
    }

    private byte[] getHeaderPayload(final Serializer serializer,
                                    final byte[] prefix,
                                    final Output output) {
        reset(output, prefix, (byte) 1); // header
        if (serializer.hasHeader()) {
            serializer.writeHeader(output);
        }
        return output.toBytes();
    }

    private byte[] getStatePayload(final Serializer serializer,
                                   final byte[] prefix,
                                   final Output output) {
        reset(output, prefix, (byte) 2); // state
        if (serializer.hasNext()) {
            serializer.writeNext(output);
        }
        return output.toBytes();
    }

    private byte[] getFooterPayload(final Serializer serializer,
                                    final byte[] prefix,
                                    final Output output) {
        reset(output, prefix, (byte) 3); // footer
        if (serializer.hasFooter()) {
            serializer.writeFooter(output);
        }
        return output.toBytes();
    }

    private void reset(final Output output,
                       final byte[] prefix,
                       final byte partId) {
        output.setPosition(0);
        output.writeBytes(prefix);
        output.writeByte(partId);
    }

    protected abstract void persist(final byte[] payload);
}
