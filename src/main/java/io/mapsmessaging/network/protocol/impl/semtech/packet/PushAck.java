package io.mapsmessaging.network.protocol.impl.semtech.packet;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PUSH_ACK;

import java.net.SocketAddress;

/**
 * ### 3.3. PUSH_ACK packet ###
 *
 * That packet type is used by the server to acknowledge immediately all the PUSH_DATA packets received.
 *
 * Bytes  | Function
 * -------|---------------------------------------------------------------------
 * 0      | protocol version = 2
 * 1-2    | same token as the PUSH_DATA packet to acknowledge
 * 3      | PUSH_ACK identifier 0x01
 */

public class PushAck extends Ack {

  public PushAck(int token, SocketAddress fromAddress) {
    super(token, PUSH_ACK, fromAddress);
  }

  @Override
  public int getIdentifier() {
    return PUSH_ACK;
  }

  public String toString() {
    return "PushAck(token=" + getToken() + ")";
  }
}
