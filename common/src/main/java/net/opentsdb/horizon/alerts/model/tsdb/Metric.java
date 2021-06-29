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

// TODO bar see why kryo serialization is not working for this class
public class Metric implements IMetric {
  protected Double value = 0.0;
  protected String type;
  protected String units;
  
  // for kryo
  public Metric() {
    
  }

  public Metric(double val) {
    value = val;
  }

  public Double getValue() {
    return value;
  }

  public void setValue(Double value) {
    this.value = value;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getUnits() {
    return units;
  }

  public void setUnits(String units) {
    this.units = units;
  }


  @Override
  public String toString() {
    return String.format("Value:%s,  type:%s, units:%s", getValue(), type, units);
  }
  
  @Override
  public boolean equals(Object o) {
    if (o instanceof Metric) {
      if (this.value.equals(((Metric)o).value)) {
        return true;
      }
    }
    
    return false;
  }

  @Override
  public void write(Kryo kryo, Output output) {
      output.writeDouble(value);
  }

  @Override
  public void read(Kryo kryo, Input input) {
    value = input.readDouble();
  }

  @Override
  public MetricType getMetricType() {
      return MetricType.DOUBLE;
  }

}
