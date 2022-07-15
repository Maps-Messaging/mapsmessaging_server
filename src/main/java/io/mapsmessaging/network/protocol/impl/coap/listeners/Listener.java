package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import java.io.IOException;

public abstract class Listener {


  public abstract BasePacket handle(BasePacket request, CoapProtocol protocol) throws IOException;

}
