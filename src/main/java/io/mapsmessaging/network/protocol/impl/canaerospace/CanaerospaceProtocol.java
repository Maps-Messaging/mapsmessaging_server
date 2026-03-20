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
 *
 */

package io.mapsmessaging.network.protocol.impl.canaerospace;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.CanAerospaceConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.CanAerospaceProtocolInformation;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.canbus.CanbusEndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.CanAerospaceSchemaConfig;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.formatters.impl.CanAerospaceFormatter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static io.mapsmessaging.engine.schema.SchemaManager.DEFAULT_JSON_SCHEMA;
import static io.mapsmessaging.logging.ServerLogMessages.CANAEROSPACE_LOADING_DATABASE_FROM_FILE;
import static io.mapsmessaging.logging.ServerLogMessages.CANAEROSPACE_LOADING_DEFAULT_DATABASE;
import static io.mapsmessaging.logging.ServerLogMessages.CANAEROSPACE_LOADING_FAILED;
import static io.mapsmessaging.logging.ServerLogMessages.CANAEROSPACE_PROTOCOL_CANBUS_BUILD_ERROR;
import static io.mapsmessaging.logging.ServerLogMessages.CANAEROSPACE_PROTOCOL_CLOSING;
import static io.mapsmessaging.logging.ServerLogMessages.CANAEROSPACE_PROTOCOL_CREATED_AND_BOUND;
import static io.mapsmessaging.logging.ServerLogMessages.CANAEROSPACE_PROTOCOL_PARSING_PACKET;

public class CanaerospaceProtocol extends Protocol {

  private final Logger logger = LoggerFactory.getLogger(CanaerospaceProtocol.class);

  private final CanAerospaceFormatter formatter;
  private final Session session;
  private final InboundProcessor inboundProcessor;
  private final String topicTemplate;
  private final String rawTopicTemplate;
  private final boolean parseToJson;
  private final SchemaConfig defaultSchemaConfig;

  public CanaerospaceProtocol(CanbusEndPoint endPoint, @NotNull @NonNull ProtocolConfigDTO protocolConfig) throws IOException {
    super(endPoint, protocolConfig);

    CanAerospaceConfigDTO canAerospaceConfig = (CanAerospaceConfigDTO) protocolConfig;
    CanAerospaceSchemaConfig canAerospaceSchemaConfig = new CanAerospaceSchemaConfig();

    try {
      String yamlPath = canAerospaceConfig.getYamlPath();
      if (yamlPath != null && !yamlPath.isEmpty()) {
        logger.log(CANAEROSPACE_LOADING_DATABASE_FROM_FILE, yamlPath);
        canAerospaceSchemaConfig.setYamlPath(yamlPath);
      }
      else {
        logger.log(CANAEROSPACE_LOADING_DEFAULT_DATABASE);
      }
    }
    catch (Exception exception) {
      logger.log(CANAEROSPACE_LOADING_FAILED, exception);
      throw new IOException(exception);
    }

    topicTemplate = canAerospaceConfig.getTopicNameTemplate();
    rawTopicTemplate = canAerospaceConfig.getUnknownPacketTopic().replace("{candevice}", endPoint.getName());
    parseToJson = canAerospaceConfig.isParseToJson();
    defaultSchemaConfig = SchemaManager.getInstance().getSchema(DEFAULT_JSON_SCHEMA);

    formatter = (CanAerospaceFormatter) SchemaManager.getInstance().getMessageFormatter(canAerospaceSchemaConfig);

    try {
      session = buildSession(endPoint.getName(), 10000);
    }
    catch (ExecutionException | TimeoutException exception) {
      throw new IOException(exception);
    }
    catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IOException(exception);
    }

    String inboundTopicName = canAerospaceConfig.getInboundTopicName();
    if (inboundTopicName != null && !inboundTopicName.isEmpty()) {
      SubscriptionContext subscriptionContext = new SubscriptionContextBuilder(inboundTopicName, ClientAcknowledgement.AUTO)
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(10)
          .setNoLocalMessages(true)
          .build();
      session.addSubscription(subscriptionContext);
    }

    inboundProcessor = new InboundProcessor(this);
    Thread thread = new Thread(inboundProcessor, "canaerospace-inbound-" + endPoint.getName());
    thread.start();

