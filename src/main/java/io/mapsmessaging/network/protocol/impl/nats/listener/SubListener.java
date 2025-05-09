package io.mapsmessaging.network.protocol.impl.nats.listener;

import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.OkFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.SubFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class SubListener implements FrameListener {

  @Override
  public void frameEvent(NatsFrame frame, SessionState engine, boolean endOfBuffer) throws IOException {

    SubFrame subscribe = (SubFrame) frame;
    if (subscribe.getSubject().startsWith("_INBOX.") && subscribe.getSubscriptionId() != null) {
      engine.getJetStreamRequestManager().setJetSubject(subscribe.getSubject());
      engine.getJetStreamRequestManager().setSubscriptionId(subscribe.getSubscriptionId());
      if (engine.isVerbose()) engine.send(new OkFrame());
      return;
    }
    engine.subscribe(subscribe.getSubject(), subscribe.getSubscriptionId(), subscribe.getShareName(), ClientAcknowledgement.AUTO, 0 );
  }
}

