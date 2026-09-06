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

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PacketFactory;
import io.mapsmessaging.test.ProtocolFragmentationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MqttFragmentationTest {

  private static final String RESOURCE = "/mqtt_3_1_1.txt";

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 5, 10, 100, 1024, 10240})
  void parses_connect_across_chunk_sizes(int chunkSize) throws Exception {
    byte[] source = ProtocolFragmentationTestSupport.readHexResourceLine(getClass(), RESOURCE, 0);
    MqttPacketProcessor processor = new MqttPacketProcessor();

    ProtocolFragmentationTestSupport.feed(source, chunkSize, processor::process);

    assertConnect(processor.getPacket());
  }

  @Test
  void parses_connect_at_every_split_point() throws Exception {
    byte[] source = ProtocolFragmentationTestSupport.readHexResourceLine(getClass(), RESOURCE, 0);

    for (int split = 1; split < source.length; split++) {
      MqttPacketProcessor processor = new MqttPacketProcessor();
      ProtocolFragmentationTestSupport.feed(source, split, source.length, processor::process);
      assertConnect(processor.getPacket());
    }
  }

  private void assertConnect(MQTTPacket packet) {
    assertNotNull(packet);
    Connect connect = assertInstanceOf(Connect.class, packet);
    assertEquals("47e03713a22d496f842f54b9568b4d30", connect.getSessionId());
    assertEquals(4, connect.getProtocolLevel());
  }

  private static final class MqttPacketProcessor {

    private final PacketFactory packetFactory;
    private MQTTPacket packet;

    private MqttPacketProcessor() {
      MQTTProtocol protocol = mock(MQTTProtocol.class);
      EndPoint endPoint = mock(EndPoint.class);
      when(protocol.getEndPoint()).thenReturn(endPoint);
      when(endPoint.isClient()).thenReturn(false);
      when(protocol.getMaximumBufferSize()).thenReturn(1024L * 1024L);
      packetFactory = new PacketFactory(protocol);
    }

    private void process(Packet incoming) throws Exception {
      if (packet != null) {
        return;
      }
      int start = incoming.position();
      try {
        packet = packetFactory.parseFrame(incoming);
      } catch (EndOfBufferException expected) {
        incoming.position(start);
      }
    }

    private MQTTPacket getPacket() {
      return packet;
    }
  }
}
