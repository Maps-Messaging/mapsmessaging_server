package io.mapsmessaging.api.transformers;

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

import java.nio.charset.StandardCharsets;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

public class JsonToSchema implements InterServerTransformation {

  private static final Logger logger = LoggerFactory.getLogger(JsonToSchema.class);
  private final JsonToSchemaTransformationDTO config;
  private SchemaConfig schemaConfig;

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
      JsonObject jsonObject = getJsonObject(messageBuilder.getOpaqueData());
      if(schemaConfig == null){
        schemaConfig = lookupConfig();
      }
      if(schemaConfig != null){
        MessageFormatter messageFormatter =  SchemaManager.getInstance().getMessageFormatter(schemaConfig);
        if(messageFormatter != null){
          byte[] packed = messageFormatter.parseFromJson(jsonObject);
          messageBuilder.setOpaqueData(packed);
        }
      }
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName());
    }
  }

  private SchemaConfig lookupConfig(){
    if(config == null) return null;
    SchemaConfig schemaConfig = null;
    if(config.getSchemaName() != null && !config.getSchemaName().isEmpty()) {
      schemaConfig = SchemaManager.getInstance().getSchema(config.getSchemaName());
      if (schemaConfig == null) {
        schemaConfig = SchemaManager.getInstance().getSchemaByName(config.getSchemaName());
      }
    }
    if(schemaConfig == null && config.getFormat() != null && config.getMessageName() != null){
      schemaConfig = SchemaManager.getInstance().getSchemaByNameAndType(config.getMessageName(), config.getFormat());

    }
    return schemaConfig;
  }

  private JsonObject getJsonObject(byte[] payload) {
    return JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
  }

  public JsonToSchema(JsonToSchemaTransformationDTO config) {
    this.config = config;
    schemaConfig = null;
  }

  public JsonToSchema() {
    this.config = null;
    schemaConfig = null;
  }
}