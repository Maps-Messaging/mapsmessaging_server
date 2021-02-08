package org.maps.network.protocol.impl.stomp.state;

import java.io.IOException;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.network.protocol.impl.stomp.StompProtocolException;
import org.maps.network.protocol.impl.stomp.frames.Connected;
import org.maps.network.protocol.impl.stomp.frames.Frame;
import org.maps.network.protocol.impl.stomp.listener.FrameListener;

public class InitialClientState implements State {

  public void handleFrame(StateEngine engine, Frame frame, boolean endOfBuffer) throws IOException {
    if(frame instanceof Connected){
      FrameListener listener = frame.getFrameListener();
      listener.frameEvent(frame, engine, endOfBuffer);
      listener.postFrameHandling(frame, engine);
    } else {
      throw new StompProtocolException("Invalid frame received");
    }
  }

  @Override
  public boolean sendMessage(StateEngine engine, Destination destination, String normalisedName, SubscriptionContext info, Message message, Runnable completionTask) {
    return false;
  }
}
