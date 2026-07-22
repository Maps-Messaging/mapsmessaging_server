/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License.
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

package io.mapsmessaging.state.n2k;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.state.MessageHandler;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.DroneInfoRegistry;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.util.SessionHelper;
import io.mapsmessaging.utilities.Lifecycle;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static io.mapsmessaging.state.logging.StateLogMessages.*;

public class N2kSession implements MessageHandler, Lifecycle {

  private static final Logger logger = LoggerFactory.getLogger(N2kSession.class);

  private final StateLoopProtocol protocol;
  private final String namespaceTopicPath;
  private final N2KTwinConfig n2kConfig;
  private final N2kTwinUpdater twinUpdater;
  private final DroneInfoDTO droneInfo;

  public N2kSession(
      @NonNull @NotNull TwinManager twinManager,
      @NonNull @NotNull N2KTwinConfig n2kConfig,
      @NonNull @NotNull DroneInfoRegistry droneRegistry) {
    this.protocol = SessionHelper.createLoopbackProtocol(this);
    this.namespaceTopicPath = n2kConfig.getTopic();
    this.n2kConfig = n2kConfig;
    this.twinUpdater = new N2kTwinUpdater(twinManager);
    this.droneInfo = droneRegistry.getDroneInfo(n2kConfig.getName());

    if (droneInfo == null) {
      logger.log(N2K_DRONE_CONFIG_MISSING, n2kConfig.getName(), namespaceTopicPath);
    } else {
      logger.log(N2K_DRONE_CONFIG_RESOLVED, n2kConfig.getName(), namespaceTopicPath);
    }
  }

  @Override
  public void start() {
    if (droneInfo == null) {
      logger.log(N2K_SESSION_START_SKIPPED, n2kConfig.getName(), namespaceTopicPath);
      return;
    }

    logger.log(N2K_SESSION_STARTING, n2kConfig.getName(), namespaceTopicPath);

    try {
      protocol.connect(UUID.randomUUID().toString(), "anonymous", "anonymous");
      protocol.subscribeLocal(namespaceTopicPath, namespaceTopicPath, QualityOfService.AT_MOST_ONCE, null, null, null, null, null);
      logger.log(N2K_SESSION_STARTED, n2kConfig.getName(), namespaceTopicPath);
    } catch (IOException exception) {
      logger.log(N2K_SESSION_START_FAILED, exception);
    }
  }

  @Override
  public void stop() {
    if (droneInfo == null) {
      logger.log(N2K_SESSION_STOP_SKIPPED, n2kConfig.getName());
      return;
    }

    logger.log(N2K_SESSION_STOPPING, n2kConfig.getName(), namespaceTopicPath);

    try {
      protocol.unsubscribeLocal(namespaceTopicPath);
      protocol.close();
      logger.log(N2K_SESSION_STOPPED, n2kConfig.getName(), namespaceTopicPath);
    } catch (IOException exception) {
      logger.log(N2K_SESSION_STOP_FAILED, exception);
    }
  }

  @Override
  public void handle(@NonNull @NotNull MessageEvent messageEvent) {
    String sourceName = messageEvent.getDestinationName();

    try {
      logger.log(N2K_MESSAGE_RECEIVED, sourceName);

      if (droneInfo == null) {
        logger.log(N2K_MESSAGE_IGNORED_NO_DRONE, sourceName, n2kConfig.getName());
        return;
      }

      Message message = messageEvent.getMessage();
      byte[] opaqueData = message.getOpaqueData();

      if (opaqueData == null || opaqueData.length == 0) {
        logger.log(N2K_MESSAGE_IGNORED_EMPTY, sourceName);
        return;
      }

      if (opaqueData[0] != '{') {
        logger.log(N2K_MESSAGE_IGNORED_NOT_JSON, sourceName);
        return;
      }

      JsonObject root = JsonParser.parseString(new String(opaqueData, StandardCharsets.UTF_8)).getAsJsonObject();
      JsonObject j1939 = getJsonObject(root, "j1939");

      if (j1939 == null) {
        logger.log(N2K_MESSAGE_IGNORED_NO_J1939, sourceName);
        return;
      }

      Integer pgn = getInteger(j1939, "pgn");
      if (pgn == null) {
        logger.log(N2K_MESSAGE_IGNORED_NO_PGN, sourceName);
        return;
      }

      JsonObject n2k = getJsonObject(j1939, "n2k");
      if (n2k == null) {
        logger.log(N2K_MESSAGE_IGNORED_NO_N2K, sourceName);
        return;
      }

      JsonObject packet = getJsonObject(n2k, "packet");
      if (packet == null) {
        logger.log(N2K_MESSAGE_IGNORED_NO_PACKET, pgn, sourceName);
        return;
      }

      TwinUpdateContext context = createContext(sourceName, j1939, n2k, packet);

      logger.log(N2K_TWIN_UPDATE, pgn, sourceName, context.getReason());
      twinUpdater.updateTwinState(pgn, packet, context, n2kConfig, droneInfo);
      logger.log(N2K_TWIN_UPDATED, pgn, sourceName);
    } catch (RuntimeException exception) {
      logger.log(N2K_MESSAGE_PROCESSING_FAILED, exception);
      throw exception;
    } finally {
      messageEvent.getCompletionTask().run();
    }
  }

  private JsonObject getJsonObject(JsonObject jsonObject, String name) {
    if (jsonObject == null || !jsonObject.has(name) || !jsonObject.get(name).isJsonObject()) {
      return null;
    }

    return jsonObject.getAsJsonObject(name);
  }

  private Double getDouble(JsonObject jsonObject, String name) {
    if (jsonObject == null || !jsonObject.has(name) || jsonObject.get(name).isJsonNull()) {
      return null;
    }

    return jsonObject.get(name).getAsDouble();
  }

  private Integer getInteger(JsonObject jsonObject, String name) {
    if (jsonObject == null || !jsonObject.has(name) || jsonObject.get(name).isJsonNull()) {
      return null;
    }

    return jsonObject.get(name).getAsInt();
  }

  private TwinUpdateContext createContext(String sourceName, JsonObject j1939, JsonObject n2k, JsonObject packet) {
    TwinUpdateContext context = new TwinUpdateContext();

    context.setUpdateSource("n2k-updater");
    context.setSourceInstanceId(resolveSourceInstanceId(sourceName, j1939));
    context.setReceivedTime(Instant.now());
    context.setSequenceNumber(resolveSequenceNumber(packet));
    context.setReason(resolveN2kName(n2k));
    context.setFullSnapshot(false);

    return context;
  }

  private String resolveSourceInstanceId(String sourceName, JsonObject j1939) {
    Integer sourceAddress = getInteger(j1939, "source");

    if (sourceAddress == null) {
      return sourceName;
    }

    return sourceName + ":source-" + sourceAddress;
  }

  private Long resolveSequenceNumber(JsonObject packet) {
    Double sequenceId = getDouble(packet, "sequenceId");

    if (sequenceId == null) {
      sequenceId = getDouble(packet, "sid");
    }

    if (sequenceId == null) {
      return null;
    }

    return sequenceId.longValue();
  }

  private String resolveN2kName(JsonObject n2k) {
    if (n2k == null || !n2k.has("name") || n2k.get("name").isJsonNull()) {
      return null;
    }

    return n2k.get("name").getAsString();
  }
}