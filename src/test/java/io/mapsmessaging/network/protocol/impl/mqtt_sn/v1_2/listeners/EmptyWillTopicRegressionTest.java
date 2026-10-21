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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.WillTask;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.*;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmptyWillTopicRegressionTest {

  @Test
  void parsed_empty_will_topic_encodes_without_flags() throws Exception {
    WillTopic topic = (WillTopic) parse((byte) 2, (byte) MQTT_SNPacket.WILLTOPIC);
    assertNull(topic.getTopic());
    ByteBuffer buffer = ByteBuffer.allocate(8);
    assertEquals(2, topic.packFrame(new Packet(buffer)));
    assertEquals(2, buffer.position());
    assertArrayEquals(new byte[]{2, (byte) MQTT_SNPacket.WILLTOPIC},
        new byte[]{buffer.get(0), buffer.get(1)});
  }

  @Test
  void empty_update_with_no_existing_will_does_not_create_will() throws Exception {
    Session session = mock(Session.class);
    WillTopicUpdate empty = (WillTopicUpdate) parse((byte) 2, (byte) MQTT_SNPacket.WILLTOPICUPD);
    Protocol protocol = mock(Protocol.class, RETURNS_DEEP_STUBS);
    when(protocol.getProtocolConfig().getMessageDefaults()).thenReturn(null);
    WillTopicResponse response = (WillTopicResponse) new WillTopicUpdateListener()
        .handlePacket(empty, session, null, protocol, null);
    assertEquals(ReasonCodes.SUCCESS, response.getStatus());
    verify(session).getWillTask();
    verifyNoMoreInteractions(session);
  }

  @Test
  void parsed_empty_update_cancels_existing_will_without_updating_settings() throws Exception {
    Session session = mock(Session.class);
    WillTask task = mock(WillTask.class);
    when(session.getWillTask()).thenReturn(task);
    WillTopicUpdate empty = (WillTopicUpdate) parse((byte) 2, (byte) MQTT_SNPacket.WILLTOPICUPD);
    assertNull(empty.getTopic());
    WillTopicResponse response = (WillTopicResponse) new WillTopicUpdateListener()
        .handlePacket(empty, session, null, null, null);
    assertEquals(ReasonCodes.SUCCESS, response.getStatus());
    verify(task).cancel();
    verifyNoMoreInteractions(task);
  }

  @Test
  void nonempty_will_topic_retains_flags_and_payload_on_round_trip() throws Exception {
    byte[] wire = {6, (byte) MQTT_SNPacket.WILLTOPIC, 0x50, 'a', '/', 'b'};
    WillTopic topic = (WillTopic) parse(wire);
    assertEquals("a/b", topic.getTopic());
    assertEquals(QualityOfService.EXACTLY_ONCE, topic.getQoS());
    assertTrue(topic.getRetain());
    ByteBuffer buffer = ByteBuffer.allocate(wire.length);
    assertEquals(wire.length, topic.packFrame(new Packet(buffer)));
    assertArrayEquals(wire, buffer.array());
  }

  private MQTT_SNPacket parse(byte... bytes) throws Exception {
    return new PacketFactory().parseFrame(new Packet(ByteBuffer.wrap(bytes)));
  }
}
