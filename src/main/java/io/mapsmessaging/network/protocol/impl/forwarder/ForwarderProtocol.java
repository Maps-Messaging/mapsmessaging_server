package io.mapsmessaging.network.protocol.impl.forwarder;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class ForwarderProtocol  extends ProtocolImpl {

  private final SelectorTask selectorTask;

  protected ForwarderProtocol(@NonNull @NotNull EndPoint endPoint) throws IOException {
    super(endPoint);
    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    messageEvent.getMessage().getOpaqueData();
    // Send to configure host:port
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    // Send to configured topic

    return true;
  }

  @Override
  public String getName() {
    return "Packet Forwarder";
  }

  @Override
  public String getSessionId() {
    return "";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

}
