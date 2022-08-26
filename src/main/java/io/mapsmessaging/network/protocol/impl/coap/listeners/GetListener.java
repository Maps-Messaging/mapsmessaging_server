package io.mapsmessaging.network.protocol.impl.coap.listeners;

import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.BLOCK2;
import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.OBSERVE;
import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.URI_PATH;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.BlockWiseSend;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.Empty;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Block;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.ContentFormat;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Format;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Observe;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Option;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.UriPath;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.Context;
import java.io.IOException;
import org.jetbrains.annotations.Nullable;

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
