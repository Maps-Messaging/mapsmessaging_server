package io.mapsmessaging.network.protocol.impl.coap.listeners;

import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.schema.LinkFormat;
import io.mapsmessaging.engine.schema.LinkFormatManager;
import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.ContentFormat;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Format;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.UriPath;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.Context;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class GetListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) throws ExecutionException, InterruptedException {
    BasePacket response = null;
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
    List<LinkFormat> linkFormatList = MessageDaemon.getInstance().getDestinationManager().getWellKnown();
    String linkContent = LinkFormatManager.getInstance().buildLinkFormatString("", linkFormatList);
    BasePacket response = getRequest.buildAckResponse(Code.CONTENT);
    response.setPayload(linkContent.getBytes());
    ContentFormat format = new ContentFormat(Format.LINK_FORMAT);
    response.getOptions().putOption(format);
    return response;
  }
}
