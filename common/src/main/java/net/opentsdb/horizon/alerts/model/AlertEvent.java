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

package net.opentsdb.horizon.alerts.model;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import net.opentsdb.horizon.alerts.enums.AlertState;

import java.util.HashMap;
import java.util.Map;

public abstract class AlertEvent implements KryoSerializable, MonitorEvent {

    private String namespace = null;

    //private String application = null;

    private long alertId;

    private long alertRaisedtimestamp = 0l;

    private Map<String,String> tags = null;

    private Map<String,String> additionalProperties = new HashMap<>();

    private AlertState origin = null;

    private AlertState current = null;

    private long alertHash;

    private String alertDetails;

    private boolean isNag;

    private boolean isSnoozed;

    public AlertEvent() {

    }


    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }



    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public AlertState getSignal() {
        return current;
    }

    public void setCurrentSignal(AlertState current) {
        this.current = current;
    }

    public long getAlertRaisedTimestamp() {
        return alertRaisedtimestamp;
    }

    public void setAlertRaisedTimestamp(long timestamp) {
        this.alertRaisedtimestamp = timestamp;
    }

    public String getAlertDetails() {
        return alertDetails;
    }

    public void setAlertDetails(String alertDetails) {
        this.alertDetails = alertDetails;
    }

    public long getAlertId() {
        return alertId;
    }

    public void setAlertId(long alertId) {
        this.alertId = alertId;
    }

    public long getAlertHash() {
        return alertHash;
    }

    public void setAlertHash(long alertHash) {
        this.alertHash = alertHash;
    }

    public AlertState getOriginSignal() {
        return origin;
    }

    public void setOriginSignal(AlertState origin) {
        this.origin = origin;
    }

    public boolean isNag() {
        return isNag;
    }

    public void setNag(boolean nag) {
        isNag = nag;
    }

    public boolean isSnoozed() {
        return isSnoozed;
    }

    public void setSnoozed(boolean snoozed) {
        isSnoozed = snoozed;
    }

    @Override
    public void write(Kryo kryo, Output output) {

        output.writeLong(alertRaisedtimestamp);

        output.writeString(namespace);
        output.writeString(alertDetails);
        output.writeByte(origin.getId());
        output.writeByte(current.getId());
        output.writeBoolean(isNag);
        output.writeBoolean(isSnoozed);
        output.writeLong(alertId);

        output.writeShort(tags.size());

        for(Map.Entry<String, String> entry : tags.entrySet()) {
            output.writeString(entry.getKey());
            output.writeString(entry.getValue());
        }

        output.writeShort(additionalProperties.size());

        for(Map.Entry<String, String> entry : additionalProperties.entrySet()) {
            output.writeString(entry.getKey());
            output.writeString(entry.getValue());
        }


    }

    @Override
    public void read(Kryo kryo, Input input) {
        alertRaisedtimestamp = input.readLong();
        namespace = input.readString();
        alertDetails = input.readString();
        origin = AlertState.fromId(input.readByte());
        current = AlertState.fromId(input.readByte());;
        isNag = input.readBoolean();
        isSnoozed = input.readBoolean();
        alertId = input.readLong();
        int size = input.readShort();
        tags = new HashMap<>(size);

        for (int i = 0; i < size; i++) {
            tags.put(input.readString(), input.readString());
        }

        size = input.readShort();
        additionalProperties = new HashMap<>(size);

        for (int i = 0; i < size; i++) {
            additionalProperties.put(input.readString(), input.readString());
        }
    }

    @Override
    public String toString() {

        return String.format("[namespace: %s , alertId: %s, alertType: %s, origin: %s   timestamp: %s , tags: %s alertDetails: %s addtion: %s snooze: %s]"
                            , namespace, alertId,current.name(), origin.name(), alertRaisedtimestamp, tags,alertDetails, additionalProperties.toString(), isSnoozed);

    }


}
