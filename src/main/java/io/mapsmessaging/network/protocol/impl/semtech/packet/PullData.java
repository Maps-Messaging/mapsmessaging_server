package io.mapsmessaging.network.protocol.impl.semtech.packet;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PULL_DATA;
import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.ToString;

/**
 * ### 5.2. PULL_DATA packet ###
 *
 * That packet type is used by the gateway to poll data from the server.
 *
 * This data exchange is initialized by the gateway because it might be impossible for the server to send packets to the gateway if the gateway is behind a NAT.
 *
 * When the gateway initialize the exchange, the network route towards the server will open and will allow for packets to flow both directions. The gateway must periodically send
 * PULL_DATA packets to be sure the network route stays open for the server to be used at any time.
 *
 * Bytes  | Function
 * :------:|---------------------------------------------------------------------
 * 0      | protocol version = 2
 * 1-2    | random token
 * 3      | PULL_DATA identifier 0x02
 * 4-11   | Gateway unique identifier (MAC address)
 */

@ToString
public class PullData extends SemTechPacket {

  @Getter
  private final int token;
  @Getter
  private final byte[] gatewayIdentifier;

  public PullData(int token, Packet packet) {
    super(packet.getFromAddress());
    this.token = token;
    gatewayIdentifier = new byte[8];
    packet.get(gatewayIdentifier);
  }

  @Override
  public int packFrame(Packet packet) {
    packet.putByte(VERSION);
    packet.putShort(token);
    packet.putByte(PULL_DATA);
    packet.put(gatewayIdentifier);
    return 12;
  }

  @Override
  public int getIdentifier() {
    return PULL_DATA;
  }

}