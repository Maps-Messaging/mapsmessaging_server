package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.transactions.handler;

import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.TransactionSubject;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;

import java.io.IOException;

public class TransactionHandler extends JetStreamFrameHandler {

  private final TransactionProcessor ackProcessor = new AckProcessor();
  private final TransactionProcessor nackProcessor = new NackProcessor();

  @Override
  public String getName() {
    return "$JS.ACK";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();

    String[] parts = subject.split("\\.");
    if (parts.length < 6) {
      return null;
    }
    TransactionSubject transactionSubject = TransactionSubject.parse(subject);
    StreamSubscriptionInfo info = lookupInfo(transactionSubject, sessionState);
    if(info != null) {
      String commandString = new String(frame.getPayload()).trim().toLowerCase();
      switch (commandString) {
        case "+ack":
        case "+term":
          ackProcessor.handle(info, transactionSubject);
          break;
        case "-nak":
          nackProcessor.handle(info, transactionSubject);
          break;

        case "+nxt":
        default:
          return null;
      }
    }
    return null;
  }


  private StreamSubscriptionInfo lookupInfo( TransactionSubject transactionSubject, SessionState sessionState ) {
    NamedConsumer consumer = sessionState.getNamedConsumers().get(transactionSubject.getConsumer());
    if(consumer != null && transactionSubject.getDestinationIndex() < consumer.getStreams().size()) {
      return consumer.getStreams().get(transactionSubject.getDestinationIndex());
    }
    return null;
  }
}
