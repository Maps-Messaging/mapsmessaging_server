package io.mapsmessaging.network.protocol.impl.nats.streams;

import io.mapsmessaging.engine.destination.DestinationImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamInfo {
  private String subject;
  private DestinationImpl destination;
}
