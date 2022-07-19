package io.mapsmessaging.network.protocol.impl.coap.listeners;

import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.OBSERVE;
import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.URI_PATH;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.ContentFormat;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Format;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Observe;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Option;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.UriPath;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.Context;
import java.util.concurrent.ExecutionException;

public class GetListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) throws ExecutionException, InterruptedException {
    BasePacket response;
    OptionSet optionSet = request.getOptions();
    String path = "/";
    UriPath uriPath = (UriPath) optionSet.getOption(URI_PATH);
    if(uriPath != null){
      path = uriPath.toString();
    }
    if(path.equals(".well-known/core")){
      return sendWellKnown(request);
    }
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
        protocol.sendResponse(response);
        response = null;
        SubscriptionContext context = new SubscriptionContext(path);
        context.setReceiveMaximum(1);
        context.setBrowserFlag(true);
        Context subscriptionContext = protocol.getSubscriptionState().create(path, request);
        subscriptionContext.setSubscribedEventManager(session.addSubscription(context));
      } else {
        response = request.buildAckResponse(Code.CONTENT);
        response.setCode(Code.NOT_FOUND);
      }
    }
    catch(Exception exception){
      exception.printStackTrace();
      response = request.buildAckResponse(Code.INTERNAL_SERVER_ERROR);
    }
    if (request.getType().equals(TYPE.NON) && response != null) {
      request.setType(TYPE.NON);
    }
    return response;
  }

  private BasePacket sendWellKnown(BasePacket getRequest) {
    String linkContent = SchemaManager.getInstance().buildLinkFormatResponse();
    BasePacket response = getRequest.buildAckResponse(Code.CONTENT);
    response.setPayload(linkContent.getBytes());
    ContentFormat format = new ContentFormat(Format.LINK_FORMAT);
    response.getOptions().putOption(format);
    return response;
  }
}
