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

package io.mapsmessaging.engine.schema;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import io.mapsmessaging.schemas.config.impl.CsvSchemaConfig;
import io.mapsmessaging.schemas.config.impl.XmlSchemaConfig;
import io.mapsmessaging.utilities.GsonFactory;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public class VersionUpgradeHelper {

  private VersionUpgradeHelper() {}

  public static SchemaConfig convertAndCreate(SchemaManager manager, String topicName, ConfigurationProperties config) {
    if(config.containsKey("uuid")){
      String uuid = config.getProperty("uuid");
      if(uuid != null){
        SchemaConfig schemaConfig = manager.getSchema(uuid);
        if(schemaConfig != null){
          return schemaConfig;
        }
      }
    }
    SchemaConfig schemaConfig = new SchemaConfig();
    schemaConfig.setFormat(config.getProperty("format"));
    schemaConfig.setVersion(config.getProperty("version"));
    schemaConfig.setTitle(config.getProperty("title"));
    schemaConfig.setInterfaceDescription(config.getProperty("interface-description"));
    schemaConfig.setDescription(config.getProperty("description"));
    schemaConfig.setComments(config.getProperty("comments"));
    schemaConfig.setUniqueId(config.getProperty("uuid"));
    schemaConfig.setResourceType(config.getProperty("resource-type"));
    schemaConfig.setMatchExpression(config.getProperty("match-expression", ""));
    schemaConfig.setModifiedAt(OffsetDateTime.now());
    schemaConfig.setCreatedAt(OffsetDateTime.now());
    if(config.containsKey("creation")){
      schemaConfig.setCreatedAt(LocalDateTime.parse(config.getProperty("creation")).atOffset(ZoneOffset.UTC));
    }

    switch(config.getProperty("format", "").toLowerCase()){
      case "csv" -> processCsv(schemaConfig, config);
      case "xml" -> processXml(schemaConfig, config);
      case "json" -> processJson(schemaConfig, config);
      case "avro" -> processAvro(schemaConfig, config);
      case "protobuf" -> processProtoBuf(schemaConfig, config);
    }
    schemaConfig = SchemaConfigFactory.getInstance().constructConfig(schemaConfig); // Create a concrete one
    manager.addSchema(topicName, schemaConfig);
    return schemaConfig;
  }

  private static void processAvro(SchemaConfig schemaConfig, ConfigurationProperties config) {
    String avroSchema = config.getProperty("schema");
    if(avroSchema != null){
      schemaConfig.setSchemaBase64(avroSchema);
    }
  }

  private static void processCsv(SchemaConfig schemaConfig, ConfigurationProperties config) {
    CsvSchemaConfig.CsvConfig csvConfig = new CsvSchemaConfig.CsvConfig();
    csvConfig.setHeaderValues(config.getProperty("header", ""));
    csvConfig.setInterpretNumericStrings(config.getBooleanProperty("numericStrings", true));
    schemaConfig.setSchema(JsonParser.parseString(GsonFactory.getInstance().getSimpleGson().toJson(csvConfig)).getAsJsonObject());
  }

  private static void processJson(SchemaConfig schemaConfig, ConfigurationProperties config) {
    if( config.containsKey("jsonSchema")) {
      Map<String, Object> jsonSchema = ((ConfigurationProperties) config.get("jsonSchema")).getMap();
      JsonElement jsonElement = GsonFactory.getInstance().getSimpleGson().toJsonTree(jsonSchema);
      if (!jsonElement.isJsonObject()) {
        return;
      }
      schemaConfig.setSchema(jsonElement.getAsJsonObject());
    }
  }

  private static void processProtoBuf(SchemaConfig schemaConfig, ConfigurationProperties config) {
    String descriptor = config.getProperty("descriptor");
    String messageName = config.getProperty("messageName");
    if(descriptor != null && !descriptor.isEmpty() && messageName != null && !messageName.isEmpty()){
      JsonObject descriptorJson = new JsonObject();
      descriptorJson.addProperty("descriptor", descriptor);
      descriptorJson.addProperty("messageName", messageName);
      schemaConfig.setSchema(descriptorJson);
    }
  }

  private static void processXml(SchemaConfig schemaConfig, ConfigurationProperties config){
    XmlSchemaConfig.XmlConfig xmlConfig = new XmlSchemaConfig.XmlConfig();
    xmlConfig.setCoalescing(config.getBooleanProperty("coalescing", true));
    xmlConfig.setValidating(config.getBooleanProperty("validating", false));
    xmlConfig.setNamespaceAware(config.getBooleanProperty("namespaceAware", true));
    xmlConfig.setRootEntry(config.getProperty("rootEntry", ""));
    schemaConfig.setSchema(JsonParser.parseString(GsonFactory.getInstance().getSimpleGson().toJson(xmlConfig)).getAsJsonObject());

  }
}
