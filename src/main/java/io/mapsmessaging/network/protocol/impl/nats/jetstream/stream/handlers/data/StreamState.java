package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.handlers.data;

import lombok.Data;

@Data
public class StreamState {
  private long messages;
  private long bytes;
  private long first_seq;
  private long last_seq;
  private int consumer_count;
}