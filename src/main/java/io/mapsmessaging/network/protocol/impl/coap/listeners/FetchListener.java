package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import org.jetbrains.annotations.Nullable;

public class FetchListener extends GetListener {

  @Override
  protected @Nullable String getSelector(BasePacket packet){
    byte[] payload = packet.getPayload();
    if(payload != null){
      return new String(payload);
    }
    return null;
  }

}