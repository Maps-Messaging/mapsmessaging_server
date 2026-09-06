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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Connect5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PacketFactory5;
import io.mapsmessaging.test.ProtocolFragmentationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Mqtt5FragmentationTest {

  private static final String RESOURCE = "/mqtt_5_0.txt";

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 5, 10, 100, 1024, 10240})
  void parses_connect_across_chunk_sizes(int chunkSize) throws Exception {
    byte[] source = ProtocolFragmentationTestSupport.readHexResourceLine(getClass(), RESOURCE, 0);
    Mqtt5PacketProcessor processor = new Mqtt5PacketProcessor();

    ProtocolFragmentationTestSupport.feed(source, chunkSize, processor::process);

    assertConnect(processor.getPacket());
  }

  @Test
  void parses_connect_at_every_split_point() throws Exception {
    byte[] source = ProtocolFragmentationTestSupport.readHexResourceLine(getClass(), RESOURCE, 0);

    for (int split = 1; split < source.length; split++) {
      Mqtt5PacketProcessor processor = new Mqtt5PacketProcessor();
      ProtocolFragmentationTestSupport.feed(source, split, source.length, processor::process);
      assertConnect(processor.getPacket());
    }
  }

  private void assertConnect(MQTTPacket5 packet) {
    assertNotNull(packet);
    Connect5 connect = assertInstanceOf(Connect5.class, packet);
    assertEquals("myclientid", connect.getSessionId());
    assertEquals(5, connect.getProtocolLevel());
  }

  private static final class Mqtt5PacketProcessor {

    private final PacketFactory5 packetFactory;
    private MQTTPacket5 packet;

    private Mqtt5PacketProcessor() {
      MQTT5Protocol protocol = mock(MQTT5Protocol.class);
      when(protocol.getMaxBufferSize()).thenReturn(1024L * 1024L);
      packetFactory = new PacketFactory5(protocol);
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

    private MQTTPacket5 getPacket() {
      return packet;
    }
  }
}
