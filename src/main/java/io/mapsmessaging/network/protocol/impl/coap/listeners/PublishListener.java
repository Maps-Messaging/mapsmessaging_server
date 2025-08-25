/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.coap.listeners;


import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.blockwise.BlockReceiveState;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.*;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.mapsmessaging.logging.ServerLogMessages.COAP_FAILED_TO_PROCESS;
import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.*;

public abstract class PublishListener extends  Listener {

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
      destination.storeMessage(build(request, protocol));
    }
  }

  protected Message build(BasePacket request, CoapProtocol protocol){
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
    meta.put("sessionId", protocol.getSessionId());
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
    return  MessageOverrides.createMessageBuilder( protocol.getProtocolConfig().getMessageDefaults(), messageBuilder).build();
  }

  private boolean handleBlock(BasePacket request, String path, OptionSet optionSet, CoapProtocol protocol) {
    Block block = (Block) optionSet.getOption(BLOCK1);
    BlockReceiveState blockReceiveState = protocol.getBlockReceiveMonitor().registerOrGet(block, path);
    blockReceiveState.getReceivePacket().add(block.getNumber(), request.getPayload());
    if (request.getType().equals(TYPE.CON)) {
      BasePacket response = request.buildAckResponse(Code.CONTINUE);
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
    if (block.isMore()) {
      return false;
    }
    protocol.getBlockReceiveMonitor().complete(path);
    request.setPayload(blockReceiveState.getReceivePacket().getFull());
    return true;
  }

  protected void publishMessage(BasePacket request, CoapProtocol protocol, boolean isDelete) {
    OptionSet optionSet = request.getOptions();
    String path = "/";
    if (optionSet.hasOption(URI_PATH)) {
      UriPath uriPath = (UriPath) optionSet.getOption(URI_PATH);
      path = uriPath.toString();
    }
    if (optionSet.hasOption(BLOCK1) && !handleBlock(request, path, optionSet, protocol)) {
      return;
    }

    String finalPath = path;

    try {
      boolean exists = protocol.getSession().destinationExists(path).get(); // Waits for the result
      Destination destination = protocol.getSession()
          .findDestination(finalPath, DestinationType.TOPIC)
          .get(); // Waits for the destination

      if (destination != null) {
        handleEvent(isDelete, exists, destination, request, protocol);
      }
    } catch (IOException e) {
      protocol.getLogger().log(COAP_FAILED_TO_PROCESS, request.getFromAddress(), e);
      try {
        protocol.close();
      } catch (IOException ioException) {
        // Ignore, we are in an error state
      }
    } catch (InterruptedException | ExecutionException e) {
      // Handle InterruptedException or ExecutionException
      Thread.currentThread().interrupt(); // Restore interrupted state
      protocol.getLogger().log(COAP_FAILED_TO_PROCESS, request.getFromAddress(), e);
    }
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
