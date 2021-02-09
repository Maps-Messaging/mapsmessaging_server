package org.maps.network.protocol.impl.stomp.listener;

import java.io.IOException;
import java.util.Map;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.Transaction;
import org.maps.messaging.api.message.Message;
import org.maps.network.protocol.impl.stomp.frames.Error;
import org.maps.network.protocol.impl.stomp.frames.Event;
import org.maps.network.protocol.impl.stomp.state.StateEngine;

public class SendListener extends EventListener {

  protected void processEvent( StateEngine engine, Event event, Message message) throws IOException {
    Destination destination = engine.getSession().findDestination(engine.getMapping(event.getDestination()));
    if(destination != null) {
      if (event.getTransaction() != null) {
        Transaction transaction = engine.getSession().getTransaction(event.getTransaction());
        if (transaction == null) {
          Error error = new Error();
          error.setReceipt(event.getReceipt());
          error.setContent(("No known transaction found " + event.getTransaction()).getBytes());
          error.setContentType("text/plain");
          event.setReceipt(null);
        } else {
          transaction.add(destination, message);
        }
      } else {
        destination.storeMessage(message);
      }
    }
  }
}
