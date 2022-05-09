package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.BaseMqttSnConfig;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ConnectPacketTest extends BaseMqttSnConfig {


  @ParameterizedTest
  @MethodSource
  public void testMissingExpiry(int version) throws IOException {
    try(UDPPacketTransport packetTransport = new UDPPacketTransport(new InetSocketAddress("localhost", 12000+version))) {
      Packet packet = new Packet(1024, false);
      byte len = 1;
      packet.put(len);
      packet.put((byte) 0x4); // Connect
      packet.put((byte) 0x0); // Flags
      packet.put((byte) version); // Version
      if(version == 2) {
        MQTTPacket.writeShort(packet, 60); // KeepAlive
        MQTTPacket.writeInt(packet, 1500); // Max Buffer Size
        MQTTPacket.writeUTF8(packet, "TestConnectionPacket");
      }
      len = (byte) packet.position();
      packet.put(0, len);
      packet.flip();
      packet.setFromAddress(new InetSocketAddress("localhost", 1884));
      packetTransport.sendPacket(packet);
      packet.clear();
      long time = System.currentTimeMillis()+10000;
      while(time > System.currentTimeMillis() && packet.position() == 0) {
        packetTransport.readPacket(packet);
      }
      packet.flip();
      Assertions.assertTrue(packet.hasRemaining());
      Assertions.assertEquals(packet.get(1), 0x5);
      Assertions.assertEquals(packet.get(2), 0x3);
    }
  }
  private static Stream<Arguments> testMissingExpiry() {
    return createVersionStream();
  }

  @ParameterizedTest
  @MethodSource
  public void testInvalidFlags(int version) throws IOException {
    try(UDPPacketTransport packetTransport = new UDPPacketTransport(new InetSocketAddress("localhost", 12000+version))) {
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
        MQTTPacket.writeUTF8(packet, "TestConnectionPacket");
      }
      len = (byte) packet.position();
      packet.put(0, len);
      packet.flip();
      packet.setFromAddress(new InetSocketAddress("localhost", 1884));
      packetTransport.sendPacket(packet);
      packet.clear();
      long time = System.currentTimeMillis()+10000;
      while(time > System.currentTimeMillis() && packet.position() == 0) {
        packetTransport.readPacket(packet);
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
  public void testWillRequests(int version) throws IOException {
    try(UDPPacketTransport packetTransport = new UDPPacketTransport(new InetSocketAddress("localhost", 12000+version))) {
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
        MQTTPacket.writeUTF8(packet, "TestConnectionPacket");
      }
      len = (byte) packet.position();
      packet.put(0, len);
      packet.flip();
      packet.setFromAddress(new InetSocketAddress("localhost", 1884));
      packetTransport.sendPacket(packet);
      packet.clear();
      long time = System.currentTimeMillis()+10000;
      while(time > System.currentTimeMillis() && packet.position() == 0) {
        packetTransport.readPacket(packet);
      }
      packet.flip();
      Assertions.assertTrue(packet.hasRemaining());
      Assertions.assertEquals(packet.get(1), 0x6);
      packet.clear(); // Send Will Topic

      packet.put((byte)0);
      packet.put((byte) 0x7);
      packet.put("willTopicName".getBytes(StandardCharsets.UTF_8));
      if(version == 2){
        packet.put((byte)0b00110000);
      }
      packet.put(0, (byte)packet.position());
      packet.flip();
      packetTransport.sendPacket(packet);
      packet.clear();
      time = System.currentTimeMillis()+10000;
      while(time > System.currentTimeMillis() && packet.position() == 0) {
        packetTransport.readPacket(packet);
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
      packetTransport.sendPacket(packet);
      packet.clear();
      time = System.currentTimeMillis()+10000;
      while(time > System.currentTimeMillis() && packet.position() == 0) {
        packetTransport.readPacket(packet);
      }
      packet.flip();
      Assertions.assertTrue(packet.hasRemaining());
      Assertions.assertEquals(packet.get(1), 0x5);
      Assertions.assertEquals(packet.get(2), 0x0);
      packet.clear();

      packet.put((byte)2);
      packet.put((byte) 0x18);
      packet.flip();
      packetTransport.sendPacket(packet);
      packet.clear();
      time = System.currentTimeMillis()+10000;
      while(time > System.currentTimeMillis() && packet.position() == 0) {
        packetTransport.readPacket(packet);
      }
      packet.flip();
      Assertions.assertTrue(packet.hasRemaining());
      Assertions.assertEquals(packet.get(1), 0x18);
    }
  }
  private static Stream<Arguments> testWillRequests() {
    return createVersionStream();
  }


}
