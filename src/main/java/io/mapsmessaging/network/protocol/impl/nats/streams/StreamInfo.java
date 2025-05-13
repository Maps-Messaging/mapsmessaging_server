package io.mapsmessaging.network.protocol.impl.nats.streams;

import io.mapsmessaging.engine.destination.DestinationImpl;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StreamInfo {
  private String subject;
  private DestinationImpl destination;
}
