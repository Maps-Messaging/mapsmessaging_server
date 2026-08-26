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

  @Test
  void copyConstructor_preservesMessageFields_andIsolatesMaps() {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("meta", "original");
    Map<String, TypedData> dataMap = new LinkedHashMap<>();
    dataMap.put("data", new TypedData("original"));
    long creation = 123456789L;
    Message original = new MessageBuilder()
        .setCreation(creation)
        .setSchemaId("schema-id")
        .setMeta(meta)
        .setDataMap(dataMap)
        .build();

    Message copy = new MessageBuilder(original).build();

    Assertions.assertEquals(creation, copy.getCreation());
    Assertions.assertEquals("schema-id", copy.getSchemaId());
    Assertions.assertNotSame(original.getMeta(), copy.getMeta());
    Assertions.assertNotSame(original.getDataMap(), copy.getDataMap());

    copy.getMeta().put("copy-meta", "copy");
    copy.getDataMap().put("copy-data", new TypedData("copy"));
    Assertions.assertFalse(original.getMeta().containsKey("copy-meta"));
    Assertions.assertFalse(original.getDataMap().containsKey("copy-data"));
  }
}
