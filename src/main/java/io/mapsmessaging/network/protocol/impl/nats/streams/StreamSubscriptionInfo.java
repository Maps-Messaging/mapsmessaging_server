package io.mapsmessaging.network.protocol.impl.nats.streams;

import io.mapsmessaging.api.SubscribedEventManager;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StreamSubscriptionInfo extends StreamInfo {
  private SubscribedEventManager subscribedEventManager;

  public StreamSubscriptionInfo(final SubscribedEventManager subscribedEventManager, StreamInfo streamInfo) {
    super(streamInfo.getSubject(), streamInfo.getDestination());
    this.subscribedEventManager = subscribedEventManager;
  }
}
