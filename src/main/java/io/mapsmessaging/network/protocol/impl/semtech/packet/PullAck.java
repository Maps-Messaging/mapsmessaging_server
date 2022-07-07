package io.mapsmessaging.network.protocol.impl.semtech.packet;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PULL_ACK;

import java.net.SocketAddress;
import lombok.ToString;

/**
 * ### 5.3. PULL_ACK packet ###
 *
 * That packet type is used by the server to confirm that the network route is open and that the server can send PULL_RESP packets at any time.
 *
 * Bytes  | Function :------:|--------------------------------------------------------------------- 0      | protocol version = 2 1-2    | same token as the PULL_DATA packet to
 * acknowledge 3      | PULL_ACK identifier 0x04
 */

@ToString
public class PullAck extends Ack {

  public PullAck(int token, SocketAddress address) {
    super(token, PULL_ACK, address);
  }

  @Override
  public int getIdentifier() {
    return PULL_ACK;
  }

}
