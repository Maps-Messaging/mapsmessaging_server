package io.mapsmessaging.state.stanag.messages.node.description;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.stanag.messages.core.MessageHeader;
import io.mapsmessaging.state.stanag.messages.core.MessageHeaderBuilder;
import io.mapsmessaging.state.stanag.messages.core.MessageType;
import io.mapsmessaging.state.stanag.messages.node.common.NodeMessageSupport;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public class NodeDescriptionBuilder {

  private final MessageHeaderBuilder messageHeaderBuilder;

  private final NodeMessageSupport nodeMessageSupport;

  public NodeDescription build(DroneTwin droneTwin) {
    Objects.requireNonNull(droneTwin, "droneTwin cannot be null");
    Objects.requireNonNull(droneTwin.getUuid(), "droneTwin uuid cannot be null");

    MessageHeader header = messageHeaderBuilder.build(
        MessageType.NODE_DESCRIPTION,
        MessageDaemon.getInstance().getUuid(),
        droneTwin.getLastSeenAt());

    NodeDescriptionBody body = NodeDescriptionBody.builder()
        .identifier(droneTwin.getUuid())
        .description(nodeMessageSupport.buildDescription(droneTwin))
        .capabilities(droneTwin.getCapabilities())
        .timestamp(droneTwin.getOperationalUpdatedAt())
        .timeOfValidity(droneTwin.getValidTill())
        .pose(nodeMessageSupport.buildPose(droneTwin))
        .velocity(nodeMessageSupport.buildVelocity(droneTwin))
        .timeOfInitiation(droneTwin.getCreatedAt())
        .sourceOfInformation(MessageDaemon.getInstance().getUuid())
        .build();

    return new NodeDescription(header, body);
  }
}