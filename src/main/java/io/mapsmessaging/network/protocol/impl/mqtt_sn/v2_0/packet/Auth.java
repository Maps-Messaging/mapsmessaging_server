package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import lombok.Getter;
import lombok.ToString;

@ToString
public class Auth extends MQTT_SN_2_Packet {

  @Getter
  private final ReasonCodes reasonCode;
  @Getter
  private final String method;
  @Getter
  private final byte[] data;

  public Auth(ReasonCodes reasonCode, String method, byte[] data) {
    super(AUTH);
    this.reasonCode = reasonCode;
    this.method = method;
    this.data = data;
  }

  public Auth(Packet packet, int length) {
    super(AUTH);
    reasonCode = ReasonCodes.lookup(packet.get());
    int methodLen = packet.get();
    byte[] tmp = new byte[methodLen];
    packet.get(tmp, 0, tmp.length);
    method = new String(tmp);
    data = MQTTPacket.readRemaining(packet, length - (4+methodLen));
  }

  @Override
  public int packFrame(Packet packet) {
    int len = 4 + method.length() + data.length;
    len = packLength(packet, len);
    packet.put((byte) AUTH);
    packet.put((byte) reasonCode.getValue());
    byte[] buf = method.getBytes();
    packet.put((byte) buf.length);
    packet.put(buf);
    packet.put(data);
    return len;
  }
}