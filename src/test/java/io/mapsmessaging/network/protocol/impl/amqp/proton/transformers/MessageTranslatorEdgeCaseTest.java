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

package io.mapsmessaging.network.protocol.impl.amqp.proton.transformers;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.BaseMessageTranslator;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.MapMessageTranslator;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.TextMessageTranslator;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.encoders.HeaderEncoder;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.UnsignedByte;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Header;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.amqp.messaging.Properties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageTranslatorEdgeCaseTest {

  @Test
  void base_translator_with_sliced_data_preserves_visible_payload() {
    org.apache.qpid.proton.message.Message protonMessage = org.apache.qpid.proton.message.Message.Factory.create();
    protonMessage.setBody(new Data(new Binary(new byte[]{9, 1, 2, 3, 9}, 1, 3)));

    MessageBuilder decoded = new BaseMessageTranslator().decode(new MessageBuilder(), protonMessage);

    assertArrayEquals(new byte[]{1, 2, 3}, decoded.getOpaqueData());
  }

  @Test
  void base_translator_with_amqp_binary_value_preserves_visible_payload() {
    org.apache.qpid.proton.message.Message protonMessage = org.apache.qpid.proton.message.Message.Factory.create();
    protonMessage.setBody(new AmqpValue(new Binary(new byte[]{9, 2, 3, 9}, 1, 2)));

    MessageBuilder decoded = new BaseMessageTranslator().decode(new MessageBuilder(), protonMessage);

    assertArrayEquals(new byte[]{2, 3}, decoded.getOpaqueData());
  }

  @Test
  void text_translator_with_unicode_uses_utf8() {
    org.apache.qpid.proton.message.Message protonMessage = org.apache.qpid.proton.message.Message.Factory.create();
    protonMessage.setBody(new AmqpValue("Καλημέρα 世界"));

    MessageBuilder decoded = new TextMessageTranslator().decode(new MessageBuilder(), protonMessage);

    assertEquals("Καλημέρα 世界", new String(decoded.getOpaqueData(), StandardCharsets.UTF_8));
  }

  @Test
  void map_translator_with_general_map_and_sliced_binary_decodes_values() {
    Map<String, Object> body = new HashMap<>();
    body.put("data", new Binary(new byte[]{9, 4, 5, 9}, 1, 2));
    org.apache.qpid.proton.message.Message protonMessage = org.apache.qpid.proton.message.Message.Factory.create();
    protonMessage.setBody(new AmqpValue(body));

    MessageBuilder decoded = new MapMessageTranslator().decode(new MessageBuilder(), protonMessage);

    assertArrayEquals(new byte[]{4, 5}, (byte[]) decoded.getDataMap().get("data").getData());
  }

  @Test
  void application_properties_with_sliced_binary_preserve_visible_value() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("data", new Binary(new byte[]{9, 6, 7, 9}, 1, 2));
    org.apache.qpid.proton.message.Message protonMessage = org.apache.qpid.proton.message.Message.Factory.create();
    protonMessage.setApplicationProperties(new ApplicationProperties(properties));

    MessageBuilder decoded = new BaseMessageTranslator().decode(new MessageBuilder(), protonMessage);

    assertArrayEquals(new byte[]{6, 7}, (byte[]) decoded.getDataMap().get("data").getData());
  }

  @Test
  void message_id_with_sliced_binary_preserves_visible_value() {
    Properties properties = new Properties();
    properties.setMessageId(new Binary(new byte[]{9, 8, 7, 9}, 1, 2));
    org.apache.qpid.proton.message.Message protonMessage = org.apache.qpid.proton.message.Message.Factory.create();
    protonMessage.setProperties(properties);

    MessageBuilder decoded = new BaseMessageTranslator().decode(new MessageBuilder(), protonMessage);

    assertArrayEquals(new byte[]{8, 7}, (byte[]) decoded.getDataMap().get("JMSMessageID").getData());
  }

  @Test
  void translator_factory_accepts_unsigned_jms_message_type_annotation() {
    Map<Symbol, Object> annotations = new HashMap<>();
    annotations.put(Symbol.valueOf("x-opt-jms-msg-type"), new UnsignedByte((byte) 5));

    MessageTranslator translator = MessageTranslatorFactory.getMessageTranslator(new MessageAnnotations(annotations));

    assertInstanceOf(TextMessageTranslator.class, translator);
  }

  @Test
  void text_translator_with_non_string_value_rejects_body() {
    org.apache.qpid.proton.message.Message protonMessage = org.apache.qpid.proton.message.Message.Factory.create();
    protonMessage.setBody(new AmqpValue(42));

    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> new TextMessageTranslator().decode(new MessageBuilder(), protonMessage));
  }

  @Test
  void header_encoder_with_expired_message_does_not_wrap_ttl() {
    Message message = mock(Message.class);
    when(message.getPriority()).thenReturn(Priority.NORMAL);
    when(message.getQualityOfService()).thenReturn(QualityOfService.AT_MOST_ONCE);
    when(message.getExpiry()).thenReturn(System.currentTimeMillis() - 1000);
    Header header = new Header();

    HeaderEncoder.packHeader(message, header);

    assertEquals(0, header.getTtl().longValue());
  }
}
