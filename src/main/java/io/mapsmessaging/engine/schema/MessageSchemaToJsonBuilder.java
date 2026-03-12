package io.mapsmessaging.engine.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
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


public class MessageSchemaToJsonBuilder {

  private final Gson gson;

  public MessageSchemaToJsonBuilder() {
    gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime.class, new GsonDateTimeSerialiser())
        .create();
  }

  public Message parse(Message message, String destinationName) throws Exception {
    String schemaId = message.getSchemaId();
    if(schemaId != null && !destinationName.startsWith("$")) {
      SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
      if(config != null) {
        MessageFormatter formatter = SchemaManager.getInstance().getMessageFormatter(config);
        if (formatter != null && !(formatter instanceof RawFormatter)) {
          byte[] data = pack(message, config, formatter);
          MessageBuilder messageBuilder = new MessageBuilder();
          messageBuilder.setOpaqueData(data);
          return messageBuilder.build();
        }
      }
    }
    return null;
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
