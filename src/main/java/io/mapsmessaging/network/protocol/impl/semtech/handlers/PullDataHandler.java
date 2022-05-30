package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import io.mapsmessaging.network.protocol.impl.semtech.GatewayInfo;
import io.mapsmessaging.network.protocol.impl.semtech.GatewayManager;
import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PullAck;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PullData;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class PullDataHandler extends Handler {

  @Override
  public void process(@NotNull @NonNull SemTechProtocol protocol,@NotNull @NonNull SemTechPacket packet) {
    PullData pullData = (PullData) packet;
    protocol.sendPacket(new PullAck(pullData.getToken(), packet.getFromAddress()));
    GatewayInfo info = protocol.getGatewayManager().getInfo(GatewayManager.dumpIdentifier(pullData.getGatewayIdentifier()));
    if(info != null) {
      sendMessage(protocol, info, packet.getFromAddress());
    }
  }

}