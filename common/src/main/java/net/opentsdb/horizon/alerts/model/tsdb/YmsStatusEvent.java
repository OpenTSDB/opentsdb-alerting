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


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mridul on 5/6/15.
 */
public class YmsStatusEvent extends YmsEvent {

    // TODO bar serialization is failing because of this
    // we don't use the map so removing it for now
    //@JsonProperty("event_map")
    //private transient Map<String,YmsEvent> eventMap = new HashMap<>();

    @Override
    public YmsStatusEvent createEmpty() {
        return new YmsStatusEvent();
    }

    public void setEventMap(String key, YmsEvent value){
        //this.eventMap.put(key, value);
    }

    public Map<String,YmsEvent> getEventMap(){
        return new HashMap<>();//this.eventMap;
    }

    @Override
    public void write(Kryo kryo, Output output) {
        super.write(kryo,output);
        /*output.writeShort(eventMap.size());
        for (Map.Entry<String, YmsEvent> entry : eventMap.entrySet()) {
            output.writeString(entry.getKey());
            kryo.writeObject(output, entry.getValue());
        }*/
    }

    @Override
    public void read(Kryo kryo, Input input) {
        super.read(kryo,input);
        /*int size = input.readShort();
        for (int i = 0; i < size; i++) {
            eventMap.put(input.readString(), kryo.readObject(input,YmsEvent.class));
        }*/
    }

    @Override
    public String toString() {
        return super.toString();
    }

}

