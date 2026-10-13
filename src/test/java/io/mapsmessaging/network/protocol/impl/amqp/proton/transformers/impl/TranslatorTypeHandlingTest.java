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

package io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl;

import io.mapsmessaging.api.MessageBuilder;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TranslatorTypeHandlingTest {

  // AMQP 1.0 Part 3 sections 3.2.1 and 3.2.4: preserve TTL and absolute expiry.
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void expiry_metadata_remains_mutable_and_round_trips(boolean absoluteExpiry) {
    long created = System.currentTimeMillis();
    Message incoming = Message.Factory.create();
    var properties = new org.apache.qpid.proton.amqp.messaging.Properties();
    properties.setCreationTime(new java.util.Date(created));
    if (absoluteExpiry) {
      properties.setAbsoluteExpiryTime(new java.util.Date(created + 60000));
    }
    incoming.setProperties(properties);
    var header = new org.apache.qpid.proton.amqp.messaging.Header();
    header.setTtl(org.apache.qpid.proton.amqp.UnsignedInteger.valueOf(60000));
    incoming.setHeader(header);
    incoming.setBody(new AmqpValue("expires-later"));

    MessageBuilder builder = new TextMessageTranslator().decode(new MessageBuilder(), incoming);
    assertEquals(Long.toString(created + 60000), builder.getMeta().get(
        io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.encoders.PropertiesEncoder.ORIGINAL_ABSOLUTE_EXPIRY));
    assertArrayEquals("expires-later".getBytes(java.nio.charset.StandardCharsets.UTF_8), builder.getOpaqueData());
    builder.getMeta().put("extension", "retained");
    var outgoing = new org.apache.qpid.proton.amqp.messaging.Properties();
    io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.encoders.PropertiesEncoder.packProperties(outgoing, builder.build());
    assertEquals(created + 60000, outgoing.getAbsoluteExpiryTime().getTime());
  }

  @Test
  void object_body_preserves_binary_bytes() {
    byte[] payload = {0, 127, (byte) 255};
    Message incoming = Message.Factory.create();
    incoming.setBody(new Data(new Binary(payload)));
    MessageBuilder builder = new ObjectMessageTranslator().decode(new MessageBuilder(), incoming);
    assertArrayEquals(payload, builder.getOpaqueData());
  }

  @Test
  void map_body_preserves_binary_and_scalar_values() {
    byte[] payload = {1, (byte) 255};
    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
    values.put("binary", new Binary(payload));
    values.put("scalar", 42);
    Message incoming = Message.Factory.create();
    incoming.setBody(new AmqpValue(values));
    MessageBuilder builder = new MapMessageTranslator().decode(new MessageBuilder(), incoming);
    assertArrayEquals(payload, (byte[]) builder.getDataMap().get("binary").getData());
    assertEquals(42, builder.getDataMap().get("scalar").getData());
  }

  @Test
  void sequence_body_preserves_order_and_binary_values() {
    byte[] payload = {2, 3};
    Message incoming = Message.Factory.create();
    incoming.setBody(new AmqpSequence(List.of("first", new Binary(payload), 7)));
    MessageBuilder builder = new StreamMessageTranslator().decode(new MessageBuilder(), incoming);
    assertEquals("first", builder.getDataMap().get("0").getData());
    assertArrayEquals(payload, (byte[]) builder.getDataMap().get("1").getData());
    assertEquals(7, builder.getDataMap().get("2").getData());
  }

  @Test
  void text_body_preserves_text() {
    Message incoming = Message.Factory.create();
    incoming.setBody(new AmqpValue("sensor"));
    MessageBuilder builder = new TextMessageTranslator().decode(new MessageBuilder(), incoming);
    assertArrayEquals("sensor".getBytes(), builder.getOpaqueData());
  }

  @Test
  void absent_bodies_do_not_create_typed_data() {
    for (BaseMessageTranslator translator : List.of(new ObjectMessageTranslator(),
        new MapMessageTranslator(), new StreamMessageTranslator(), new TextMessageTranslator())) {
      MessageBuilder builder = translator.decode(new MessageBuilder(), Message.Factory.create());
      assertTrue(builder.getDataMap().isEmpty());
    }
  }

  @Test
  void unmatched_map_and_sequence_bodies_do_not_create_typed_data() {
    Message incoming = Message.Factory.create();
    incoming.setBody(new AmqpValue(123));
    assertTrue(new MapMessageTranslator().decode(new MessageBuilder(), incoming).getDataMap().isEmpty());
    assertTrue(new StreamMessageTranslator().decode(new MessageBuilder(), incoming).getDataMap().isEmpty());
  }
}
