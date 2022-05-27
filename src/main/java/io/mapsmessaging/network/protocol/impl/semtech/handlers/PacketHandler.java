package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.MAX_EVENTS;
import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PULL_DATA;
import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PUSH_DATA;

import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import lombok.Getter;

public class PacketHandler {

  private static final PacketHandler instance = new PacketHandler();

  public static PacketHandler getInstance() {
    return instance;
  }

  private final Handler[] handlers = new Handler[MAX_EVENTS];

  @Getter
  private final MessageStateContext messageStateContext;

  private PacketHandler() {
    messageStateContext = new MessageStateContext();
    handlers[PUSH_DATA] = new PushDataHandler();
    handlers[PULL_DATA] = new PullDataHandler();
  }

  public void handle(SemTechProtocol protocol, SemTechPacket packet) {
    Handler handler = handlers[packet.getIdentifier()];
    if (handler != null) {
      handler.process(protocol, packet);
    }
  }

}
