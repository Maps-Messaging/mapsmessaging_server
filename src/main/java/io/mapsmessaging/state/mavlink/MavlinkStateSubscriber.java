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

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.mavlink.context.FrameFailureReason;
import io.mapsmessaging.mavlink.message.Frame;
import io.mapsmessaging.mavlink.message.Version;
import io.mapsmessaging.network.io.impl.noop.NoOpEndPoint;
import io.mapsmessaging.network.protocol.impl.mavlink.GsonFactory;
import io.mapsmessaging.state.MessageHandler;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.mavlink.listener.ListenerManager;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacketFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class MavlinkStateSubscriber implements MessageHandler {

  private final Logger logger = LoggerFactory.getLogger(MavlinkStateSubscriber.class);

  private final StateLoopProtocol protocol;
  private final String namespaceTopicPath;
  private final MavlinkSourceRegistry sourceRegistry;
  private final DroneInfoRegistry droneRegistry;
  private final MavlinkTwinUpdater twinUpdater;

  public MavlinkStateSubscriber(
      @NonNull @NotNull TwinManager twinManager,
      @NonNull @NotNull MavlinkTwinConfigDTO mavlinkConfig,
      @NonNull @NotNull DroneInfoRegistry registry
  ) throws IOException {
    this.protocol = new StateLoopProtocol(new NoOpEndPoint(1, null, new ArrayList<>()), this);
    this.namespaceTopicPath = mavlinkConfig.getTopic();
    this.sourceRegistry = new MavlinkSourceRegistry(mavlinkConfig);
    this.droneRegistry = registry;
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
    ProcessedFrame env = parseJson(message.getOpaqueData());
    if(env == null){
      return;
    }
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
    twinUpdater.updateTwinState(env, packet, context, knownSource, droneRegistry.getDroneInfo(knownSource.getName()));
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

  private ProcessedFrame parseJson(byte[] opaqueData) {
    JsonObject jsonObject = JsonParser.parseString(new String(opaqueData, StandardCharsets.UTF_8)).getAsJsonObject();
    JsonObject mavlinkObject = jsonObject.getAsJsonObject("mavlink");
    if(mavlinkObject == null) {
      return null;
    }
    JsonObject payloadObject = mavlinkObject.getAsJsonObject("payload");

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
      fields = GsonFactory.createStrictJsonWithSafeFloats().fromJson(
          payloadObject.getAsJsonObject("decoded"),
          new TypeToken<LinkedHashMap<String, Object>>() {
          }.getType()
      );
    }

    ProcessedFrame processedFrame = new ProcessedFrame(""+frame.getMessageId(), frame, fields, true, Collections.emptyList());
    return processedFrame;
  }
}