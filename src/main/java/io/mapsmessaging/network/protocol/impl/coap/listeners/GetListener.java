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
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.*;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.*;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.Context;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.*;

public class GetListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) throws IOException {
    switch(request.getType()){
      case CON:
      case NON:
        return handleGetRequest(request, protocol);

      case ACK:
        if (request.getToken() != null) {
          protocol.ack(request);
        }
        break;

      case RST:
        protocol.close();
    }
    return null;
  }

  private BasePacket handleGetRequest(BasePacket request, CoapProtocol protocol){
    BasePacket response;
    OptionSet optionSet = request.getOptions();
    String path;
    UriPath uriPath = (UriPath) optionSet.getOption(URI_PATH);
    path = uriPath.toString();
    if(path.equals(".well-known/core")){
      response = sendWellKnown(request, protocol);
    }
    else{
      response = buildSubscription(path, request, protocol);
    }

    if (request.getType().equals(TYPE.NON) && response != null) {
      request.setType(TYPE.NON);
    }
    return response;
  }

  protected @Nullable String getSelector(BasePacket packet){
    return null;
  }

  private BasePacket buildSubscription(String path, BasePacket request, CoapProtocol protocol){
    BasePacket response;
    try {
      Session session = protocol.getSession();
      Destination destination = session.findDestination(path, DestinationType.TOPIC).get();
      if (destination != null) {
        response = request.buildWaitResponse();
        if (request.getType().equals(TYPE.NON)) {
          request.setType(TYPE.NON);
        }
        if(request.getOptions().hasOption(OBSERVE)) {
          Option observe = request.getOptions().getOption(OBSERVE);
          if (((Observe) observe).register()) {
            Observe option = new Observe(0);
            response.getOptions().putOption(option);
            response.setToken(request.getToken());
          }
        }
        if(request.getType().equals(TYPE.CON)) {
          protocol.sendResponse(response);
        }
        response = null;
        SubscriptionContext context = new SubscriptionContext(path);
        context.setReceiveMaximum(1);
        context.setMaxAtRest(1);
        String selector = getSelector(request);
        if(selector != null){
          context.setSelector(selector);
        }
        Context subscriptionContext = protocol.getSubscriptionState().create(path, request);
        subscriptionContext.setSubscribedEventManager(session.addSubscription(context));
      } else {
        response = request.buildAckResponse(Code.CONTENT);
        response.setCode(Code.NOT_FOUND);
      }
    }
    catch(Exception exception){
      response = request.buildAckResponse(Code.INTERNAL_SERVER_ERROR);
      Thread.currentThread().interrupt();
    }
    return response;
  }

  private BasePacket sendWellKnown(BasePacket getRequest, CoapProtocol protocol) {
    String linkContent = SchemaManager.getInstance().buildLinkFormatResponse();
    BasePacket response = getRequest.buildAckResponse(Code.CONTENT);
    response.setPayload(linkContent.getBytes());
    ContentFormat format = new ContentFormat(Format.LINK_FORMAT);
    response.getOptions().putOption(format);
    if (getRequest.getOptions().hasOption(BLOCK2)) {
      Block block = (Block) getRequest.getOptions().getOption(BLOCK2);
      int size = 1 << (block.getSizeEx() + 4);
      if (response.getPayload().length > size) {
        BasePacket ack = new Empty(getRequest.getMessageId());
        ack.setCode(Code.EMPTY);
        ack.setFromAddress(getRequest.getFromAddress());
        try {
          protocol.sendResponse(ack);
        } catch (IOException e) {
          // Ignore this
        }
        BlockWiseSend blockWiseSend = new BlockWiseSend(getRequest);
        blockWiseSend.getOptions().getOptionList().putAll(getRequest.getOptions().getOptionList());
        blockWiseSend.getOptions().add(format);
        blockWiseSend.setCode(Code.CONTENT);
        blockWiseSend.setBlockSize(block.getSizeEx());
        blockWiseSend.setPayload(response.getPayload());
        blockWiseSend.setFromAddress(getRequest.getFromAddress());
        response = blockWiseSend;
      }
    }
    return response;
  }
}
