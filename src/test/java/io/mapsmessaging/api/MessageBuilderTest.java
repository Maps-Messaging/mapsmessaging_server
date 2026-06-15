package io.mapsmessaging.api;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class MessageBuilderTest {

  @Test
  void setMeta_mergesWhenBothNonNull() {
    MessageBuilder builder = new MessageBuilder();

    Map<String, String> first = new LinkedHashMap<>();
    first.put("a", "1");

    Map<String, String> second = new LinkedHashMap<>();
    second.put("b", "2");

    builder.setMeta(first);
    builder.setMeta(second);

    Assertions.assertNotNull(builder.getMeta());
    Assertions.assertEquals("1", builder.getMeta().get("a"));
    Assertions.assertEquals("2", builder.getMeta().get("b"));
  }

  @Test
  void setDataMap_mergesWhenBothNonNull() {
    MessageBuilder builder = new MessageBuilder();

    Map<String, TypedData> first = new LinkedHashMap<>();
    first.put("a", new TypedData("x"));

    Map<String, TypedData> second = new LinkedHashMap<>();
    second.put("b", new TypedData(123L));

    builder.setDataMap(first);
    builder.setDataMap(second);

    Assertions.assertNotNull(builder.getDataMap());
    Assertions.assertEquals("x", builder.getDataMap().get("a").getData());
    Assertions.assertEquals(123L, builder.getDataMap().get("b").getData());
  }

  @Test
  void setMeta_replacesWhenCurrentNull() {
    MessageBuilder builder = new MessageBuilder();

    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("k", "v");

    builder.setMeta(meta);

    Assertions.assertEquals("v", builder.getMeta().get("k"));
  }

  @Test
  void setDataMap_replacesWhenCurrentNull() {
    MessageBuilder builder = new MessageBuilder();

    Map<String, TypedData> data = new LinkedHashMap<>();
    data.put("k", new TypedData("v"));

    builder.setDataMap(data);

    Assertions.assertEquals("v", builder.getDataMap().get("k").getData());
  }

  @Test
  void build_appliesIncomingTransformationOnlyOnce() {
    AtomicInteger invocationCount = new AtomicInteger();
    ProtocolMessageTransformation transformation = new ProtocolMessageTransformation() {
      @Override
      public String getName() {
        return "test";
      }

      @Override
      public String getDescription() {
        return "test transformation";
      }

      @Override
      public int getId() {
        return 1;
      }

      @Override
      public void incoming(MessageBuilder messageBuilder) {
        invocationCount.incrementAndGet();
        messageBuilder.setContentType("application/transformed");
      }
    };
    MessageBuilder builder = new MessageBuilder().setTransformation(transformation);

    Message firstMessage = builder.build();
    Message secondMessage = builder.build();

    Assertions.assertEquals("application/transformed", firstMessage.getContentType());
    Assertions.assertEquals("application/transformed", secondMessage.getContentType());
    Assertions.assertEquals(1, invocationCount.get());
    Assertions.assertNull(builder.getTransformation());
  }
}
