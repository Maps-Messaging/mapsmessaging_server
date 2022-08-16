package io.mapsmessaging.network.protocol.impl.coap.listeners;

import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.MAX_AGE;
import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.URI_PATH;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.ETag;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.IfMatch;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.IfNoneMatch;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.MaxAge;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.UriPath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class Listener {


  public abstract BasePacket handle(BasePacket request, CoapProtocol protocol) throws IOException, ExecutionException, InterruptedException;


  protected void publishMessage(BasePacket request,CoapProtocol protocol, boolean isDelete ){

    OptionSet optionSet = request.getOptions();
    String path = "/";
    UriPath uriPath = (UriPath) optionSet.getOption(URI_PATH);
    if(uriPath != null){
      path = uriPath.toString();
    }

    String finalPath = path;
    protocol.getSession().destinationExists(path).thenApply(exists->{
      protocol.getSession().findDestination(finalPath, DestinationType.TOPIC).thenApply(destination -> {
        if (destination != null) {
          try {
            Code code = Boolean.TRUE.equals(exists) ? Code.CHANGED : Code.CREATED;
            if(isDelete)code = Code.DELETED;

            if(request.getType().equals(TYPE.CON)){
              BasePacket response = request.buildAckResponse(code);
              protocol.sendResponse(response);
            }

            if(isDelete || canProcess(destination, request)) {
              destination.storeMessage(build(request));
            }
          } catch (IOException e) {
            e.printStackTrace();
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

  private boolean canProcess(Destination destination, BasePacket request) throws IOException {
    OptionSet optionSet = request.getOptions();
    IfMatch ifMatch = (IfMatch)optionSet.getOption(Constants.IF_MATCH);
    IfNoneMatch ifNoneMatch = (IfNoneMatch) optionSet.getOption(Constants.IF_NONE_MATCH);

    if ((ifMatch != null &&!ifMatch.getList().isEmpty()) || (ifNoneMatch != null && !ifNoneMatch.getList().isEmpty())) {
      Message message = destination.getRetained();
      if (message != null) {
        List<byte[]> etags = extractTags(message);
        if (ifMatch != null) {
          return compareTags(etags, ifMatch.getList());
        }
        return !compareTags(etags, ifNoneMatch.getList());
      }
    }
    return true;
  }

  private boolean compareTags(List<byte[]> etags, List<byte[]> matching){
    for(byte[] match:matching){
      for(byte[] etag:etags){
        if(checkTag(match, etag)){
          return true;
        }
      }
    }
    return false;
  }

  private List<byte[]> extractTags(Message message){
    List<byte[]> etags = new ArrayList<>();
    Map<String, TypedData> map = message.getDataMap();
    for(Entry<String, TypedData> entry:map.entrySet()){
      if(entry.getKey().startsWith("etag_")){
        etags.add((byte[])entry.getValue().getData());
      }
    }
    return etags;
  }

  private boolean checkTag(byte[] lhs, byte[] rhs){
    if(lhs.length != rhs.length) return false;
    for(int x=0;x<lhs.length;x++){
      if(rhs[x] != lhs[x]){
        return false;
      }
    }
    return true;
  }

  protected Message build(BasePacket request){
    MessageBuilder messageBuilder = new MessageBuilder();

    messageBuilder.setOpaqueData(request.getPayload());
    messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
    messageBuilder.setRetain(true);

    OptionSet optionSet = request.getOptions();
    if(optionSet != null){
      MaxAge maxAge = (MaxAge) optionSet.getOption(MAX_AGE);
      if(maxAge != null){
        messageBuilder.setMessageExpiryInterval(maxAge.getValue(), TimeUnit.SECONDS);
      }
    }

    if(request.getToken() != null && request.getToken().length > 0){
      messageBuilder.setCorrelationData(request.getToken());
    }

    HashMap<String, String> meta = new LinkedHashMap<>();
    meta.put("protocol", "CoAP");
    meta.put("version", "1");
    meta.put("time_ms", "" + System.currentTimeMillis());
    messageBuilder.setMeta(meta);

    Map<String, TypedData> map = new LinkedHashMap<>();
    ETag eTags = (ETag) request.getOptions().getOption(Constants.ETAG);
    if(eTags != null){
      List<byte[]> tagList =eTags.getList();
      for(int x=0;x<tagList.size();x++){
        map.put("etag_"+x, new TypedData(tagList.get(x)));
      }
    }
    messageBuilder.setDataMap(map);

    return messageBuilder.build();
  }
}
