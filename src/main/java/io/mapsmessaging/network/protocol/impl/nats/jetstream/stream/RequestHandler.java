package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public abstract class RequestHandler extends BaseStreamApiHandler {

  public abstract String getType();

  public abstract NatsFrame process(String subject, PayloadFrame frame, SessionState sessionState) throws IOException;

}
