package io.mapsmessaging.network.protocol.impl.semtech.packet;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PULL_RESPONSE;
import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

import io.mapsmessaging.network.io.Packet;
import java.net.SocketAddress;
import lombok.ToString;

/**
 * ### 5.4. PULL_RESP packet ###
 *
 * That packet type is used by the server to send RF packets and associated metadata that will have to be emitted by the gateway.
 *
 * Bytes  | Function :------:|--------------------------------------------------------------------- 0      | protocol version = 2 1-2    | random token 3      | PULL_RESP
 * identifier 0x03 4-end  | JSON object, starting with {, ending with }, see section 6
 */
@ToString
public class PullResponse extends SemTechPacket {

  private final int token;
  private final byte[] jsonObject;

  public PullResponse(int token, byte[] jsonObject, SocketAddress fromAddress) {
    super(fromAddress);
    this.token = token;
    this.jsonObject = jsonObject;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.putByte(VERSION);
    packet.putShort(token);
    packet.putByte(PULL_RESPONSE);
    packet.put(jsonObject);
    return 4 + jsonObject.length;
  }

  @Override
  public int getIdentifier() {
    return PULL_RESPONSE;
  }
}
