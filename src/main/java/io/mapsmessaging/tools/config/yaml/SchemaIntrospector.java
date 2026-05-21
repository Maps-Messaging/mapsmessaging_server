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

package io.mapsmessaging.tools.config.yaml;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class SchemaIntrospector {

  public SchemaDoc extract(JsonObject schemaNode) {
    SchemaDoc schemaDoc = new SchemaDoc();

    schemaDoc.setTitle(getString(schemaNode, "title"));
    schemaDoc.setDescription(getString(schemaNode, "description"));

    schemaDoc.setFormat(getString(schemaNode, "format"));
    schemaDoc.setPattern(getString(schemaNode, "pattern"));

    schemaDoc.setDefaultValue(getRaw(schemaNode, "default"));

    schemaDoc.setMinimum(getRaw(schemaNode, "minimum"));
    schemaDoc.setMaximum(getRaw(schemaNode, "maximum"));
    schemaDoc.setExclusiveMinimum(getRaw(schemaNode, "exclusiveMinimum"));
    schemaDoc.setExclusiveMaximum(getRaw(schemaNode, "exclusiveMaximum"));

    schemaDoc.setMinLength(getRaw(schemaNode, "minLength"));
    schemaDoc.setMaxLength(getRaw(schemaNode, "maxLength"));

    schemaDoc.setMultipleOf(getRaw(schemaNode, "multipleOf"));

    schemaDoc.setAllowedValues(readEnum(schemaNode));

    return schemaDoc;
  }

  private List<String> readEnum(JsonObject schemaNode) {
    if (schemaNode == null) {
      return null;
    }
    if (!schemaNode.has("enum")) {
      return null;
    }
    JsonElement enumElement = schemaNode.get("enum");
    if (enumElement == null || !enumElement.isJsonArray()) {
      return null;
    }

    JsonArray enumArray = enumElement.getAsJsonArray();
    if (enumArray.isEmpty()) {
      return null;
    }

    List<String> values = new ArrayList<>();
    for (JsonElement element : enumArray) {
      if (element == null || element.isJsonNull()) {
        continue;
      }
      if (element.isJsonPrimitive()) {
        values.add(element.getAsString());
      } else {
        values.add(element.toString());
      }
    }
    return values;
  }

  private String getString(JsonObject schemaNode, String key) {
    if (schemaNode == null || !schemaNode.has(key)) {
      return null;
    }
    JsonElement element = schemaNode.get(key);
    if (element == null || element.isJsonNull()) {
      return null;
    }
    if (!element.isJsonPrimitive()) {
      return element.toString();
    }
    return element.getAsString();
  }

  private String getRaw(JsonObject schemaNode, String key) {
    if (schemaNode == null || !schemaNode.has(key)) {
      return null;
    }
    JsonElement element = schemaNode.get(key);
    if (element == null || element.isJsonNull()) {
      return null;
    }
    return element.toString();
  }
}