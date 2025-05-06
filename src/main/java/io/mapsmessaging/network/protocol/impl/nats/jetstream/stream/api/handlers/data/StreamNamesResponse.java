package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.data;


import lombok.Data;

import java.util.List;

@Data
public class StreamNamesResponse {
  private String type = "io.nats.jetstream.api.v1.stream_names_response";
  private List<String> streams;
  private int total;
  private int offset;
  private int limit = 1024;
}
