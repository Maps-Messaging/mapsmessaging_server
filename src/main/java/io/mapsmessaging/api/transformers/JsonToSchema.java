package io.mapsmessaging.api.transformers;

import com.ecwid.consul.json.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToSchemaTransformationDTO;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.mapsmessaging.logging.ServerLogMessages.SCHEMA_MESSAGE_DROP;
import static io.mapsmessaging.logging.ServerLogMessages.SCHEMA_MESSAGE_FORMAT;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

public class JsonToSchema implements InterServerTransformation {

  private static final Gson gson = GsonFactory.getGson();

  private static final Logger logger = LoggerFactory.getLogger(JsonToSchema.class);
  private final JsonToSchemaTransformationDTO config;
  private SchemaConfig schemaConfig;
  private MessageFormatter messageFormatter;

  @Override
  public ParsedMessage transform(String source, ParsedMessage message){
    MessageBuilder messageBuilder = new MessageBuilder(message.getMessage());
    convert(messageBuilder);
    message.setMessage(messageBuilder.build());
    return message;
  }

  @Override
  public InterServerTransformation build(TransformationConfigDTO dto) {
    return new JsonToSchema((JsonToSchemaTransformationDTO)dto);
  }

  @Override
  public String getName() {
    return "JsonToSchema";
  }

  @Override
  public String getDescription() {
    return "Converts JSON to Schema specified and configured";
  }

  private void convert(MessageBuilder messageBuilder) {
    try {
      if(schemaConfig == null){
        schemaConfig = lookupConfig();
      }
      if(messageFormatter == null){
        loadMessageFormatter();
      }
      if(messageFormatter != null){
        JsonObject jsonObject = getJsonObject(messageBuilder.getOpaqueData());
        byte[] packed = messageFormatter.parseFromJson(jsonObject);
        messageBuilder.setOpaqueData(packed);
      }
      else{
        logger.log(SCHEMA_MESSAGE_DROP, getName());
      }
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName());
    }
  }

  private SchemaConfig lookupConfig() {
    if(config == null) return null;
    SchemaConfig schema = null;
    if(config.getSchemaName() != null && !config.getSchemaName().isEmpty()) {
      schema = SchemaManager.getInstance().getSchema(config.getSchemaName());
      if (schema == null) {
        schema = SchemaManager.getInstance().getSchemaByName(config.getSchemaName());
      }
    }
    if(schema == null && config.getFormat() != null && config.getMessageName() != null){
      schema = SchemaManager.getInstance().getSchemaByNameAndType(config.getMessageName(), config.getFormat());
    }
    return schema;
  }

  private void loadMessageFormatter() {
    try {
      messageFormatter = SchemaManager.getInstance().getMessageFormatter(schemaConfig);
      Map<String, Object> expected = messageFormatter.getFormat();
      String json = gson.toJson(expected);
      if (!json.isEmpty()) {
        String name = config.getSchemaName();
        if(name == null || name.isEmpty()){
          name = config.getFormat()+"/"+config.getMessageName();
        }
        logger.log(SCHEMA_MESSAGE_FORMAT, name, json);
      }
    } catch (IOException e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), e);
    }
  }

  private JsonObject getJsonObject(byte[] payload) {
    return JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
  }

  public JsonToSchema(JsonToSchemaTransformationDTO config) {
    this.config = config;
    schemaConfig = lookupConfig();
    if(schemaConfig != null){
      loadMessageFormatter();
    }
  }

  public JsonToSchema() {
    this.config = null;
    schemaConfig = null;
  }
}