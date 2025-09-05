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

package io.mapsmessaging.rest.translation;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GsonDateTimeDeserialiser implements JsonDeserializer<Object> {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
    if (json.isJsonNull()) {
      return null;
    }

    // Handle LocalDateTime specifically
    if (typeOfT == LocalDateTime.class) {
      return LocalDateTime.parse(json.getAsString(), FORMATTER);
    }

    if (typeOfT == LocalDate.class) {
      return LocalDate.parse(json.getAsString(), FORMATTER);
    }


    // Delegate to Gson for other types
    return context.deserialize(json, typeOfT);
  }
}