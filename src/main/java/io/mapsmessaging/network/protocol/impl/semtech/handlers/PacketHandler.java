package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.MAX_EVENTS;
import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PULL_DATA;
import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PUSH_DATA;
import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.TX_ACK;

import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import java.util.Arrays;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class PacketHandler {

  private static final PacketHandler instance = new PacketHandler();

  public static PacketHandler getInstance() {
    return instance;
  }

  private final Handler[] handlers;

  @Getter
  private final MessageStateContext messageStateContext;

  private PacketHandler() {
    messageStateContext = new MessageStateContext();
    handlers = new Handler[MAX_EVENTS];
    Arrays.fill(handlers, null);
    handlers[PUSH_DATA] = new PushDataHandler();
    handlers[PULL_DATA] = new PullDataHandler();
    handlers[TX_ACK] = new TxAckHandler();
  }

  public void handle(@NotNull @NonNull SemTechProtocol protocol, @NotNull @NonNull SemTechPacket packet) {
    Handler handler = handlers[packet.getIdentifier()];
    if (handler != null) {
      handler.process(protocol, packet);
    }
  }
}
