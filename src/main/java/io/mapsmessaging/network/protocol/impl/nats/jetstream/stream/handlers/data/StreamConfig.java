package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.handlers.data;

import lombok.Data;

import java.util.List;

@Data
public class StreamConfig {
  private String name;
  private List<String> subjects;
  private String storage;
}