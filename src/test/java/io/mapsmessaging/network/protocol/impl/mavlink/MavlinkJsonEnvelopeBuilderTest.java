/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import io.mapsmessaging.mavlink.message.Frame;
import io.mapsmessaging.mavlink.message.Version;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MavlinkJsonEnvelopeBuilderTest {

  @Test
  void preservesVehicleIdentitySequenceAndRawPayload() {
    Frame frame = frame(new byte[]{1, 2, (byte) 255});
    JsonObject envelope = MavlinkJsonEnvelopeBuilder.toJson(frame, Map.of("battery_remaining", 86));

    assertEquals("V2", envelope.get("version").getAsString());
    assertEquals(147, envelope.get("messageId").getAsInt());
    assertEquals(3, envelope.get("systemId").getAsInt());
    assertEquals(7, envelope.get("componentId").getAsInt());
    assertEquals(42, envelope.get("sequence").getAsInt());
    assertTrue(envelope.get("signed").getAsBoolean());
    assertEquals("AQL/", envelope.getAsJsonObject("payload").get("rawBase64").getAsString());
    assertEquals(86, envelope.getAsJsonObject("payload").getAsJsonObject("decoded").get("battery_remaining").getAsInt());
  }

  @Test
  void retainsNestedTelemetryTypesAndArrays() {
    Frame frame = frame(new byte[]{9});
    Map<String, Object> telemetry = Map.of("armed", true, "altitude", 123.25, "heading", -2,
        "temperatures", List.of(21, 22, 23), "status", Map.of("source", "autopilot", "healthy", true));

    JsonObject decoded = MavlinkJsonEnvelopeBuilder.toJson(frame, telemetry).getAsJsonObject("payload").getAsJsonObject("decoded");

    assertTrue(decoded.get("armed").getAsBoolean());
    assertEquals(123.25, decoded.get("altitude").getAsDouble());
    assertEquals(-2, decoded.get("heading").getAsInt());
    assertEquals(3, decoded.getAsJsonArray("temperatures").size());
    assertEquals("autopilot", decoded.getAsJsonObject("status").get("source").getAsString());
    assertTrue(decoded.getAsJsonObject("status").get("healthy").getAsBoolean());
  }

  @Test
  void missingDecodedTelemetryIsRepresentedAsJsonNull() {
    JsonObject decoded = MavlinkJsonEnvelopeBuilder.toJson(frame(new byte[0]), null);
    assertTrue(decoded.getAsJsonObject("payload").get("decoded").isJsonNull());
    assertEquals("", decoded.getAsJsonObject("payload").get("rawBase64").getAsString());
  }

  private static Frame frame(byte[] payload) {
    Frame frame = mock(Frame.class);
    when(frame.getVersion()).thenReturn(Version.V2);
    when(frame.getMessageId()).thenReturn(147);
    when(frame.getSystemId()).thenReturn(3);
    when(frame.getComponentId()).thenReturn(7);
    when(frame.getSequence()).thenReturn(42);
    when(frame.getPayloadLength()).thenReturn(payload.length);
    when(frame.isSigned()).thenReturn(true);
    when(frame.getPayload()).thenReturn(payload);
    return frame;
  }
}
