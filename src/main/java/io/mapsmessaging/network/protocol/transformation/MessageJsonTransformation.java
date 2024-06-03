package io.mapsmessaging.network.protocol.transformation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;

public class MessageJsonTransformation implements ProtocolMessageTransformation {

  private static final ObjectMapper objectMapper = createObjectMapper();

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    // Configure the ObjectMapper as needed
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    return mapper;
  }

  @Override
  public String getName() {
    return "Message-JSON";
  }

  @Override
  public String getDescription() {
    return "Transforms MessageBuilder to JSON payload and vice versa";
  }

  @Override
  public void incoming(MessageBuilder messageBuilder) {
    try {
      byte[] opaqueData = messageBuilder.getOpaqueData();
      if (opaqueData != null) {
        String json = new String(opaqueData);
        Message message = objectMapper.readValue(json, Message.class);
        messageBuilder.setMeta(message.getMeta())
            .setDataMap(message.getDataMap())
            .setOpaqueData(message.getOpaqueData())
            .setCorrelationData(message.getCorrelationData())
            .setContentType(message.getContentType())
            .setResponseTopic(message.getResponseTopic())
            .setId(message.getKey())
            .setPriority(message.getPriority())
            .setRetain(message.isRetain())
            .setTransformation(this)
            .setDelayed(message.getDelayed())
            .setSchemaId(message.getSchemaId());
      }
    } catch (Exception e) {
      // Log the exception and handle it as needed
      e.printStackTrace();
    }
  }

  @Override
  public byte[] outgoing(Message message) {
    try {
      return objectMapper.writeValueAsBytes(message);
    } catch (Exception e) {
      // Log the exception and handle it as needed
      e.printStackTrace();
      return message.getOpaqueData();
    }
  }
}
