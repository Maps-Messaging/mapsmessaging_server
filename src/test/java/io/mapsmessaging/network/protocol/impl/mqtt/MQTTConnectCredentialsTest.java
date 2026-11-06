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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.config.protocol.impl.MqttConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Connect5;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MQTTConnectCredentialsTest {
  private static final AtomicInteger IDS = new AtomicInteger();

  static Stream<Arguments> credentials() {
    return Stream.of(
        Arguments.of(false, null, null, 0),
        Arguments.of(false, "user", null, 0x80),
        Arguments.of(false, "user", "pass", 0xc0),
        Arguments.of(false, "", null, 0x80),
        Arguments.of(false, "", "", 0xc0),
        Arguments.of(false, "üser", "pass", 0xc0),
        Arguments.of(true, null, null, 0),
        Arguments.of(true, "user", null, 0x80),
        Arguments.of(true, null, "pass", 0x40),
        Arguments.of(true, "user", "pass", 0xc0),
        Arguments.of(true, "", null, 0x80),
        Arguments.of(true, null, "", 0x40),
        Arguments.of(true, "", "", 0xc0),
        Arguments.of(true, "  ", "pass", 0xc0));
  }

  @ParameterizedTest
  @MethodSource("credentials")
  void encoder_matches_credential_flags_and_payload(boolean mqtt5, String username, String password, int flags) throws Exception {
    ServerPacket connect;
    if (mqtt5) {
      Connect5 frame = new Connect5();
      frame.setSessionId("client");
      frame.setUsername(username);
      frame.setPassword(password == null ? null : password.toCharArray());
      connect = frame;
    } else {
      Connect frame = new Connect();
      frame.setSessionId("client");
      frame.setUsername(username);
      frame.setPassword(password == null ? null : password.toCharArray());
      connect = frame;
    }
    assertCredentials(connect, mqtt5, username, password, flags);
  }

  @ParameterizedTest
  @MethodSource("credentials")
  void outbound_protocol_emits_optional_credentials(boolean mqtt5, String username, String password, int flags) throws Exception {
    Fixture fixture = fixture(mqtt5);
    try {
      fixture.protocol.connect("client", username, password);
      assertEquals(1, fixture.frames.size());
      assertCredentials(fixture.frames.getFirst(), mqtt5, username, password, flags);
    } finally {
      fixture.protocol.setConnected(false);
      fixture.protocol.close();
    }
  }

  @Test
  void mqtt311_encoder_rejects_password_without_username_before_writing() {
    Connect frame = new Connect();
    frame.setSessionId("client");
    frame.setPassword("pass".toCharArray());
    Packet packet = new Packet(ByteBuffer.allocate(256));
    assertThrows(IllegalArgumentException.class, () -> frame.packFrame(packet));
    assertEquals(0, packet.position());
  }

  @Test
  void mqtt311_protocol_rejects_password_without_username_before_sending() throws Exception {
    Fixture fixture = fixture(false);
    try {
      assertThrows(IOException.class, () -> fixture.protocol.connect("client", null, "pass"));
      assertTrue(fixture.frames.isEmpty());
    } finally {
      fixture.protocol.setConnected(false);
      fixture.protocol.close();
    }
  }

  private static void assertCredentials(ServerPacket connect, boolean mqtt5, String username, String password, int flags) throws Exception {
    Packet packet = new Packet(ByteBuffer.allocate(1024));
    connect.packFrame(packet);
    packet.flip();
    byte fixed = packet.get();
    assertEquals(0x10, fixed & 0xff);
    long remaining = MQTTPacket.readVariableInt(packet);
    assertEquals(packet.available(), remaining, "Remaining Length must cover exactly the emitted bytes");
    int bodyStart = packet.position();
    assertEquals(flags, packet.getRawBuffer().get(bodyStart + 7) & 0xc0);
    if (mqtt5) {
      Connect5 decoded = new Connect5(fixed, remaining, packet);
      assertEquals("client", decoded.getSessionId());
      assertEquals(username, decoded.getUsername());
      assertArrayEquals(password == null ? null : password.toCharArray(), decoded.getPassword());
    } else {
      Connect decoded = new Connect(fixed, remaining, packet);
      assertEquals("client", decoded.getSessionId());
      assertEquals(username, decoded.getUsername());
      assertArrayEquals(password == null ? null : password.toCharArray(), decoded.getPassword());
    }
    assertFalse(packet.hasRemaining(), "No trailing credential bytes");
  }

  private static Fixture fixture(boolean mqtt5) throws Exception {
    EndPoint endpoint = mock(EndPoint.class);
    EndPointServerConfigDTO config = mock(EndPointServerConfigDTO.class);
    MqttConfig mqttConfig = new MqttConfig();
    mqttConfig.setMaxServerKeepAlive(60);
    when(config.getProtocolConfig("mqtt")).thenReturn(mqttConfig);
    when(config.getEndPointConfig()).thenReturn(new EndPointConfigDTO("tcp"));
    when(endpoint.getConfig()).thenReturn(config);
    int id = IDS.incrementAndGet();
    when(endpoint.getName()).thenReturn("credentials-" + id);
    when(endpoint.getJMXTypePath()).thenReturn(List.of("Credentials=" + id));
    when(endpoint.getEndPointStatus()).thenReturn(mock(EndPointStatus.class));
    when(endpoint.getServer()).thenReturn(mock(EndPointServerStatus.class));
    Protocol protocol = mqtt5 ? spy(new MQTT5Protocol(endpoint)) : spy(new MQTTProtocol(endpoint));
    Field selector = protocol.getClass().getDeclaredField("selectorTask");
    selector.setAccessible(true);
    selector.set(protocol, mock(SelectorTask.class));
    List<ServerPacket> frames = new ArrayList<>();
    if (mqtt5) {
      doAnswer(call -> { frames.add(call.getArgument(0)); return null; }).when((MQTT5Protocol) protocol).writeFrame(any(ServerPacket.class));
    } else {
      doAnswer(call -> { frames.add(call.getArgument(0)); return null; }).when((MQTTProtocol) protocol).writeFrame(any(ServerPacket.class));
    }
    return new Fixture(protocol, frames);
  }

  private record Fixture(Protocol protocol, List<ServerPacket> frames) {}
}
