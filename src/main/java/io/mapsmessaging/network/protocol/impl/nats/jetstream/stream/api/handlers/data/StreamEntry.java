package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.data;

import lombok.Data;

import java.time.Instant;

@Data
public class StreamEntry {
  private StreamConfig config;
  private Instant created;
}
