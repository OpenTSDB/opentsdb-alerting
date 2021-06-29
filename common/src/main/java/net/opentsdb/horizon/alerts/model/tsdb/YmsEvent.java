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

package net.opentsdb.horizon.alerts.model.tsdb;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import net.opentsdb.horizon.alerts.model.MonitorEvent;
import lombok.ToString;

@ToString
public class YmsEvent extends AbstractYmsEvent implements MonitorEvent {

    protected String sourceHost ="";
    protected Long sendTime = 0l;


    protected List<Datum> data = new ArrayList<>(1); // mostly one datum per event
    protected Map<String, Object> additionalProperties = new HashMap<String, Object>();

    protected boolean replicatedEvent = false;
    protected ProcessingStage generatedByProcessingStage = ProcessingStage.AdditionalProps;

    // for kryo
    public YmsEvent() {

    }

    public YmsEvent createEmpty() {
        return new YmsEvent();
    }

    public void markAsReplicatedEvent() {
        this.replicatedEvent = true;
    }

    public void isReplicatedEvent(final boolean replicatedEvent) {
        this.replicatedEvent = replicatedEvent;
    }

    public String getSourceHost() {
        return sourceHost;
    }

    public void setSourceHost(String sourceHost) {
        this.sourceHost = sourceHost;
    }

    public Long getSendTime() {
        return sendTime;
    }

    public void setSendTime(Long sendTime) {
        this.sendTime = sendTime;
    }

    public List<Datum> getData() {
        return data;
    }

    public Datum getDatum() {
        return (Datum)data.get(0);
    }

    public void setData(List<Datum> data) {
        this.data = data;
    }

    public void setData(Datum data) {
        this.data = new ArrayList<>();
        this.data.add(data);
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public void setGeneratedByProcessingStage(ProcessingStage newStage) {
    /*if (generatedByProcessingStage != ProcessingStage.Null
        && this.generatedByProcessingStage.pastStage(newStage)) {
      throw new AssertionError("Some mess up with the processing stage handling, generatedByProcessingStage:"
          + generatedByProcessingStage  + ", newStage" + newStage);
    }*/

        this.generatedByProcessingStage = newStage;
    }

    public ProcessingStage getGeneratedByProcessingStage() {
        return generatedByProcessingStage;
    }

    public boolean isReplicatedEvent() {
        return this.replicatedEvent;
    }

    public boolean equals(Object o) {
        if (o instanceof YmsEvent) {
            YmsEvent e = (YmsEvent)o;

            if (this.sourceHost.equals(e.sourceHost)
                    && this.sendTime.equals(e.sendTime)
                    && this.data.equals(e.data)
                    && this.additionalProperties.equals(e.additionalProperties)
                    && this.replicatedEvent == e.replicatedEvent
                    && this.generatedByProcessingStage == e.generatedByProcessingStage) {
                return true;
            }
        }

        return false;
    }




    @Override
    public void write(Kryo kryo, Output output) {
        output.writeString(sourceHost);
        output.writeLong(sendTime);
        output.writeBoolean(replicatedEvent);
        output.writeByte(ProcessingStage.ComplexMetric.getId());

        output.writeByte(additionalProperties.size());

        for (Map.Entry<String, Object> entry : additionalProperties.entrySet()) {
            output.writeString(entry.getKey());
            output.writeString(entry.getValue().toString());
        }

        output.writeShort(data.size());
        for (Datum d : data) {
            d.write(kryo, output);
        }

    }

    @Override
    public void read(Kryo kryo, Input input) {
        sourceHost = input.readString();
        sendTime = input.readLong();
        replicatedEvent = input.readBoolean();
        generatedByProcessingStage = ProcessingStage.getById(input.readByte());

        int size = input.readByte();
        additionalProperties = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            additionalProperties.put(input.readString(), input.readString());
        }
        if(generatedByProcessingStage.equals(ProcessingStage.ComplexMetric)) {
            size = input.readShort();
            for (int i = 0; i < size; i++) {
                Datum d = new Datum();
                d.readNew(kryo, input);
                data.add(d);
            }
        } else {
            size = input.readShort();
            for (int i = 0; i < size; i++) {
                Datum d = new Datum();
                d.read(kryo, input);
                data.add(d);
            }
        }
    }

    public Map<String, String> getTags() {
        return getDatum().getTags().getDimensions();
    }

    public Map<String, IMetric> getMetrics() {
        return getDatum().getMetrics();
    }

    public String getApplication() {
        return getDatum().getApplication();
    }

    public String getNamespace() {
        return getDatum().getCluster();
    }

}
