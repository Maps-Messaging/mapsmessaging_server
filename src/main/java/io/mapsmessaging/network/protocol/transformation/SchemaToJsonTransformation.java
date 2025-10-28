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

package io.mapsmessaging.network.protocol.transformation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.rest.translation.GsonDateTimeSerialiser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.schemas.formatters.impl.RawFormatter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static io.mapsmessaging.logging.ServerLogMessages.MESSAGE_TRANSFORMATION_EXCEPTION;

public class SchemaToJsonTransformation implements ProtocolMessageTransformation {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Gson gson;

  public SchemaToJsonTransformation() {
    gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime.class, new GsonDateTimeSerialiser())
        .create();
  }

  @Override
  public String getName() {
    return "Schema-To-Json";
  }

  @Override
  public String getDescription() {
    return "Transforms outgoing messages into a JSON object if there is a corresponding schema";
  }

  @Override
  public int getId() {
    return 4;
  }

  @Override
  public Message outgoing(Message message, String destinationName) {
    String schemaId = message.getSchemaId();
    if(schemaId != null && !destinationName.startsWith("$")) {
      SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
      if(config != null) {
        try {
          MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);
          if (formatter != null && !(formatter instanceof RawFormatter)) {
            byte[] data = pack(message, config, formatter);
            MessageBuilder messageBuilder = new MessageBuilder();
            messageBuilder.setOpaqueData(data);
            return messageBuilder.build();
          }
        } catch (Exception e) {
          logger.log(MESSAGE_TRANSFORMATION_EXCEPTION, e);
        }
      }
    }
    return message;
  }

  private byte[] pack(Message message, SchemaConfig config, MessageFormatter formatter) throws IOException {
    byte[] payload = message.getOpaqueData();
    JsonObject jsonObject = formatter.parseToJson(payload);
    JsonObject wrapper = new JsonObject();
    wrapper.add("payload", jsonObject);
    wrapper.addProperty("schemaId", config.getUniqueId());
    wrapper.addProperty("schemaTitle", config.getTitle());

    if(message.getDataMap() != null && !message.getDataMap().isEmpty()){
      JsonObject map = new JsonObject();
      for (Map.Entry<String, TypedData> entry : message.getDataMap().entrySet()) {
        Object data = entry.getValue().getData();
        JsonElement element = gson.toJsonTree(data);
        map.add(entry.getKey(), element);
      }
      wrapper.add("map", map);
    }
    JsonObject metaObject = new JsonObject();
    for (Map.Entry<String, String> meta : message.getMeta().entrySet()) {
      metaObject.addProperty(meta.getKey(), meta.getValue());
    }
    wrapper.add("meta", metaObject);

    if(message.getContentType() != null)wrapper.addProperty("content-type", message.getContentType());
    if(message.getCorrelationData() != null)wrapper.addProperty("correlationId", new String(message.getCorrelationData()));
    if(message.getResponseTopic() != null)wrapper.addProperty("responseTopic", message.getResponseTopic());
    if(message.getCreation() != 0){
      wrapper.addProperty("creation",  Instant.ofEpochMilli(message.getCreation()).atZone(ZoneId.systemDefault()).toString());
    }
    return gson.toJson(wrapper).getBytes();
  }
}
