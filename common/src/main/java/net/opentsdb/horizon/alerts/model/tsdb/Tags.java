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
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@ToString
public class Tags implements KryoSerializable {
  protected Map<String, String> dimensions = new HashMap<String, String>();

  // for kryo
  public Tags() {
  }
  
  public Tags(Map<String, String> t) {
	  this.dimensions.putAll(t);
  }




  public Map<String, String> getDimensions() {
    return this.dimensions;
  }


  @Override
  public boolean equals(Object o) {
    if (o instanceof Tags) {
      if (this.dimensions.equals(((Tags)o).dimensions)) {
        return true;
      }
    }
    
    return false;
  }

  @Override
  public void write(Kryo kryo, Output output) {
    output.writeInt(dimensions.size());
    
    for (Entry<String, String> entry : dimensions.entrySet()) {
      output.writeString(entry.getKey());
      output.writeString(entry.getValue());
    }
  }

  @Override
  public void read(Kryo kryo, Input input) {
    int size = input.readInt();
    
    for (int i = 0; i < size; i++) {
      dimensions.put(input.readString(), input.readString());
    }
  } 
  
}
