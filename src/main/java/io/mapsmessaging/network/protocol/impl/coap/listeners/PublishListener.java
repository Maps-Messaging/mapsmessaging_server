package io.mapsmessaging.network.protocol.impl.coap.listeners;


import static io.mapsmessaging.logging.ServerLogMessages.COAP_FAILED_TO_PROCESS;
import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.BLOCK1;
import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.ETAG;
import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.MAX_AGE;
import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.URI_PATH;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.blockwise.ReceivePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Block;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.ETag;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.IfMatch;
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
import java.util.concurrent.TimeUnit;

public abstract class PublishListener extends  Listener {


  private final Map<String, ReceivePacket> blockBasedPackets = new LinkedHashMap<>();

  private void handleEvent(boolean isDelete, Boolean exists, Destination destination, BasePacket request, CoapProtocol protocol) throws IOException {
    Code code = Boolean.TRUE.equals(exists) ? Code.CHANGED : Code.CREATED;
    if (isDelete) {
      code = Code.DELETED;
    }
    boolean process = canProcess(destination, request);
    if (request.getType().equals(TYPE.CON)) {
      BasePacket response = request.buildAckResponse(code);
      response.setCode(process ? code : Code.PRECONDITION_FAILED);
      protocol.sendResponse(response);
    }
    if (isDelete || process) {
      destination.storeMessage(build(request));
    }
  }


  protected Message build(BasePacket request){
    MessageBuilder messageBuilder = new MessageBuilder();

    messageBuilder.setOpaqueData(request.getPayload());
    messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
    messageBuilder.setRetain(true);

    OptionSet optionSet = request.getOptions();
    if (optionSet != null && optionSet.hasOption(MAX_AGE)) {
      MaxAge maxAge = (MaxAge) optionSet.getOption(MAX_AGE);
      messageBuilder.setMessageExpiryInterval(maxAge.getValue(), TimeUnit.SECONDS);
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
    if (request.getOptions().hasOption(ETAG)) {
      ETag eTags = (ETag) request.getOptions().getOption(Constants.ETAG);
      List<byte[]> tagList = eTags.getList();
      for (int x = 0; x < tagList.size(); x++) {
        map.put("etag_" + x, new TypedData(tagList.get(x)));
      }
    }
    messageBuilder.setDataMap(map);
    return messageBuilder.build();
  }

  private boolean handleBlock(BasePacket request, String path, OptionSet optionSet, CoapProtocol protocol){
    Block block = (Block) optionSet.getOption(BLOCK1);
    ReceivePacket receivePacket = blockBasedPackets.computeIfAbsent(path, k -> new ReceivePacket(block.getSizeEx()));
    receivePacket.add(block.getNumber(), request.getPayload());
    if (request.getType().equals(TYPE.CON)) {
      BasePacket response = request.buildAckResponse(Code.CHANGED);
      try {
        protocol.sendResponse(response);
      } catch (IOException e) {
        try {
          protocol.close();
        } catch (IOException ex) {
          // We are closing
        }
      }
    }
    if(block.isMore()){
      return false;
    }
    blockBasedPackets.remove(path);
    request.setPayload(receivePacket.getFull());
    return true;
  }

  protected void publishMessage(BasePacket request, CoapProtocol protocol, boolean isDelete) {
    OptionSet optionSet = request.getOptions();
    String path = "/";
    if (optionSet.hasOption(URI_PATH)) {
      UriPath uriPath = (UriPath) optionSet.getOption(URI_PATH);
      path = uriPath.toString();
    }
    if(optionSet.hasOption(BLOCK1) && !handleBlock(request, path, optionSet, protocol)){
      return;
    }

    String finalPath = path;
    protocol.getSession().destinationExists(path).thenApply(exists -> {
      protocol.getSession().findDestination(finalPath, DestinationType.TOPIC).thenApply(destination -> {
        if (destination != null) {
          try {
            handleEvent(isDelete, exists, destination, request, protocol);
          } catch (IOException e) {
            protocol.getLogger().log(COAP_FAILED_TO_PROCESS, request.getFromAddress(), e);
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
    if (optionSet.hasOption(Constants.IF_NONE_MATCH)) {
      return destination.getRetained() == null;
    }
    IfMatch ifMatch = (IfMatch) optionSet.getOption(Constants.IF_MATCH);
    if ((!ifMatch.getList().isEmpty())) {
      Message message = destination.getRetained();
      if (message != null) {
        List<byte[]> etags = extractTags(message);
        return compareTags(etags, ifMatch.getList());
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
}
