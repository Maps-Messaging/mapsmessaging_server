package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer;


import io.mapsmessaging.api.message.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Event{
  private final Message message;
  private final String destinationName;
  private final Runnable completionTask;
}