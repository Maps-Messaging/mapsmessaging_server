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

package io.mapsmessaging.api.message.interceptors;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FieldInterceptorTest {

  private static final long CREATION_TIME = 1_700_000_000_000L;

  @Test
  void dataMap_resolvesVirtualJmsAndUtcFields() {
    byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
    Message message = new MessageBuilder()
        .setId(42)
        .setCreation(CREATION_TIME)
        .setCorrelationData("correlation-id")
        .setResponseTopic("reply/topic")
        .setOpaqueData(payload)
        .setPriority(Priority.TWO_ABOVE_NORMAL)
        .setQoS(QualityOfService.AT_LEAST_ONCE)
        .build();

    Map<String, TypedData> dataMap = message.getDataMap();

    assertEquals(42L, dataMap.get("JMSMessageID").getData());
    assertEquals(CREATION_TIME, dataMap.get("JMSTimestamp").getData());
    assertEquals("correlation-id", dataMap.get("JMSCorrelationID").getData());
    assertEquals("reply/topic", dataMap.get("JMSReplyTo").getData());
    assertEquals("PERSISTENT", dataMap.get("JMSDeliveryMode").getData());
    assertEquals(Priority.TWO_ABOVE_NORMAL.getValue(), dataMap.get("JMSPriority").getData());
    assertArrayEquals(payload, (byte[]) dataMap.get("MapsMsgPayload").getData());
    assertEquals(Instant.ofEpochMilli(CREATION_TIME).toString(), dataMap.get("utcTimeIso").getData());
    assertEquals(Instant.ofEpochMilli(CREATION_TIME + 60_000).toString(), dataMap.get("utcExpiryTimeIso").getData());
  }

  @Test
  void deliveryMode_reflectsQualityOfService() {
    Message nonPersistent = new MessageBuilder()
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .build();
    Message persistent = new MessageBuilder()
        .setQoS(QualityOfService.EXACTLY_ONCE)
        .build();

    assertEquals("NON_PERSISTENT", nonPersistent.getDataMap().get("JMSDeliveryMode").getData());
    assertEquals("PERSISTENT", persistent.getDataMap().get("JMSDeliveryMode").getData());
  }

  @Test
  void explicitDataMapValue_takesPrecedenceOverVirtualField() {
    Message message = new MessageBuilder()
        .setId(42)
        .setDataMap(Map.of("JMSMessageID", new TypedData("application-id")))
        .build();

    assertEquals("application-id", message.getDataMap().get("JMSMessageID").getData());
  }

  @Test
  void unknownField_returnsNull() {
    Message message = new MessageBuilder().build();

    assertNull(message.getDataMap().get("not-a-known-field"));
  }
}
