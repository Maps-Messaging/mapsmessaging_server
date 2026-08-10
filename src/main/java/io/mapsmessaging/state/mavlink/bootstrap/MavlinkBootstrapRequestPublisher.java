/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.state.mavlink.bootstrap;

import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_BOOTSTRAP_REQUEST_FAILED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_BOOTSTRAP_REQUEST_SENT;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_BOOTSTRAP_REQUEST_SKIPPED;
import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.AUTOPILOT_VERSION;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLong;
import io.mapsmessaging.state.mavlink.messages.MavlinkCommandLongFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class MavlinkBootstrapRequestPublisher implements MavlinkBootstrapEventPublisher {

  private final Logger logger = LoggerFactory.getLogger(MavlinkBootstrapRequestPublisher.class);
  private final TwinManager twinManager;
  private final StateLoopProtocol protocol;

  public MavlinkBootstrapRequestPublisher(TwinManager twinManager, StateLoopProtocol protocol) {
    this.twinManager = twinManager;
    this.protocol = protocol;
  }

  @Override
  public void publish(MavlinkBootstrapEvent event) {
    if (!isAutopilotVersionRequest(event)) {
      return;
    }

    Optional<EntityTwin> optionalTwin = twinManager.getTwin(event.getTwinId());
    if (optionalTwin.isEmpty()) {
      logger.log(MAVLINK_BOOTSTRAP_REQUEST_SKIPPED, event.getTwinId(), "twin is not registered");
      return;
    }

    EntityTwin twin = optionalTwin.get();
    String responseTopic = twin.getResponseTopicName();
    String correlationData = twin.getUniqueOutboundIdentifier();
    if (responseTopic == null || responseTopic.isBlank() || correlationData == null || correlationData.isBlank()) {
      logger.log(MAVLINK_BOOTSTRAP_REQUEST_SKIPPED, event.getTwinId(), "MAVLink response route is unavailable");
      return;
    }

    MavlinkCommandLong request = MavlinkCommandLongFactory.requestMessage(event.getTargetSystem(), event.getTargetComponent(), 0, AUTOPILOT_VERSION);
    Message message = new MessageBuilder()
        .setOpaqueData(request.toMavlinkJsonObject().toString().getBytes(StandardCharsets.UTF_8))
        .setContentType("application/json")
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setCorrelationData(correlationData)
        .build();

    if (protocol.getSession() == null) {
      logger.log(MAVLINK_BOOTSTRAP_REQUEST_SKIPPED, event.getTwinId(), "MAVLink state session is unavailable");
      return;
    }

    protocol.getSession().findDestination(responseTopic, DestinationType.TOPIC).whenComplete((destination, failure) -> publishToDestination(event, message, destination, failure));
  }

  private boolean isAutopilotVersionRequest(MavlinkBootstrapEvent event) {
    return event != null
        && event.getEventType() == MavlinkBootstrapEventType.REQUEST
        && event.getRequestType() == MavlinkBootstrapRequestType.REQUEST_MESSAGE
        && event.getMavlinkMessageId() == AUTOPILOT_VERSION;
  }

  private void publishToDestination(MavlinkBootstrapEvent event, Message message, Destination destination, Throwable failure) {
    if (failure != null || destination == null) {
      logger.log(MAVLINK_BOOTSTRAP_REQUEST_FAILED, failure, event.getMavlinkMessageId(), event.getTwinId());
      return;
    }

    try {
      destination.storeMessage(message);
      logger.log(MAVLINK_BOOTSTRAP_REQUEST_SENT, event.getMavlinkMessageId(), event.getTargetSystem(), event.getTargetComponent(), event.getTwinId());
    } catch (IOException exception) {
      logger.log(MAVLINK_BOOTSTRAP_REQUEST_FAILED, exception, event.getMavlinkMessageId(), event.getTwinId());
    }
  }
}
