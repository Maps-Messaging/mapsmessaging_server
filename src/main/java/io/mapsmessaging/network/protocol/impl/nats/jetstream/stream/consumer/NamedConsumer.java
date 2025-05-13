package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.tasks.MessageResponse;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ConsumerConfig;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;
import lombok.Getter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Getter
public class NamedConsumer {

  private final ConsumerConfig config;
  private final String name;
  private final String streamName;
  private final Instant created;
  private final List<StreamSubscriptionInfo> streams;
  private List<Event> events;
  private int index;

  public NamedConsumer(String name, String streamName, ConsumerConfig config, List<StreamSubscriptionInfo> streams) {
    this.name = name;
    this.streamName = streamName;
    this.config = config;
    this.streams = streams;
    created = Instant.now();
    events = new ArrayList<>();
    index = 0;
  }

  public synchronized void receive(Message event, String destinationName,  Runnable callback) {
    events.add(new Event(event, destinationName, callback));
  }

  public synchronized Event poll() {
    return events.remove(0);
  }

  public synchronized int size() {
    return events.size();
  }

  public synchronized boolean isEmpty() {
    return events.isEmpty();
  }

  public synchronized MessageEvent getNextMessage() throws IOException {
    for(int x=0;x<streams.size();x++) {
      Future<Response> response = streams.get(index).getSubscribedEventManager().getNext();
      try {
        Response  val = response.get(100, TimeUnit.MILLISECONDS);
        if(val instanceof MessageResponse){
          MessageResponse messageResponse = (MessageResponse)val;
          MessageEvent msg = messageResponse.getResponse();
          if(msg != null){
            return msg;
          }
        }
      } catch (Exception e) {
        throw new IOException(e);
      }
      index = (index + 1) % streams.size();
    }
    return null;
  }

  public int getAckPendingCount() {
    return 0;
  }
}
