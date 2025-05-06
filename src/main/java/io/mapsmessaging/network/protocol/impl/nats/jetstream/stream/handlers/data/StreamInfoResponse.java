package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.handlers.data;
import lombok.Data;

import java.time.Instant;

@Data
public class StreamInfoResponse {
  private String type = "io.nats.jetstream.api.v1.stream_info_response";
  private StreamConfig config;
  private Instant created;
  private StreamState state;
}
