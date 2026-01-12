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


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.config.ConfigManager;

import java.lang.reflect.Type;
import java.util.Map;

public final class YamlWriter {

  private static final String VERSION_FIELD = "schemaLoadingVersion";
  private static final Gson gson = new GsonBuilder()
      .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
      .create();

  private static final VersionFieldStripper versionFieldStripper = new VersionFieldStripper(VERSION_FIELD);

  private static final SchemaResolver schemaResolver = new SchemaResolver();
  private static final SchemaIntrospector schemaIntrospector = new SchemaIntrospector();
  private static final YamlCommentEmitter yamlCommentEmitter = new YamlCommentEmitter();
  private static final YamlValueFormatter yamlValueFormatter = new YamlValueFormatter();
  private static final YamlNodeRenderer yamlNodeRenderer = new YamlNodeRenderer(schemaResolver, schemaIntrospector, yamlCommentEmitter, yamlValueFormatter);
  private static final YamlHeaderWriter yamlHeaderWriter = new YamlHeaderWriter();

  private YamlWriter() {
  }

  public static String toYaml(ConfigManager object, JsonObject jsonSchema, RenderMode renderMode) {
    Map<String, Object> map = toMap(object);
    versionFieldStripper.removeVersionFromChildrenOnly(map);

    StringBuilder builder = new StringBuilder(8192);

    for (String headerLine : yamlHeaderWriter.buildHeaderLines()) {
      builder.append("# ").append(headerLine).append("\n");
    }
    builder.append("\n");
    builder.append(yamlNodeRenderer.renderRoot(object.getName(), map, jsonSchema, renderMode));
    return builder.toString();
  }

  private static Map<String, Object> toMap(ConfigManager object) {
    String json = gson.toJson(object);
    Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
    return gson.fromJson(json, mapType);
  }
}