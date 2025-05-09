package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data;

import lombok.Data;

import java.time.Instant;

@Data
public class ConsumerCreateResponse {
  private String type = "io.nats.jetstream.api.v1.consumer_create_response";
  private String stream_name;
  private String name;
  private Instant created;
  private ConsumerConfig config;
}
