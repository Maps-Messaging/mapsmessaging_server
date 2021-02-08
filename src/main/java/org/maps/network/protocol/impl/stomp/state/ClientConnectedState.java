package org.maps.network.protocol.impl.stomp.state;

import org.maps.messaging.api.Destination;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.network.protocol.impl.stomp.frames.Send;

public class ClientConnectedState extends ConnectedState {

  @Override
  public boolean sendMessage(StateEngine engine, Destination destination,String normalisedName,  SubscriptionContext context, Message message, Runnable completionTask) {
    Send msg = new Send(1024);
    msg.packMessage(destination.getName(), context.getAlias(), message);
    msg.setCallback(new MessageCompletionHandler(completionTask));
    return engine.send(msg);
  }
}
