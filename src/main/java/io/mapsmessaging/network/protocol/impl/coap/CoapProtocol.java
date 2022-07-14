package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.coap.listeners.Listener;
import io.mapsmessaging.network.protocol.impl.coap.listeners.ListenerFactory;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory;
import java.io.IOException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class CoapProtocol extends ProtocolImpl {
  private final ListenerFactory listenerFactory;
  private final PacketFactory packetFactory;

  protected CoapProtocol(@NonNull @NotNull EndPoint endPoint) {
    super(endPoint);
    listenerFactory = new ListenerFactory();
    packetFactory = new PacketFactory();

  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    throw new UnsupportedOperationException("Not Yet Implemented");
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    BasePacket basePacket = packetFactory.parseFrame(packet);
    if(basePacket != null){
      Listener listener = listenerFactory.getListener(basePacket.getId());
      if(listener != null){
        listener.handle(basePacket, this);
        System.err.println("Handled>>"+basePacket);
      }

    }
    return true;
  }

  @Override
  public String getName() {
    return "CoAP";
  }

  @Override
  public String getSessionId() {
    return null;
  }

  @Override
  public String getVersion() {
    return "RFC7252";
  }
}
