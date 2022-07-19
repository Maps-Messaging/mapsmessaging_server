package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PacketPipeline {

  private final Queue<BasePacket> sendQueue;
  private final CoapProtocol protocol;

  public PacketPipeline(CoapProtocol protocol) {
    sendQueue = new ConcurrentLinkedQueue<>();
    this.protocol = protocol;
  }

  public void send(BasePacket packet) throws IOException {
    if (sendQueue.isEmpty()) {
      sendQueue.offer(packet);
      protocol.sendResponse(packet);
    } else {
      sendQueue.offer(packet);
    }
  }

}
