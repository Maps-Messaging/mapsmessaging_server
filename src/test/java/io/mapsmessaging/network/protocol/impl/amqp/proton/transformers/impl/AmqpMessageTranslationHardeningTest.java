/*
 *
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.encoders.PropertiesEncoder;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.messaging.Header;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.amqp.messaging.Properties;
import org.junit.jupiter.api.Test;

class AmqpMessageTranslationHardeningTest {

  private static final Symbol DELIVERY_TIME = Symbol.valueOf("x-opt-delivery-time");

  @Test
  void delayAnnotationBecomesNativeMapsDelayAndIsPreservedOnForward() {
    long schedule = System.currentTimeMillis() + 1_500;
    org.apache.qpid.proton.message.Message source =
        org.apache.qpid.proton.message.Message.Factory.create();
    source.setMessageAnnotations(new MessageAnnotations(
        Map.of(DELIVERY_TIME, schedule)));
    MessageBuilder builder = new MessageBuilder();

    new BaseMessageTranslator().decode(builder, source);

    assertTrue(builder.getDelayed() > 0);
    assertTrue(builder.getDelayed() <= 1_500);

    Message message = outgoingMessage(builder.getDelayed());
    org.apache.qpid.proton.message.Message encoded = new BaseMessageTranslator().encode(message);
    long scheduled = (Long) encoded.getMessageAnnotations().getValue().get(DELIVERY_TIME);
    assertTrue(scheduled >= System.currentTimeMillis());
  }

  @Test
  void exactAbsoluteExpirySurvivesAmqpRoundTrip() {
    long expiration = System.currentTimeMillis() + 10_000;
    Properties received = new Properties();
    received.setAbsoluteExpiryTime(new Date(expiration));
    org.apache.qpid.proton.message.Message source =
        org.apache.qpid.proton.message.Message.Factory.create();
    source.setProperties(received);
    MessageBuilder decoded = new MessageBuilder();

    new BaseMessageTranslator().decode(decoded, source);

    assertEquals(Long.toString(expiration),
        decoded.getMeta().get(PropertiesEncoder.ORIGINAL_ABSOLUTE_EXPIRY));

    Properties outgoing = new Properties();
    PropertiesEncoder.packProperties(outgoing, outgoingMessage(decoded.getMeta()));
    assertEquals(expiration, outgoing.getAbsoluteExpiryTime().getTime());
  }

  @Test
  void senderCreationTimeAndTtlPreserveExactJmsExpirationWithoutAbsoluteProperty() {
    long creation = System.currentTimeMillis();
    long ttl = 5_000;
    Properties received = new Properties();
    received.setCreationTime(new Date(creation));
    Header header = new Header();
    header.setTtl(UnsignedInteger.valueOf(ttl));
    org.apache.qpid.proton.message.Message source =
        org.apache.qpid.proton.message.Message.Factory.create();
    source.setProperties(received);
    source.setHeader(header);
    MessageBuilder decoded = new MessageBuilder();

    new BaseMessageTranslator().decode(decoded, source);

    assertEquals(Long.toString(creation + ttl),
        decoded.getMeta().get(PropertiesEncoder.ORIGINAL_ABSOLUTE_EXPIRY));
  }

  private static Message outgoingMessage(long delay) {
    Message message = mock(Message.class);
    when(message.getPriority()).thenReturn(Priority.NORMAL);
    when(message.getQualityOfService()).thenReturn(QualityOfService.AT_MOST_ONCE);
    when(message.getDelayed()).thenReturn(System.currentTimeMillis() + delay);
    when(message.getDataMap()).thenReturn(new LinkedHashMap<>());
    when(message.getMeta()).thenReturn(new LinkedHashMap<>());
    when(message.getCreation()).thenReturn(System.currentTimeMillis());
    return message;
  }

  private static Message outgoingMessage(Map<String, String> meta) {
    Message message = mock(Message.class);
    when(message.getDataMap()).thenReturn(new LinkedHashMap<>());
    when(message.getMeta()).thenReturn(meta);
    when(message.getCreation()).thenReturn(System.currentTimeMillis());
    return message;
  }
}
