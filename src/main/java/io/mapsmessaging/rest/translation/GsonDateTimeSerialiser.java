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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GsonDateTimeSerialiser implements JsonSerializer<Object> {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Override
  public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
    if (src == null) {
      return context.serialize(null);
    }

    // Handle LocalDateTime specifically
    if (src instanceof LocalDateTime) {
      LocalDateTime localDateTime = (LocalDateTime) src;
      return context.serialize(localDateTime.format(FORMATTER));
    }

    if (src instanceof LocalDate) {
      LocalDate localDate = (LocalDate) src;
      return context.serialize(localDate.format(DATE_FORMATTER));
    }

    // Handle other types explicitly if needed
    if (src instanceof String || src instanceof Number || src instanceof Boolean) {
      return context.serialize(src);
    }

    // For custom objects, serialize fields into a JsonObject
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("class", src.getClass().getName());
    jsonObject.add("fields", context.serialize(src)); // Serialize object fields

    return jsonObject;
  }
}