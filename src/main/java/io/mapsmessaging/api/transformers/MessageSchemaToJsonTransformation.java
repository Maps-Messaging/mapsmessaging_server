package io.mapsmessaging.api.transformers;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.engine.schema.MessageSchemaToJsonBuilder;

public class MessageSchemaToJsonTransformation implements InterServerTransformation {

  public final MessageSchemaToJsonBuilder messageSchemaToJsonBuilder;

  public MessageSchemaToJsonTransformation(){
    messageSchemaToJsonBuilder = new MessageSchemaToJsonBuilder();
  }

  @Override
  public ParsedMessage transform(String source, ParsedMessage message) {
    try {
      Message results = messageSchemaToJsonBuilder.parse(message.getMessage(), source);
      return new ParsedMessage(source, results);
    } catch (Exception e) {

    }
    return message;
  }

  @Override
  public InterServerTransformation build(TransformationConfigDTO properties) {
    return new MessageSchemaToJsonTransformation();
  }

  @Override
  public String getName() {
    return "schema-to-json";
  }

  @Override
  public String getDescription() {
    return "Schema to Json transformation";
  }
}
