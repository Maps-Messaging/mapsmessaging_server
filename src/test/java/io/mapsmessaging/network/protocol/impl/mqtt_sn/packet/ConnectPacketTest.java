/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.BaseMqttSnConfig;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class ConnectPacketTest extends BaseMqttSnConfig {


  abstract PacketTransport createTransport(String host) throws Exception;

  void sendPacket(PacketTransport packetTransport, Packet packet) throws Exception {
    packetTransport.sendPacket(packet);
  }
  void readPacket(PacketTransport packetTransport, Packet packet) throws IOException {
    packetTransport.readPacket(packet);
  }

  @ParameterizedTest
  @MethodSource
  public void testMissingExpiry(int version) throws Exception {
    try(PacketTransport packetTransport = createTransport("localhost")) {
      Packet packet = new Packet(1024, false);
      byte len = 1;
      packet.put(len);
      packet.put((byte) 0x4); // Connect
      packet.put((byte) 0x0); // Flags
      packet.put((byte) version); // Version
      if(version == 2) {
        MQTTPacket.writeShort(packet, 60); // KeepAlive
        MQTTPacket.writeInt(packet, 1500); // Max Buffer Size
      }
      len = (byte) packet.position();
      packet.put(0, len);
      packet.flip();
      sendPacket(packetTransport, packet);
      packet.clear();
      long time = System.currentTimeMillis()+10000;
      while(time > System.currentTimeMillis() && packet.position() == 0) {
        readPacket(packetTransport, packet);
      }
      packet.flip();
      Assertions.assertTrue(packet.hasRemaining());
      Assertions.assertEquals(0x5, packet.get(1));
      Assertions.assertEquals(0x3, packet.get(2));
    }
  }
  private static Stream<Arguments> testMissingExpiry() {
    return createVersionStream();
  }

  @ParameterizedTest
  @MethodSource
  public void testInvalidFlags(int version) throws Exception {
    try(PacketTransport packetTransport = createTransport("localhost")) {
      Packet packet = new Packet(1024, false);
      byte len = 1;
      packet.put(len);
      packet.put((byte) 0x4); // Connect
      packet.put((byte) 0xff); // Flags
      packet.put((byte) version); // Version
      MQTTPacket.writeShort(packet, 60); // KeepAlive
      if(version == 1){
        packet.put("TestConnectionPacket".getBytes(StandardCharsets.UTF_8));
      }
      if(version == 2) {
        MQTTPacket.writeInt(packet, 0x1ffff); // SessionTime
        MQTTPacket.writeShort(packet, 1500); // Buffersize
        MQTTPacket.writeRawBuffer("TestConnectionPacket".getBytes(), packet);
      }
      len = (byte) packet.position();
      packet.put(0, len);
      packet.flip();
      packet.setFromAddress(new InetSocketAddress("localhost", 1884));
      sendPacket(packetTransport, packet);
      packet.clear();
      long time = System.currentTimeMillis()+10000;
      while(time > System.currentTimeMillis() && packet.position() == 0) {
        readPacket(packetTransport, packet);
      }
      packet.flip();
      Assertions.assertTrue(packet.hasRemaining());
      Assertions.assertEquals(packet.get(1), 0x5);
      Assertions.assertEquals(packet.get(2), 0x3);
    }
  }
  private static Stream<Arguments> testInvalidFlags() {
    return createVersionStream();
  }


  @ParameterizedTest
  @MethodSource
  public void testWillRequests(int version) throws Exception {
    try(PacketTransport packetTransport = createTransport("localhost")) {
      Packet packet = new Packet(1024, false);
      byte len = 1;
      packet.put(len);
      packet.put((byte) 0x4); // Connect
      if(version == 1) {
        packet.put((byte) 0b01100); // Set Will Flag and Clear
      }
      else{
        packet.put((byte) 0b011); // Set Will Flag and Clear
      }
      packet.put((byte) version); // Version
      MQTTPacket.writeShort(packet, 60); // KeepAlive
      if(version == 1){
        packet.put("TestConnectionPacket".getBytes(StandardCharsets.UTF_8));
      }
      if(version == 2) {
        MQTTPacket.writeInt(packet, 0x1ffff); // SessionTime
        MQTTPacket.writeShort(packet, 1500); // Buffersize
        MQTTPacket.writeRawBuffer( "TestConnectionPacket".getBytes(), packet);
      }
      len = (byte) packet.position();
      packet.put(0, len);
      packet.flip();
      packet.setFromAddress(new InetSocketAddress("localhost", 1884));
      sendPacket(packetTransport, packet);
      packet.clear();
      long time = System.currentTimeMillis()+10000;
      while(time > System.currentTimeMillis() && packet.position() == 0) {
        readPacket(packetTransport, packet);
      }
      packet.flip();
      Assertions.assertTrue(packet.hasRemaining());
      Assertions.assertEquals(packet.get(1), 0x6);
      packet.clear(); // Send Will Topic

      packet.putByte(0);
      packet.putByte(0x7);
      packet.putByte(0);
      packet.putByte("willTopicName".length());
      packet.put("willTopicName".getBytes(StandardCharsets.UTF_8));
      if(version == 2){
        packet.putByte(0b00110000);
      }
      packet.put(0, (byte)packet.position());
      packet.flip();
      sendPacket(packetTransport, packet);
      packet.clear();
      time = System.currentTimeMillis()+10000;
      while(time > System.currentTimeMillis() && packet.position() == 0) {
        readPacket(packetTransport, packet);
      }
      packet.flip();
      Assertions.assertTrue(packet.hasRemaining());
      Assertions.assertEquals(packet.get(1), 0x8);
      packet.clear(); // Send Will Topic

      packet.put((byte)0);
      packet.put((byte) 0x9);
      packet.put("This is my will message".getBytes(StandardCharsets.UTF_8));
      packet.put(0, (byte)packet.position());
      packet.flip();
      sendPacket(packetTransport, packet);
      packet.clear();
      time = System.currentTimeMillis()+10000;
      while(time > System.currentTimeMillis() && packet.position() == 0) {
        readPacket(packetTransport, packet);
      }
      packet.flip();
      Assertions.assertTrue(packet.hasRemaining());
      Assertions.assertEquals(packet.get(1), 0x5);
      Assertions.assertEquals(packet.get(2), 0x0);
      packet.clear();

      packet.put((byte)2);
      packet.put((byte) 0x18);
      packet.flip();
      sendPacket(packetTransport, packet);
      packet.clear();
      time = System.currentTimeMillis()+10000;
      while(time > System.currentTimeMillis() && packet.position() == 0) {
        readPacket(packetTransport, packet);
      }
      packet.flip();
      Assertions.assertTrue(packet.hasRemaining());
      Assertions.assertEquals(packet.get(1), 0x18);
      TimeUnit.SECONDS.sleep(11);
    }
  }
  private static Stream<Arguments> testWillRequests() {
    return createVersionStream();
  }



}
