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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.config.SchemaManagerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.schema.*;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import io.mapsmessaging.schemas.config.SchemaResource;
import io.mapsmessaging.schemas.config.impl.*;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig.TYPE;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.schemas.repository.SchemaRepository;
import io.mapsmessaging.schemas.repository.impl.FileSchemaRepository;
import io.mapsmessaging.schemas.repository.impl.RestSchemaRepository;
import io.mapsmessaging.schemas.repository.impl.SimpleSchemaRepository;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.Map.Entry;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class SchemaManager implements Agent {
  private static final String MONITOR = "monitor";

  public static final UUID DEFAULT_RAW_UUID =              UUID.fromString("10000000-0000-1000-a000-100000000000");
  public static final UUID DEFAULT_NUMERIC_STRING_SCHEMA = UUID.fromString("10000000-0000-1000-a000-100000000001");
  public static final UUID DEFAULT_STRING_SCHEMA =         UUID.fromString("10000000-0000-1000-a000-100000000002");
  public static final UUID DEFAULT_JSON_SCHEMA =           UUID.fromString("10000000-0000-1000-a000-100000000003");
  public static final UUID DEFAULT_XML_SCHEMA =            UUID.fromString("10000000-0000-1000-a000-100000000004");

  private static class Holder {
    static final SchemaManager INSTANCE = new SchemaManager();
  }

  public static SchemaManager getInstance() {
    return Holder.INSTANCE;
  }

  private final SchemaRepository repository;
  private final Map<String, MessageFormatter> loadedFormatter;
  private final Map<String, List<SchemaConfig>> pathMap;

  @Getter
  private long updateCount = 0;


  public SchemaConfig addSchema(String path,  SchemaConfig schemaConfig) {
    if(repository.getResource(schemaConfig.getUniqueId()) != null){
      repository.deleteResource(schemaConfig.getUniqueId());
    }
    SchemaResource resource = repository.createSchema(schemaConfig.getUniqueId(), schemaConfig);
    if(resource.getDefaultVersion() == null){
      resource.setDefaultVersion(schemaConfig);
    }
    List<SchemaConfig> list = pathMap.computeIfAbsent(path, k -> new ArrayList<>());
    list.add(schemaConfig);
    try {
      if (!loadedFormatter.containsKey(schemaConfig.getUniqueId())) {
        MessageFormatter messageFormatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
        loadedFormatter.put(schemaConfig.getUniqueId(), messageFormatter);
      }
    } catch (Exception e) {
      // Unable to load the formatter
    }
    updateCount++;
    return resource.getDefaultVersion();
  }

  public void stop() {

  }

  public SchemaConfig getDefaultSchemaByName(String name){
    return switch (name.toLowerCase()) {
      case "json" -> repository.getResource(DEFAULT_JSON_SCHEMA.toString()).getDefaultVersion();
      case "raw" -> repository.getResource(DEFAULT_RAW_UUID.toString()).getDefaultVersion();
      case "numeric" -> repository.getResource(DEFAULT_NUMERIC_STRING_SCHEMA.toString()).getDefaultVersion();
      case "string" -> repository.getResource(DEFAULT_STRING_SCHEMA.toString()).getDefaultVersion();
      case "xml" -> repository.getResource(DEFAULT_XML_SCHEMA.toString()).getDefaultVersion();
      default -> repository.getResource(DEFAULT_RAW_UUID.toString()).getDefaultVersion();
    };
  }

  public MessageFormatter getMessageFormatter(String uniqueId) {
    return loadedFormatter.get(uniqueId);
  }

  public List<String> getMessageFormats() {
    return MessageFormatterFactory.getInstance().getFormatters();
  }


  public synchronized SchemaConfig getSchema(UUID uniqueId) {
    return getSchema(uniqueId.toString());
  }

  public synchronized SchemaConfig getSchema(String uniqueId) {
    SchemaResource resource = repository.getResource(uniqueId);
    if(resource != null){
      return resource.getDefaultVersion();
    }
    return null;
  }

  public synchronized SchemaConfig locateSchema(String destinationName) {
    SchemaConfig config = SchemaLocationHelper.locateSchema(repository.getAllSchemas(), destinationName);
    if(config != null){
      return config;
    }
    return getSchema(SchemaManager.DEFAULT_RAW_UUID.toString());
  }

  public synchronized List<SchemaConfig> getSchemaByTopicName(String s) {
    return pathMap.get(s);
  }

  public synchronized List<SchemaConfig> getSchemaByType(String type) {
    List<SchemaResource> resources = repository.search(type, new LinkedHashMap<>(), 0, 0);
    List<SchemaConfig> schemas = new ArrayList<>();
    for(SchemaResource resource : resources){
      if(resource.getDefaultVersion() != null) {
        schemas.add(resource.getDefaultVersion());
      }
    }
    return schemas;
  }

  public synchronized List<SchemaConfig> getAll() {
    List<SchemaResource> resources = repository.getAllSchemas();
    List<SchemaConfig> schemas = new ArrayList<>();
    for(SchemaResource resource : resources){
      if(resource.getDefaultVersion() != null) {
        schemas.add(resource.getDefaultVersion());
      }
    }
    return schemas;
  }

  public Map<String, List<SchemaConfig>> getMappedSchemas() {
    return pathMap;
  }

  public synchronized void removeSchema(String uniqueId) {
    repository.deleteResource(uniqueId);
    loadedFormatter.remove(uniqueId);
    updateCount++;
  }

  public synchronized List<LinkFormat> buildLinkFormatList() {
    List<LinkFormat> response = new ArrayList<>();
    for(Entry<String, List<SchemaConfig>> entry: pathMap.entrySet()){
      String path = entry.getKey();
      List<SchemaConfig> schemas = entry.getValue();
      for(SchemaConfig schemaConfig:schemas) {
        response.add(new LinkFormat(path, schemaConfig.getInterfaceDescription(), schemaConfig.getResourceType()));
      }
    }
    return response;
  }

  public String buildLinkFormatResponse() {
    List<LinkFormat> linkFormatList = buildLinkFormatList();
    return LinkFormatManager.getInstance().buildLinkFormatString("", linkFormatList);
  }

  @Override
  public String getName() {
    return "Schema Manager";
  }

  @Override
  public String getDescription() {
    return "Manages the life cycle of schemas on the server";
  }

  public void start() {
    SchemaConfig rawConfig = new RawSchemaConfig();
    rawConfig.setUniqueId(DEFAULT_RAW_UUID);
    rawConfig.setVersion(1);
    rawConfig.setTitle("Raw byte[]");
    rawConfig.setResourceType("unknown");
    rawConfig.setInterfaceDescription("raw");
    rawConfig.setSchema(new JsonObject());
    addSchema("", rawConfig);

    NativeSchemaConfig nativeSchemaConfig = new NativeSchemaConfig();
    nativeSchemaConfig.setUniqueId(DEFAULT_NUMERIC_STRING_SCHEMA);
    nativeSchemaConfig.setType(TYPE.NUMERIC_STRING);
    nativeSchemaConfig.setVersion(1);
    nativeSchemaConfig.setTitle("Numeric String");
    nativeSchemaConfig.setInterfaceDescription("numeric");
    nativeSchemaConfig.setResourceType(MONITOR);
    nativeSchemaConfig.setSchema(new JsonObject());
    addSchema("$SYS", nativeSchemaConfig);

    nativeSchemaConfig = new NativeSchemaConfig();
    nativeSchemaConfig.setVersion(1);
    nativeSchemaConfig.setUniqueId(DEFAULT_STRING_SCHEMA);
    nativeSchemaConfig.setTitle("String");
    nativeSchemaConfig.setType(TYPE.STRING);
    nativeSchemaConfig.setInterfaceDescription("string");
    nativeSchemaConfig.setResourceType(MONITOR);
    nativeSchemaConfig.setSchema(new JsonObject());
    addSchema("$SYS", nativeSchemaConfig);

    JsonSchemaConfig jsonSchemaConfig = new JsonSchemaConfig();
    jsonSchemaConfig.setVersion(1);
    jsonSchemaConfig.setUniqueId(DEFAULT_JSON_SCHEMA);
    jsonSchemaConfig.setInterfaceDescription("json");
    jsonSchemaConfig.setResourceType(MONITOR);
    jsonSchemaConfig.setTitle("Generic JSON");
    jsonSchemaConfig.setSchema(new JsonObject());
    addSchema("$SYS", jsonSchemaConfig);

    XmlSchemaConfig xmlSchemaConfig = new XmlSchemaConfig();
    xmlSchemaConfig.setUniqueId(DEFAULT_XML_SCHEMA);
    xmlSchemaConfig.setInterfaceDescription("xml");
    xmlSchemaConfig.setResourceType(MONITOR);
    xmlSchemaConfig.setTitle("Generic XML");
    xmlSchemaConfig.setSchema(new JsonObject());
    addSchema("$SYS", xmlSchemaConfig);

    // This ensures the factory is loaded
    MessageFormatterFactory.getInstance();
  }

  private SchemaManager() {
    SchemaManagerConfig config = ConfigurationManager.getInstance().getConfiguration(SchemaManagerConfig.class);
    SchemaRepository buildTime;
    if(config != null && config.getRepositoryConfig() != null) {
      try {
        buildTime = buildRepository(config.getRepositoryConfig());
      } catch (IOException e) {
        buildTime = new SimpleSchemaRepository();
      }
    }
    else{
      buildTime = new SimpleSchemaRepository();
    }
    repository = buildTime;
    loadedFormatter = new LinkedHashMap<>();
    pathMap = new LinkedHashMap<>();
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("Registered Schemas: "+ pathMap.size() );
    status.setStatus(Status.OK);
    return status;
  }

   public List<SchemaResource> getSchemaByContext(String context) {
    return repository.getAllSchemas().stream()
        .filter(s -> context.equals(s.getDefaultVersion().getTitle()))
        .toList();
  }

  public List<SchemaResource> getSchemas(String type) {
    return repository.getAllSchemas().stream()
        .filter(s -> type.equals(s.getDefaultVersion().getFormat()))
        .toList();
  }

  public SchemaConfig convertAndCreate(String topicName, ConfigurationProperties config)  {
    return VersionUpgradeHelper.convertAndCreate(this, topicName, config);
  }

  private SchemaRepository buildRepository(RepositoryConfigDTO config) throws IOException {
    if(config instanceof FileRepositoryConfigDTO fileConfig) {
      return new FileSchemaRepository(new File(fileConfig.getDirectoryPath()));
    }
    if(config instanceof MapsRepositoryConfigDTO mapConfig) {
      return new RestSchemaRepository(new File(mapConfig.getDirectoryPath()), mapConfig.getUrlPath());
    }
    return new SimpleSchemaRepository();
  }


}