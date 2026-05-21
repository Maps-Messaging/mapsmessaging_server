/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.n2k.msg.source;



import io.mapsmessaging.canbus.j1939.n2k.codec.FieldValueSource;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractAisFieldValueSource implements FieldValueSource {

  private final Map<String, Long> longValues;
  private final Map<String, Double> doubleValues;
  private final Map<String, String> stringValues;

  protected AbstractAisFieldValueSource() {
    this.longValues = new HashMap<>();
    this.doubleValues = new HashMap<>();
    this.stringValues = new HashMap<>();
  }

  @Override
  public boolean has(String fieldId) {
    return longValues.containsKey(fieldId)
        || doubleValues.containsKey(fieldId)
        || stringValues.containsKey(fieldId);
  }

  @Override
  public Long getLong(String fieldId) {
    return longValues.get(fieldId);
  }

  @Override
  public Double getDouble(String fieldId) {
    return doubleValues.get(fieldId);
  }

  @Override
  public String getString(String fieldId) {
    return stringValues.get(fieldId);
  }

  protected void putLong(String key, Long value) {
    if (value != null) {
      longValues.put(key, value);
    }
  }

  protected void putDouble(String key, Double value) {
    if (value != null) {
      doubleValues.put(key, value);
    }
  }

  protected void putString(String key, String value) {
    if (value != null && !value.isEmpty()) {
      stringValues.put(key, value);
    }
  }
}