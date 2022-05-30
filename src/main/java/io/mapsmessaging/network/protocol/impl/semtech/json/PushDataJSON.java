package io.mapsmessaging.network.protocol.impl.semtech.json;

import lombok.Getter;
import lombok.Setter;

public class PushDataJSON {

  @Getter
  @Setter
  private ReceivePacket[] rxpk;

  @Getter
  @Setter
  private StatPacket stats;

}
