package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
public class ConsumerCreateResponse {
  private String type = "io.nats.jetstream.api.v1.consumer_create_response";
  private String stream_name;
  private String name;
  private Instant created;
  private ConsumerConfig config;
  private DeliveryInfo delivered;
  private AckFloor ack_floor;
  private long num_ack_pending;
  private long num_redelivered;
  private long num_waiting;
  private long num_pending;
  private Instant ts;

  @Data
  @AllArgsConstructor
  public static class DeliveryInfo {
    private long consumer_seq;
    private long stream_seq;
  }

  @Data
  @AllArgsConstructor
  public static class AckFloor {
    private long consumer_seq;
    private long stream_seq;
  }
}
