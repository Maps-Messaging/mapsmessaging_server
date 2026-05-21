/*
 *
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

package io.mapsmessaging.state.mavlink;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.dto.rest.config.twin.MavlinkTwinConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.mavlink.MavlinkEventFactory;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.network.io.impl.noop.NoOpEndPoint;
import io.mapsmessaging.network.protocol.impl.mavlink.MavlinkInterfaceManager;
import io.mapsmessaging.schemas.config.impl.MavlinkSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.state.MessageHandler;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.mavlink.listener.ListenerManager;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacketFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_STATE_DIALECT_DEFAULTED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_STATE_PACKET_UNPACK_EMPTY;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_STATE_PACKET_UNPACK_FAILED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_STATE_UNSUPPORTED_PACKET_IGNORED;

public class MavlinkStateSubscriber implements MessageHandler {

  private static final String DEFAULT_DIALECT_NAME = "common";

  private final Logger logger = LoggerFactory.getLogger(MavlinkStateSubscriber.class);

  private final MavlinkStateSubscriptionMode subscriptionMode;
  private final MavlinkEventFactory mavlinkEventFactory;
  private final StateLoopProtocol protocol;
  private final String namespaceTopicPath;
  private final MavlinkPayloadResolver payloadResolver;
  private final MavlinkSourceRegistry sourceRegistry;
  private final MavlinkTwinUpdater twinUpdater;

  public MavlinkStateSubscriber(
      @NonNull @NotNull TwinManager twinManager,
      @NonNull @NotNull MavlinkTwinConfigDTO mavlinkConfig
  ) throws IOException {
    String formatterDialectName = resolveFormatterDialectName(mavlinkConfig.getDialectName());

    this.subscriptionMode = mavlinkConfig.getSubscriptionMode();
    this.protocol = new StateLoopProtocol(new NoOpEndPoint(1, null, new ArrayList<>()), this);
    this.namespaceTopicPath = mavlinkConfig.getTopic();
    this.mavlinkEventFactory = MavlinkInterfaceManager.loadDialect(mavlinkConfig.getDialectName());
    this.sourceRegistry = new MavlinkSourceRegistry(mavlinkConfig);
    this.payloadResolver = new MavlinkPayloadResolver(createFormatter(formatterDialectName));
    this.twinUpdater = new MavlinkTwinUpdater(twinManager, new ListenerManager(twinManager));
  }

  public void start() throws IOException {
    protocol.connect(UUID.randomUUID().toString(), "anonymous", "anonymous");
    protocol.subscribeLocal(namespaceTopicPath, namespaceTopicPath, QualityOfService.AT_MOST_ONCE, null, null, null, null, null);
  }

  public void stop() throws IOException {
    protocol.unsubscribeLocal(namespaceTopicPath);
    protocol.close();
  }

  public void handle(@NonNull @NotNull MessageEvent messageEvent) {
    Message message = messageEvent.getMessage();
    String sourceName = messageEvent.getDestinationName();
    byte[] payload = payloadResolver.resolve(sourceName, message.getOpaqueData());

    if (payload == null) {
      return;
    }

    Optional<ProcessedFrame> potentialFrame;
    try {
      ByteBuffer mavlink = ByteBuffer.wrap(payload);
      potentialFrame = mavlinkEventFactory.unpack(sourceName, mavlink);
    } catch (IOException exception) {
      logger.log(MAVLINK_STATE_PACKET_UNPACK_FAILED, sourceName);
      return;
    }

    if (potentialFrame.isEmpty()) {
      logger.log(MAVLINK_STATE_PACKET_UNPACK_EMPTY, sourceName);
      return;
    }

    ProcessedFrame env = potentialFrame.get();
    MavlinkPacket packet = MavlinkPacketFactory.create(env);

    if (packet == null) {
      logger.log(MAVLINK_STATE_UNSUPPORTED_PACKET_IGNORED, env.getFrame().getMessageId(), sourceName);
      return;
    }

    MavlinkKnownSourceDTO knownSource = sourceRegistry.getKnownSource(env);

    if (knownSource == null) {
      return;
    }

    TwinUpdateContext context = buildUpdateContext(env);
    twinUpdater.updateTwinState(messageEvent, env, packet, context, knownSource, subscriptionMode);

    if (subscriptionMode.includesPlanState()) {
      // Later: planListenerManager.handle(env, packet, context);
    }
  }

  private MessageFormatter createFormatter(String dialectName) throws IOException {
    MavlinkSchemaConfig schemaConfig = new MavlinkSchemaConfig();
    schemaConfig.setDialect(dialectName);

    return MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
  }

  private String resolveFormatterDialectName(String dialectName) {
    if (dialectName == null || dialectName.isBlank()) {
      logger.log(MAVLINK_STATE_DIALECT_DEFAULTED, DEFAULT_DIALECT_NAME);
      return DEFAULT_DIALECT_NAME;
    }

    return dialectName;
  }

  private TwinUpdateContext buildUpdateContext(ProcessedFrame env) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setUpdateSource("mavlink");
    context.setSourceInstanceId("mavlink:" + env.getFrame().getSystemId() + ":" + env.getFrame().getComponentId());
    context.setReceivedTime(Instant.now());
    context.setSequenceNumber((long) env.getFrame().getSequence());
    context.setReason(env.getMessageName());
    context.setFullSnapshot(false);

    return context;
  }
}