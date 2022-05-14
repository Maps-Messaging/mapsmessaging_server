package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.PacketIntegrityFactory;
import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Assertions;

public class HmacConnectPacketTest extends UDPConnectPacketTest {


  private PacketIntegrity integrity;

  PacketTransport createTransport(String host) throws Exception {
    integrity = PacketIntegrityFactory.getInstance().getPacketIntegrity(
        "HmacSHA512",
        new AppenderSignatureManager(),
        "ThisIsATestKey".getBytes()
    );
    return new UDPPacketTransport(
        new InetSocketAddress(host, 0),
        new InetSocketAddress(host, 1885));
  }


  void sendPacket(PacketTransport packetTransport, Packet packet) throws Exception {
    packet = integrity.secure(packet);
    packetTransport.sendPacket(packet);
  }
  void readPacket(PacketTransport packetTransport, Packet packet) throws IOException {
    packetTransport.readPacket(packet);
    packet.flip();
    Assertions.assertTrue(integrity.isSecure(packet));
  }


}
