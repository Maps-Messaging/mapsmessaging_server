package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data;
import lombok.Data;

@Data
public class ConsumerConfig {
  private String deliverSubject;
  private String filterSubject;
  private String durableName;
  private AckPolicy ackPolicy;
  private DeliverPolicy deliverPolicy;
  private ReplayPolicy replayPolicy;
  private Boolean flowControl;
  private Long idleHeartbeat;
  private Integer maxAckPending;
}
