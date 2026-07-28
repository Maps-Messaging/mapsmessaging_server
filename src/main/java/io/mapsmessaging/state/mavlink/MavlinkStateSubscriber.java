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

package io.mapsmessaging.state.mavlink;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.mavlink.context.FrameFailureReason;
import io.mapsmessaging.mavlink.message.Frame;
import io.mapsmessaging.mavlink.message.Version;
import io.mapsmessaging.network.protocol.impl.mavlink.GsonFactory;
import io.mapsmessaging.state.MessageHandler;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.mavlink.listener.ListenerManager;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacketFactory;
import io.mapsmessaging.state.util.SessionHelper;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_CORRELATION_DATA_MISSING;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_DRONE_NOT_CONFIGURED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_EMPTY_MESSAGE_IGNORED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_JSON_PARSE_FAILED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_MAVLINK_OBJECT_MISSING;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_PAYLOAD_OBJECT_MISSING;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_PROCESSING_FAILED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_SOURCE_NOT_CONFIGURED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_SUBSCRIBER_START_FAILED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_SUBSCRIBER_STARTED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_SUBSCRIBER_STARTING;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_SUBSCRIBER_STOP_FAILED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_SUBSCRIBER_STOPPED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_SUBSCRIBER_STOPPING;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_TWIN_UPDATE_FAILED;
import static io.mapsmessaging.state.logging.StateLogMessages.MAVLINK_STATE_UNSUPPORTED_PACKET_IGNORED;

public class MavlinkStateSubscriber implements MessageHandler, AutoCloseable {

  private final Logger logger = LoggerFactory.getLogger(MavlinkStateSubscriber.class);

  private final StateLoopProtocol protocol;
  private final String namespaceTopicPath;
  private final MavlinkSourceRegistry sourceRegistry;
  private final DroneInfoRegistry droneRegistry;
  private final MavlinkTwinUpdater twinUpdater;

  private volatile boolean started;
  private volatile boolean closed;

  public MavlinkStateSubscriber(@NonNull @NotNull TwinManager twinManager, @NonNull @NotNull MavlinkTwinConfigDTO mavlinkConfig, @NonNull @NotNull DroneInfoRegistry registry) {
    this.protocol = SessionHelper.createLoopbackProtocol(this);
    this.namespaceTopicPath = mavlinkConfig.getTopic();
    this.sourceRegistry = new MavlinkSourceRegistry(mavlinkConfig);
    this.droneRegistry = registry;
    this.twinUpdater = new MavlinkTwinUpdater(twinManager, new ListenerManager(twinManager));
  }

  MavlinkStateSubscriber(
      StateLoopProtocol protocol,
      String namespaceTopicPath,
      MavlinkSourceRegistry sourceRegistry,
      DroneInfoRegistry droneRegistry,
      MavlinkTwinUpdater twinUpdater
  ) {
    this.protocol = Objects.requireNonNull(protocol, "protocol must not be null");
    this.namespaceTopicPath = Objects.requireNonNull(namespaceTopicPath, "namespaceTopicPath must not be null");
    this.sourceRegistry = Objects.requireNonNull(sourceRegistry, "sourceRegistry must not be null");
    this.droneRegistry = Objects.requireNonNull(droneRegistry, "droneRegistry must not be null");
    this.twinUpdater = Objects.requireNonNull(twinUpdater, "twinUpdater must not be null");
  }

  public synchronized void start() throws IOException {
    if (closed) {
      throw new IllegalStateException("MAVLink state subscriber is closed");
    }

    if (started) {
      return;
    }

    logger.log(MAVLINK_STATE_SUBSCRIBER_STARTING, namespaceTopicPath);

    try {
      protocol.connect(UUID.randomUUID().toString(), "anonymous", "anonymous");
      protocol.subscribeLocal(namespaceTopicPath, namespaceTopicPath, QualityOfService.AT_MOST_ONCE, null, null, null, null, null);
      started = true;
      logger.log(MAVLINK_STATE_SUBSCRIBER_STARTED, namespaceTopicPath);
    } catch (IOException exception) {
      try {
        protocol.close();
      } catch (IOException closeException) {
        exception.addSuppressed(closeException);
      }
      logger.log(MAVLINK_STATE_SUBSCRIBER_START_FAILED, exception, namespaceTopicPath);
      throw exception;
    }
  }

  public synchronized void stop() throws IOException {
    if (closed) {
      return;
    }

    closed = true;
    logger.log(MAVLINK_STATE_SUBSCRIBER_STOPPING, namespaceTopicPath);

    IOException failure = null;
    if (started) {
      try {
        protocol.unsubscribeLocal(namespaceTopicPath);
      } catch (IOException exception) {
        failure = exception;
      }

      try {
        protocol.close();
      } catch (IOException exception) {
        if (failure == null) {
          failure = exception;
        } else {
          failure.addSuppressed(exception);
        }
      }
    }

    started = false;

    try {
      twinUpdater.close();
    } catch (RuntimeException exception) {
      if (failure == null) {
        throw exception;
      }
      failure.addSuppressed(exception);
    }

    if (failure != null) {
      logger.log(MAVLINK_STATE_SUBSCRIBER_STOP_FAILED, failure, namespaceTopicPath);
      throw failure;
    }

    logger.log(MAVLINK_STATE_SUBSCRIBER_STOPPED, namespaceTopicPath);
  }

  @Override
  public void close() throws IOException {
    stop();
  }

