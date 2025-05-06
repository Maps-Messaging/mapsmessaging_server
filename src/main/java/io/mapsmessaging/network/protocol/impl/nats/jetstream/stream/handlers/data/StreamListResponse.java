package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.handlers.data;

import lombok.Data;

import java.util.List;

@Data
public class StreamListResponse {
  private String type = "io.nats.jetstream.api.v1.stream_list_response";
  private List<StreamEntry> streams;
  private int total;
  private int offset;
  private int limit;
}
