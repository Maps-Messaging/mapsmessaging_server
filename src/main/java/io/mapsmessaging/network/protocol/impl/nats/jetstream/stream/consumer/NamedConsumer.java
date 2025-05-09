package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ConsumerConfig;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfo;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
public class NamedConsumer {

  private final ConsumerConfig config;
  private final String name;
  private final String streamName;
  private final Instant created;
  private final List<StreamInfo> streams;
  private List<Event> events;

  public NamedConsumer(String name, String streamName, ConsumerConfig config, List<StreamInfo> streams) {
    this.name = name;
    this.streamName = streamName;
    this.config = config;
    this.streams = streams;
    created = Instant.now();
    events = new ArrayList<>();
  }

  public synchronized void receive(Message event, Runnable callback) {
    events.add(new Event(event, callback));
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

  public int getAckPendingCount() {
    return 0;
  }
}
