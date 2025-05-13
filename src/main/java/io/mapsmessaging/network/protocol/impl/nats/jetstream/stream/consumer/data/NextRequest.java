package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data;

import lombok.Data;

@Data
public class NextRequest {
  private int batch = 1;           // default to 1 if missing
  private boolean no_wait = false; // default false
  private Long expires;            // in nanoseconds, optional
}