    logger.log(CANAEROSPACE_PROTOCOL_CREATED_AND_BOUND, endPoint.getName());
  }

  @Override
  public void close() throws IOException {
    super.close();
    inboundProcessor.close();
    endPoint.close();
    logger.log(CANAEROSPACE_PROTOCOL_CLOSING, endPoint.getName());
  }

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    CanAerospaceProtocolInformation information = new CanAerospaceProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    Message message = messageEvent.getMessage();
    byte[] payload = message.getOpaqueData();
    CanFrame frame = tryBuildFrameFromOpaquePayload(payload);

    if (frame == null) {
      return;
    }

    try {
      ((CanbusEndPoint) endPoint).writeFrame(frame);
    }
    catch (IOException exception) {
      logger.log(CANAEROSPACE_PROTOCOL_CANBUS_BUILD_ERROR, exception);
    }
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    CanFrame frame = ((CanbusEndPoint) endPoint).readFrame();

    if (frame == null) {
      return true;
    }

    if (logger.isDebugEnabled()) {
      logger.log(CANAEROSPACE_PROTOCOL_PARSING_PACKET, packetToString(frame));
    }

    if (parseToJson) {
      ParseMode parseMode = SchemaManager.getInstance().getDefaultParseMode();
      JsonObject json = formatter.parseToJson(frame.getRawData(), parseMode);
      processPacket(json);
    }
    else {
      publishRawFrame(frame);
    }

    return true;
  }

  public boolean processPacket(JsonObject json) {
    MessageBuilder messageBuilder = new MessageBuilder();
    Map<String, String> metadata = new HashMap<>();
    metadata.put("protocol", "canaerospace");
    metadata.put("version", getVersion());
    metadata.put("sessionId", session.getName());
    metadata.put("time_ms", Long.toString(System.currentTimeMillis()));

    Map<String, TypedData> dataMap = new LinkedHashMap<>();
    String messageName = extractMessageName(json);

    Message message = messageBuilder
        .setOpaqueData(json.toString().getBytes(StandardCharsets.UTF_8))
        .setDataMap(dataMap)
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setRetain(false)
        .storeOffline(false)
        .setContentType("application/json")
        .setMeta(metadata)
        .setSchemaId(defaultSchemaConfig.getUniqueId())
        .build();

    String topicName = computeTopicName(messageName);
    publishMessage(topicName, message);
    return true;
  }

  protected String computeTopicName(String messageName) {
    if (messageName == null || messageName.isEmpty()) {
      return rawTopicTemplate;
    }

    String template = topicTemplate;
    template = template.replace("{candevice}", endPoint.getName());
    template = template.replace("{messageName}", messageName);
    return template;
  }

  @Override
  public String getName() {
    return "canaerospace";
  }

  @Override
  public String getSessionId() {
    return session.getName();
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  private CanFrame tryBuildFrameFromOpaquePayload(byte[] payload) {
    if (payload == null || payload.length == 0) {
      return null;
    }

    CanFrame frame = tryParseRawFrame(payload);
    if (frame != null) {
      return frame;
    }

    try {
      String jsonString = new String(payload, StandardCharsets.UTF_8);
      JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
      byte[] rawFrame = formatter.parseFromJson(jsonObject);
      return CanFrame.fromBytes(rawFrame);
    }
    catch (Exception exception) {
      logger.log(CANAEROSPACE_PROTOCOL_CANBUS_BUILD_ERROR, exception);
      return null;
    }
  }

  private CanFrame tryParseRawFrame(byte[] payload) {
    try {
      return CanFrame.fromBytes(payload);
    }
    catch (Exception ignored) {
      return null;
    }
  }

  private void publishRawFrame(CanFrame frame) {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("protocol", "canaerospace");
    metadata.put("version", getVersion());
    metadata.put("sessionId", session.getName());
    metadata.put("time_ms", Long.toString(System.currentTimeMillis()));

    byte[] rawData = frame.getRawData();

    Message message = new MessageBuilder()
        .setOpaqueData(rawData)
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setMeta(metadata)
        .setRetain(false)
        .setContentType("application/octet-stream")
        .build();

    publishMessage(rawTopicTemplate, message);
  }

  private String extractMessageName(JsonObject json) {
    if (json == null || !json.has("canaerospace") || !json.get("canaerospace").isJsonObject()) {
      return null;
    }

    JsonObject canAerospaceObject = json.getAsJsonObject("canaerospace");
    if (!canAerospaceObject.has("name") || canAerospaceObject.get("name").isJsonNull()) {
      return null;
    }

    String name = canAerospaceObject.get("name").getAsString();
    if (name == null || name.isEmpty()) {
      return null;
    }

    return sanitiseTopicToken(name);
  }

  private String sanitiseTopicToken(String value) {
    StringBuilder stringBuilder = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      if (Character.isLetterOrDigit(character) || character == '_' || character == '-' || character == '.') {
        stringBuilder.append(character);
      }
      else {
        stringBuilder.append('_');
      }
    }
    return stringBuilder.toString();
  }

  private String packetToString(CanFrame frame) {
    return "CanId: " + frame.canIdentifier() + " Data: " + Base64.getEncoder().encodeToString(frame.data());
  }

  private void publishMessage(String topicName, Message message) {
    CompletableFuture<Destination> future = session.findDestination(topicName, DestinationType.TOPIC);
    if (future == null) {
      return;
    }

    future.thenApply(destination -> {
      try {
        if (destination != null) {
          if (destination.getSchema() == null
              || SchemaManager.DEFAULT_RAW_UUID.toString().equals(destination.getSchema().getUniqueId())) {
            destination.updateSchema(defaultSchemaConfig, null);
          }
          destination.storeMessage(message);
        }
      }
      catch (IOException exception) {
        future.completeExceptionally(exception);
      }
      return destination;
    });
  }
}