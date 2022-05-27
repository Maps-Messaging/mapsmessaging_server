package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PullAck;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PullData;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;

public class PullDataHandler extends Handler {

  @Override
  public void process(SemTechProtocol protocol, SemTechPacket packet) {
    PullData pullData = (PullData) packet;
    protocol.sendPacket(new PullAck(pullData.getToken(), packet.getFromAddress()));
    sendMessage(protocol, packet.getFromAddress());
  }

}
