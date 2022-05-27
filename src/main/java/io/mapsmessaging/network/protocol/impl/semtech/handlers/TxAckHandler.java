package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import io.mapsmessaging.network.protocol.impl.semtech.packet.TxAcknowledge;

public class TxAckHandler extends Handler {

  @Override
  public void process(SemTechProtocol protocol, SemTechPacket packet) {
    TxAcknowledge txAck = (TxAcknowledge) packet;
    PacketHandler.getInstance().getMessageStateContext().complete(txAck.getToken());
    sendMessage(protocol, packet.getFromAddress());
  }

}
