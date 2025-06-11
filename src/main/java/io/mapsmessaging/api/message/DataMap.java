/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.api.message;

import io.mapsmessaging.api.message.interceptors.FieldInterceptor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataMap extends LinkedHashMap<String, TypedData> {

  private transient Message message;

  public DataMap() {
    super();
  }

  public DataMap(@Nullable Map<String, TypedData> dataMap) {
    super(dataMap);
  }

  protected void setMessage(Message message) {
    this.message = message;
  }

  @Override
  public int hashCode() {
    return super.hashCode() + message.hashCode();
  }

  @Override
  public boolean equals(Object val) {
    if (val instanceof DataMap) {
      return super.equals(val);
    }
    return false;
  }

  @Override
  public TypedData get(Object key) {
    TypedData val = super.get(key);
    if (val == null && message != null) {
      return lookup(message, (String) key);
    }
    return val;
  }

  private TypedData lookup(@NonNull @NotNull Message message, String key) {
    return FieldInterceptor.getInstance().lookup(message, key);
  }

}
