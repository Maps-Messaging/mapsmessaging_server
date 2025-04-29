package io.mapsmessaging.network.protocol.impl.nats.listener;

import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.SubFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class SubListener implements FrameListener {

  @Override
  public void frameEvent(NatsFrame frame, SessionState engine, boolean endOfBuffer) throws IOException {
    ClientAcknowledgement ackManger;
    SubFrame subscribe = (SubFrame) frame;
    ackManger = ClientAcknowledgement.AUTO;
    String subject = subscribe.getSubject();
    String[] split = subject.split("&");
    String destination = convertSubject(split[0]);
    String selector = split.length > 1 ? split[1] : null;
    SubscriptionContextBuilder builder = new SubscriptionContextBuilder(destination, ackManger);
    builder.setAlias(subscribe.getSubscriptionId());
    builder.setReceiveMaximum(engine.getProtocol().getMaxReceiveSize());
    builder.setNoLocalMessages(!engine.isEchoEvents());
    if (selector != null) builder.setSelector(selector);
    if(subscribe.getShareName() != null ) builder.setSharedName(subscribe.getShareName());

    try {
      engine.createSubscription(builder.build());
    } catch (IOException ioe) {
      ErrFrame error = new ErrFrame();
      error.setError("Unable to find subject");
      engine.send(error);
    }
  }
}

