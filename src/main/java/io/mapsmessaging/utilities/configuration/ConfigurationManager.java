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

package io.mapsmessaging.utilities.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import io.mapsmessaging.config.ConfigManager;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.configuration.PropertyManager;
import io.mapsmessaging.configuration.consul.ConsulManagerFactory;
import io.mapsmessaging.configuration.consul.ConsulPropertyManager;
import io.mapsmessaging.configuration.file.FileYamlPropertyManager;
import io.mapsmessaging.configuration.yaml.YamlPropertyManager;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.tools.config.schema.RuntimeJsonSchemaGenerator;
import io.mapsmessaging.tools.config.schema.RuntimeJsonSchemaService;
import io.mapsmessaging.tools.config.yaml.RenderMode;
import io.mapsmessaging.tools.config.yaml.YamlWriter;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.mapsmessaging.logging.ServerLogMessages.*;


@SuppressWarnings("java:S6548") // yes it is a singleton
public class ConfigurationManager {

  private static class Holder {
    static final ConfigurationManager INSTANCE = new ConfigurationManager();
  }

  public static ConfigurationManager getInstance() {
    return Holder.INSTANCE;
  }

  private final SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012());
  private final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

  private final List<PropertyManager> propertyManagers;
  private final Map<String, ConfigManager> managerMap;
  private final Map<String, String> configSchemas;

  private PropertyManager authoritative;
  @Setter
  @Getter
  private FeatureManager featureManager;

  private ConfigurationManager() {
    logger.log(ServerLogMessages.PROPERTY_MANAGER_START);
    propertyManagers = new ArrayList<>();
    authoritative = null;
    managerMap = new ConcurrentHashMap<>();
    configSchemas = new ConcurrentHashMap<>();
  }

  public void register(){
    // nothing to do here
  }

  public String[] getKnownManagers(){
    return managerMap.keySet().toArray(new String[0]);
  }

  public ConfigManager getManager(String name){
    return managerMap.get(name);
  }

  public String getSchema(String name) {
    return configSchemas.get(name);
  }

  public void initialise(@NonNull @NotNull String serverId) {
    PropertyManager defaultManager = null;
    if (ConsulManagerFactory.getInstance().isStarted()) {
      defaultManager = processConsulConfig(serverId);
    }
    YamlPropertyManager yamlPropertyManager = new FileYamlPropertyManager();
    propertyManagers.add(yamlPropertyManager);
    for (PropertyManager manager : propertyManagers) {
      manager.load();
    }

    try {
      // We have a consul link but there is no config loaded, so load up the configuration into the
      // consul server to bootstrap the server
      if (defaultManager != null && defaultManager.getProperties().isEmpty()) {
        defaultManager.copy(yamlPropertyManager);
      }
      if (authoritative != null && authoritative.getProperties().isEmpty()) {
        authoritative.copy(defaultManager); // Define the local host
      }
    }
    catch(IOException th){
      logger.log(CONSUL_CLIENT_EXCEPTION, th);
    }
  }

  public @Nullable <T extends ConfigManager> T getConfiguration(Class<T> clazz) {
    synchronized (managerMap) {
      if (managerMap.isEmpty()) {
        loadAll();
      }
    }
    Object config = managerMap.get(clazz.getSimpleName());
    if (clazz.isInstance(config)) {
      return clazz.cast(config);
    }
    return null;
  }
  
  public @NonNull @NotNull ConfigurationProperties getProperties(String name) {
    if (authoritative != null && authoritative.contains(name)) {
      logger.log(PROPERTY_MANAGER_LOOKUP, name, "Main");
      return authoritative.getProperties(name);
    }

    for (PropertyManager manager : propertyManagers) {
      if ( manager.contains(name)) {
        ConfigurationProperties props = manager.getProperties(name);
        String schema = configSchemas.get(name);
        Map<String, Object> raw = props.getMap();

        List<String> errors = validate(schema, raw);
        if(!errors.isEmpty()){
          for(String error:errors){
            logger.log(PROPERTY_MANAGER_ENTRY_SCHEMA_ERROR, name, error);
          }
        }
        logger.log(PROPERTY_MANAGER_LOOKUP, name, "Backup");
        return props;
      }
    }
    logger.log(PROPERTY_MANAGER_LOOKUP_FAILED, name);
    return new ConfigurationProperties(new HashMap<>());
  }

  public void saveConfiguration(String configName, ConfigurationProperties config) throws IOException {
    ConfigurationProperties newConfig = new ConfigurationProperties();
    newConfig.put(configName, config);
    if(authoritative != null && authoritative.contains(configName)){
      authoritative.store("./conf", configName);
    }
    else{
      for (PropertyManager manager : propertyManagers) {
        if ( manager.contains(configName)) {
          logger.log(PROPERTY_MANAGER_LOOKUP, configName, "Backup");
          manager.update("./conf", configName, newConfig);
        }
      }
    }
  }

  private PropertyManager processConsulConfig(String serverId){
    String configPath = ConsulManagerFactory.getInstance().getPath();
    if (configPath == null) {
      configPath = "/";
    }
    String defaultName = "default";
    if (!configPath.endsWith("/")) {
      configPath = configPath + File.separator;
    }
    serverId = configPath + serverId;
    defaultName = configPath + defaultName;
    authoritative = new ConsulPropertyManager(serverId);
    authoritative.load();
    String locatedDefault = authoritative.scanForDefaultConfig(configPath);
    if(!locatedDefault.isEmpty()){
      defaultName = locatedDefault;
    }

    PropertyManager defaultManager = new ConsulPropertyManager(defaultName);
    defaultManager.load();
    propertyManagers.add(defaultManager);
    return defaultManager;
  }

  public void loadAll(){
    RuntimeJsonSchemaGenerator generator = new RuntimeJsonSchemaGenerator();
    RuntimeJsonSchemaService service = new RuntimeJsonSchemaService(generator);
    configSchemas.putAll(service.generateAllSchemas());
    ServiceLoader<ConfigManager> configManagers = ServiceLoader.load(ConfigManager.class);
    for(ConfigManager manager : configManagers){
      ConfigManager loaded = manager.load(featureManager);
      try {
        try(FileOutputStream fileOutputStream = new FileOutputStream(loaded.getName()+".yaml")) {
          String schemaString = this.getSchema(loaded.getName());
          JsonObject schemaObject = JsonParser.parseString(schemaString).getAsJsonObject();
          fileOutputStream.write(YamlWriter.toYaml(loaded, schemaObject, RenderMode.FULL).getBytes());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      managerMap.put(loaded.getClass().getSimpleName(), loaded);
    }
  }

  private List<String> validate(String stringSchema, Map<String, Object> raw) {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode instance = mapper.valueToTree(raw);
    JsonNode jsonSchema = mapper.valueToTree(stringSchema);

    JsonNode effectiveSchema = resolveTopLevelRef(jsonSchema);

    Schema schema = schemaRegistry.getSchema(effectiveSchema);

    List<Error> errors = schema.validate(instance);

    return errors.stream()
        .map(Error::getMessage)
        .collect(Collectors.toList());
  }

  private JsonNode resolveTopLevelRef(JsonNode schemaRoot) {
    JsonNode refNode = schemaRoot.get("$ref");
    if (refNode == null || !refNode.isTextual()) {
      return schemaRoot;
    }

    JsonNode resolved = resolveRef(schemaRoot, schemaRoot);
    if (resolved == null || !resolved.isObject()) {
      return schemaRoot;
    }

    ObjectNode merged = ((ObjectNode) resolved).deepCopy();

    JsonNode defs = schemaRoot.get("$defs");
    if (defs != null && defs.isObject()) {
      merged.set("$defs", defs);
    }

    JsonNode schemaDecl = schemaRoot.get("$schema");
    if (schemaDecl != null) {
      merged.set("$schema", schemaDecl);
    }

    JsonNode id = schemaRoot.get("$id");
    if (id != null) {
      merged.set("$id", id);
    }

    return merged;
  }

  private JsonNode resolveRef(JsonNode schemaRoot, JsonNode node) {
    JsonNode ref = node.get("$ref");
    if (ref == null || !ref.isTextual()) {
      return node;
    }

    String refText = ref.asText();
    if (!refText.startsWith("#/")) {
      return node;
    }

    String[] parts = refText.substring(2).split("/");
    JsonNode current = schemaRoot;

    for (String part : parts) {
      current = current.get(part);
      if (current == null) {
        return node;
      }
    }

    return current;
  }

}
