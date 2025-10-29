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

import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig.TYPE;
import io.mapsmessaging.schemas.config.impl.RawSchemaConfig;
import io.mapsmessaging.schemas.config.impl.XmlSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.schemas.repository.SchemaRepository;
import io.mapsmessaging.schemas.repository.impl.SimpleSchemaRepository;
import io.mapsmessaging.utilities.Agent;
import lombok.Getter;

import java.util.*;
import java.util.Map.Entry;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class SchemaManager implements SchemaRepository, Agent {

  private static final String MONITOR = "monitor";

  public static final String DEFAULT_RAW_UUID =              UUID.fromString("10000000-0000-1000-a000-100000000000").toString();
  public static final String DEFAULT_NUMERIC_STRING_SCHEMA = UUID.fromString("10000000-0000-1000-a000-100000000001").toString();
  public static final String DEFAULT_STRING_SCHEMA =         UUID.fromString("10000000-0000-1000-a000-100000000002").toString();
  public static final String DEFAULT_JSON_SCHEMA =           UUID.fromString("10000000-0000-1000-a000-100000000003").toString();
  public static final String DEFAULT_XML_SCHEMA =            UUID.fromString("10000000-0000-1000-a000-100000000004").toString();

  private static class Holder {
    static final SchemaManager INSTANCE = new SchemaManager();
  }

  public static SchemaManager getInstance() {
    return Holder.INSTANCE;
  }

  private final SchemaRepository repository;
  private final Map<String, MessageFormatter> loadedFormatter;

  @Getter
  private long updateCount = 0;

  @Override
  public synchronized SchemaConfig addSchema(String path, SchemaConfig schemaConfig) {
    try {
      if (!loadedFormatter.containsKey(schemaConfig.getUniqueId())) {
        MessageFormatter messageFormatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
        loadedFormatter.put(schemaConfig.getUniqueId(), messageFormatter);
      }
    } catch (Exception e) {
      // Unable to load the formatter
    }

    if(repository.getSchemaByContext(path) != null){
      List<SchemaConfig> schemas = repository.getSchemaByContext(path);
      for(SchemaConfig schema : schemas){
        if(schema.getUniqueId().equals(schemaConfig.getUniqueId())){
          return schema;
        }
      }
    }
    updateCount++;
    return repository.addSchema(path, schemaConfig);
  }

  public void stop() {

  }

  public SchemaConfig getDefaultSchemaByName(String name){
    switch(name.toLowerCase()) {
      case "json": return repository.getSchema(DEFAULT_JSON_SCHEMA);
      case "raw": return repository.getSchema(DEFAULT_RAW_UUID);
      case "numeric": return repository.getSchema(DEFAULT_NUMERIC_STRING_SCHEMA);
      case "string": return repository.getSchema(DEFAULT_STRING_SCHEMA);
      case "xml": return repository.getSchema(DEFAULT_XML_SCHEMA);
      default: return repository.getSchema(DEFAULT_RAW_UUID);
    }
  }

  public MessageFormatter getMessageFormatter(String uniqueId) {
    return loadedFormatter.get(uniqueId);
  }

  public List<String> getMessageFormats() {
    return MessageFormatterFactory.getInstance().getFormatters();
  }

  @Override
  public synchronized SchemaConfig getSchema(String uniqueId) {
    return repository.getSchema(uniqueId);

  }

  public synchronized SchemaConfig locateSchema(String destinationName) {
    SchemaConfig config = SchemaLocationHelper.locateSchema(repository.getAll(), destinationName);
    return config != null ? config : getSchema(SchemaManager.DEFAULT_RAW_UUID);
  }

  @Override
  public synchronized List<SchemaConfig> getSchemaByContext(String s) {
    return repository.getSchemaByContext(s);
  }

  @Override
  public synchronized List<SchemaConfig> getSchemas(String s) {
    return repository.getSchemas(s);
  }

  @Override
  public synchronized List<SchemaConfig> getAll() {
    return repository.getAll();
  }

  @Override
  public Map<String, List<SchemaConfig>> getMappedSchemas() {
    return repository.getMappedSchemas();
  }

  @Override
  public synchronized void removeSchema(String uniqueId) {
    repository.removeSchema(uniqueId);
    loadedFormatter.remove(uniqueId);
    updateCount++;
  }

  @Override
  public synchronized void removeAllSchemas() {
    loadedFormatter.clear();
    repository.removeAllSchemas();
    updateCount++;
  }

  public synchronized List<LinkFormat> buildLinkFormatList() {
    List<LinkFormat> response = new ArrayList<>();
    for(Entry<String, List<SchemaConfig>> entry: repository.getMappedSchemas().entrySet()){
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
    rawConfig.setTitle("Raw byte[]");
    rawConfig.setResourceType("unknown");
    rawConfig.setInterfaceDescription("raw");
    addSchema("", rawConfig);

    NativeSchemaConfig nativeSchemaConfig = new NativeSchemaConfig();
    nativeSchemaConfig.setUniqueId(DEFAULT_NUMERIC_STRING_SCHEMA);
    nativeSchemaConfig.setType(TYPE.NUMERIC_STRING);
    nativeSchemaConfig.setVersion(1);
    nativeSchemaConfig.setTitle("Numeric String");
    nativeSchemaConfig.setInterfaceDescription("numeric");
    nativeSchemaConfig.setResourceType(MONITOR);
    addSchema("$SYS", nativeSchemaConfig);

    nativeSchemaConfig = new NativeSchemaConfig();
    nativeSchemaConfig.setVersion(1);
    nativeSchemaConfig.setUniqueId(DEFAULT_STRING_SCHEMA);
    nativeSchemaConfig.setTitle("String");
    nativeSchemaConfig.setType(TYPE.STRING);
    nativeSchemaConfig.setInterfaceDescription("string");
    nativeSchemaConfig.setResourceType(MONITOR);
    addSchema("$SYS", nativeSchemaConfig);

    JsonSchemaConfig jsonSchemaConfig = new JsonSchemaConfig();
    nativeSchemaConfig.setVersion(1);
    jsonSchemaConfig.setUniqueId(DEFAULT_JSON_SCHEMA);
    jsonSchemaConfig.setInterfaceDescription("json");
    jsonSchemaConfig.setResourceType(MONITOR);
    jsonSchemaConfig.setTitle("Generic JSON");
    addSchema("$SYS", jsonSchemaConfig);

    XmlSchemaConfig xmlSchemaConfig = new XmlSchemaConfig();
    xmlSchemaConfig.setUniqueId(DEFAULT_XML_SCHEMA);
    xmlSchemaConfig.setInterfaceDescription("xml");
    xmlSchemaConfig.setResourceType(MONITOR);
    xmlSchemaConfig.setTitle("Generic XML");
    addSchema("$SYS", xmlSchemaConfig);

    // This ensures the factory is loaded
    MessageFormatterFactory.getInstance();
  }

  private SchemaManager() {
    repository = new SimpleSchemaRepository();
    loadedFormatter = new LinkedHashMap<>();
  }
  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("Registered Schemas: " + repository.getMappedSchemas().size());
    status.setStatus(Status.OK);
    return status;
  }

}
