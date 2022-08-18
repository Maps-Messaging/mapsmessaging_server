package io.mapsmessaging.network.protocol.impl.coap.listeners;

import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.URI_PATH;

import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.UriPath;

public class PatchListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) {
    OptionSet optionSet = request.getOptions();
    String path = "/";
    if (optionSet.hasOption(URI_PATH)) {
      UriPath uriPath = (UriPath) optionSet.getOption(URI_PATH);
      path = uriPath.toString();
    }
    if (request.getType().equals(TYPE.CON)) {
      // Need to create a response here!!!
    }
    return null;
  }
}