package io.mapsmessaging.network.protocol.impl.coap.listeners;

import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.URI_PATH;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.UriPath;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutionException;

public abstract class Listener {


  public abstract BasePacket handle(BasePacket request, CoapProtocol protocol) throws IOException, ExecutionException, InterruptedException;


  protected void publishMessage(BasePacket request,CoapProtocol protocol ){
    HashMap<String, String> meta = new LinkedHashMap<>();
    meta.put("protocol", "CoAP");
    meta.put("version", "1");
    meta.put("time_ms", "" + System.currentTimeMillis());
    OptionSet optionSet = request.getOptions();
    String path = "/";
    UriPath uriPath = (UriPath) optionSet.getOption(URI_PATH);
    if(uriPath != null){
      path = uriPath.toString();
    }

    String finalPath = path;
    protocol.getSession().destinationExists(path).thenApply(exists->{
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(request.getPayload());
      messageBuilder.setQoS(QualityOfService.AT_MOST_ONCE);
      messageBuilder.setRetain(true);
      messageBuilder.setCorrelationData(request.getToken());
      messageBuilder.setMeta(meta);
      messageBuilder.setDataMap( new LinkedHashMap<>());
      protocol.getSession().findDestination(finalPath, DestinationType.TOPIC).thenApply(destination -> {
        if (destination != null) {
          try {
            destination.storeMessage(messageBuilder.build());
            if(request.getType().equals(TYPE.CON)){
              // Need to create a response here!!!
              Code code = Boolean.TRUE.equals(exists) ? Code.CHANGED : Code.CREATED;
              BasePacket response = request.buildAckResponse(code);
              protocol.sendResponse(response);
            }

          } catch (IOException e) {
//            logger.log(ServerLogMessages.MQTT_PUBLISH_STORE_FAILED, e);
            try {
              protocol.close();
            } catch (IOException ioException) {
              // Ignore we are in an error state
            }
          }
        }
        return destination;
      });

      return exists;
    });
  }
}
