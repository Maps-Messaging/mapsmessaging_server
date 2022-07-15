package io.mapsmessaging.network.protocol.impl.coap.listeners;

import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.URI_PATH;

import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.ContentFormat;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Format;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.UriPath;
import java.io.IOException;

public class GetListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) throws IOException {
    BasePacket response = null;
    OptionSet optionSet = request.getOptions();
    String path = "/";
    UriPath uriPath = (UriPath) optionSet.getOption(URI_PATH);
    if(uriPath != null){
      path = uriPath.toString();
    }
    if(request.getType().equals(TYPE.CON)){
      response = request.buildAckResponse(Code.CONTENT);
      response.setPayload("Hello there, hopefully this works".getBytes());
      ContentFormat format = new ContentFormat(Format.TEXT_PLAIN);
      response.getOptions().putOption(format);

    }
    return response;
  }
}
