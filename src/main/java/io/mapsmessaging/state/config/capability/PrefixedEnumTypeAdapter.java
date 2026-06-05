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

package io.mapsmessaging.state.config.capability;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

public class PrefixedEnumTypeAdapter<T extends Enum<T>> implements JsonSerializer<T>, JsonDeserializer<T> {

  private final Class<T> enumClass;
  private final String prefix;

  public PrefixedEnumTypeAdapter(Class<T> enumClass, String prefix) {
    this.enumClass = enumClass;
    this.prefix = prefix;
  }

  @Override
  public JsonElement serialize(
      T value,
      Type type,
      JsonSerializationContext context
  ) {
    if(value == null) {
      return JsonNull.INSTANCE;
    }

    return new JsonPrimitive(prefix + value.name());
  }

  @Override
  public T deserialize(
      JsonElement jsonElement,
      Type type,
      JsonDeserializationContext context
  ) throws JsonParseException {
    if(jsonElement == null || jsonElement.isJsonNull()) {
      return null;
    }

    String value = jsonElement.getAsString();

    if(value.startsWith(prefix)) {
      value = value.substring(prefix.length());
    }

    return Enum.valueOf(enumClass, value);
  }
}