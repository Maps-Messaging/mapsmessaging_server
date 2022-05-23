package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import java.io.IOException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class CoapProtocol extends ProtocolImpl {

  protected CoapProtocol(@NonNull @NotNull EndPoint endPoint) {
    super(endPoint);
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {

  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    return false;
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
