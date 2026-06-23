package io.mapsmessaging.state.stanag.messages.node.status;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.stanag.messages.core.MessageHeader;
import io.mapsmessaging.state.stanag.messages.core.MessageHeaderBuilder;
import io.mapsmessaging.state.stanag.messages.core.MessageType;
import io.mapsmessaging.state.stanag.messages.node.common.NodeMessageSupport;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public class NodeStatusBuilder {

  private final MessageHeaderBuilder messageHeaderBuilder;

  private final NodeMessageSupport nodeMessageSupport;

  public NodeStatus build(DroneTwin droneTwin) {
    Objects.requireNonNull(droneTwin, "droneTwin cannot be null");
    Objects.requireNonNull(droneTwin.getUuid(), "droneTwin uuid cannot be null");

    MessageHeader header = messageHeaderBuilder.build(
        MessageType.NODE_STATUS,
        droneTwin.getUuid(),
        droneTwin.getLastSeenAt());

    NodeStatusBody body = NodeStatusBody.builder()
        .identifier(droneTwin.getUuid())
        .description(nodeMessageSupport.buildDescription(droneTwin))
        .timestamp(droneTwin.getOperationalUpdatedAt())
        .timeOfValidity(droneTwin.getValidTill())
        .pose(nodeMessageSupport.buildPose(droneTwin))
        .velocity(nodeMessageSupport.buildVelocity(droneTwin))
        .timeOfInitiation(droneTwin.getCreatedAt())
        .build();

    return new NodeStatus(header, body);
  }
}