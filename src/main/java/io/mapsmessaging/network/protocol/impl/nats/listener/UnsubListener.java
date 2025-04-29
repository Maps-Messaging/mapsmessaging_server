package io.mapsmessaging.network.protocol.impl.nats.listener;

import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.OkFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.UnsubFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;


import java.io.IOException;

public class UnsubListener implements FrameListener {

  @Override
  public void frameEvent(NatsFrame frame, SessionState engine, boolean endOfBuffer) throws IOException {
    UnsubFrame unsubscribe = (UnsubFrame) frame;
    engine.removeSubscription(unsubscribe.getSubscriptionId());
    if(engine.isVerbose()){
      engine.send(new OkFrame());
    }
  }
}
