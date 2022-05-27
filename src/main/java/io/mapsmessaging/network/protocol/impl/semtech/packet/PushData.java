package io.mapsmessaging.network.protocol.impl.semtech.packet;


import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.PUSH_DATA;
import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

import io.mapsmessaging.network.io.Packet;
import java.nio.charset.StandardCharsets;
import lombok.Getter;

/**
 * ### 3.2. PUSH_DATA packet ###
 *
 * That packet type is used by the gateway mainly to forward the RF packets received, and associated metadata, to the server.
 *
 * Bytes  | Function :------:|--------------------------------------------------------------------- 0      | protocol version = 2 1-2    | random token 3      | PUSH_DATA
 * identifier 0x00 4-11   | Gateway unique identifier (MAC address) 12-end | JSON object, starting with {, ending with }, see section 4
 */

public class PushData extends SemTechPacket {

  @Getter
  private final int token;
  @Getter
  private final int identifier;
  @Getter
  private final byte[] gatewayIdentifier;
  @Getter
  private final String jsonObject;

  public PushData(int token, Packet packet) {
    super(packet.getFromAddress());
    identifier = PUSH_DATA;
    this.token = token;

    gatewayIdentifier = new byte[8];
    packet.get(gatewayIdentifier);
    byte[] tmp = new byte[packet.available()];
    packet.get(tmp);
    jsonObject = new String(tmp);
  }

  public boolean isValid() {
    return jsonObject.startsWith("{") && jsonObject.endsWith("}") && identifier == PUSH_DATA;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.putByte(VERSION);
    packet.putShort(token);
    packet.putByte(PUSH_DATA);
    packet.put(gatewayIdentifier);
    packet.put(jsonObject.getBytes(StandardCharsets.UTF_8));
    return 0;
  }

  @Override
  public int getIdentifier() {
    return PUSH_DATA;
  }
}