  @Override
  public void handle(@NonNull @NotNull MessageEvent messageEvent) {
    if (closed) {
      messageEvent.getCompletionTask().run();
      return;
    }

    String sourceName = messageEvent.getDestinationName();
    Integer messageId = null;
    String droneName = null;
    boolean updatingTwin = false;

    try {
      Message message = messageEvent.getMessage();
      ProcessedFrame env = parseJson(message.getOpaqueData(), sourceName);

      if (env == null) {
        return;
      }

      Frame frame = env.getFrame();
      messageId = frame.getMessageId();

      MavlinkPacket packet = MavlinkPacketFactory.create(env);
      if (packet == null) {
        logger.log(MAVLINK_STATE_UNSUPPORTED_PACKET_IGNORED, messageId, sourceName);
        return;
      }

      MavlinkKnownSourceDTO knownSource = sourceRegistry.getKnownSource(env);
      if (knownSource == null) {
        logger.log(MAVLINK_STATE_SOURCE_NOT_CONFIGURED, messageId, frame.getSystemId(), frame.getComponentId());
        return;
      }

      droneName = knownSource.getName();
      DroneInfoDTO droneInfo = droneRegistry.getDroneInfo(droneName);

      if (droneInfo == null) {
        logger.log(MAVLINK_STATE_DRONE_NOT_CONFIGURED, messageId, sourceName, droneName);
        return;
      }

      TwinUpdateContext context = buildUpdateContext(env, message.getResponseTopic());
      byte[] correlationData = message.getCorrelationData();

      if (correlationData == null || correlationData.length == 0) {
        logger.log(MAVLINK_STATE_CORRELATION_DATA_MISSING, messageId, sourceName);
      } else {
        context.setUniqueOutboundIdentifier(new String(correlationData, StandardCharsets.UTF_8));
      }

      updatingTwin = true;
      twinUpdater.updateTwinState(env, packet, context, knownSource, droneInfo);
    } catch (RuntimeException exception) {
      if (updatingTwin) {
        logger.log(MAVLINK_STATE_TWIN_UPDATE_FAILED, exception, droneName, messageId, sourceName);
      } else {
        logger.log(MAVLINK_STATE_PROCESSING_FAILED, exception, sourceName);
      }

      throw exception;
    } finally {
      messageEvent.getCompletionTask().run();
    }
  }

  private TwinUpdateContext buildUpdateContext(ProcessedFrame env, String responseTopic) {
    TwinUpdateContext context = new TwinUpdateContext();
    context.setUpdateSource("mavlink");
    context.setSourceInstanceId("mavlink:" + env.getFrame().getSystemId() + ":" + env.getFrame().getComponentId());
    context.setReceivedTime(Instant.now());
    context.setSequenceNumber((long) env.getFrame().getSequence());
    context.setReason(env.getMessageName());
    context.setFullSnapshot(false);
    context.setResponseTopic(responseTopic);
    return context;
  }

  private ProcessedFrame parseJson(byte[] opaqueData, String sourceName) {
    if (opaqueData == null || opaqueData.length == 0) {
      logger.log(MAVLINK_STATE_EMPTY_MESSAGE_IGNORED, sourceName);
      return null;
    }

    try {
      JsonObject jsonObject = JsonParser.parseString(new String(opaqueData, StandardCharsets.UTF_8)).getAsJsonObject();
      JsonObject mavlinkObject = jsonObject.getAsJsonObject("mavlink");

      if (mavlinkObject == null) {
        logger.log(MAVLINK_STATE_MAVLINK_OBJECT_MISSING, sourceName);
        return null;
      }

      Object messageId = mavlinkObject.has("messageId") && !mavlinkObject.get("messageId").isJsonNull() ? mavlinkObject.get("messageId").getAsInt() : "unknown";
      JsonObject payloadObject = mavlinkObject.getAsJsonObject("payload");

      if (payloadObject == null) {
        logger.log(MAVLINK_STATE_PAYLOAD_OBJECT_MISSING, messageId, sourceName);
        return null;
      }

      Frame frame = new Frame();
      frame.setVersion(Version.valueOf(mavlinkObject.get("version").getAsString()));
      frame.setMessageId(mavlinkObject.get("messageId").getAsInt());
      frame.setSystemId(mavlinkObject.get("systemId").getAsInt());
      frame.setComponentId(mavlinkObject.get("componentId").getAsInt());
      frame.setSequence(mavlinkObject.get("sequence").getAsInt());
      frame.setPayloadLength(mavlinkObject.get("payloadLength").getAsInt());
      frame.setSigned(mavlinkObject.get("signed").getAsBoolean());
      frame.setValidated(FrameFailureReason.OK);
      frame.setChecksum(0);
      frame.setIncompatibilityFlags((byte) 0);
      frame.setCompatibilityFlags((byte) 0);
      frame.setSignature(null);

      Map<String, Object> fields = new LinkedHashMap<>();

      if (payloadObject.has("decoded") && payloadObject.get("decoded").isJsonObject()) {
        fields = GsonFactory.createStrictJsonWithSafeFloats().fromJson(payloadObject.getAsJsonObject("decoded"), new TypeToken<LinkedHashMap<String, Object>>() {}.getType());
      }

      return new ProcessedFrame(Integer.toString(frame.getMessageId()), frame, fields, true, Collections.emptyList(), null);
    } catch (RuntimeException exception) {
      logger.log(MAVLINK_STATE_JSON_PARSE_FAILED, exception, sourceName);
      return null;
    }
  }
}
