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

package io.mapsmessaging.network.protocol.impl.mavlink;

import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_DESTINATION_LOOKUP_FAILED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_DESTINATION_NOT_AVAILABLE;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_FAILED_SENDING_OUTBOUND_PACKET;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_FAILED_STORING_PACKET_MESSAGE;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_OUTBOUND_MESSAGE_IGNORED_ENDPOINT_MISMATCH;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_OUTBOUND_MESSAGE_IGNORED_INVALID_CORRELATION;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_OUTBOUND_MESSAGE_IGNORED_NO_CORRELATION;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkAcceptedSourceDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.MavlinkProtocolInformation;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.mavlink.MavlinkEventFactory;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.mavlink.message.Frame;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mavlink.monitor.SequenceResult;
import io.mapsmessaging.network.protocol.impl.mavlink.monitor.SequenceTracker;
import io.mapsmessaging.schemas.config.impl.MavlinkSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.security.auth.Subject;

import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class MavlinkProtocol extends Protocol {

  private static final Logger logger = LoggerFactory.getLogger(MavlinkProtocol.class);

  private final Gson gson;
  private final MavlinkConnectionManager factory;
  private final MavlinkDeviceKey key;
  protected final MavlinkConfigDTO mavlinkConfig;
  protected Session session;
  private final Map<Integer, MavlinkAcceptedSourceDTO> acceptedComponents;
  private final SequenceTracker tracker;
  private final String outboundTopicName;
  protected final MavlinkEventFactory mavlinkEventFactory;
  protected final MessageFormatter formatter;
  private final QualityOfService qos;
  private final boolean storeOffline;
  private final AtomicInteger sequenceCounter = new AtomicInteger(0);
  private final MavlinkHeartbeatEmitter heartbeatEmitter;
  private ScheduledFuture<?> heartbeatFuture;


  protected MavlinkProtocol(
      @NonNull @NotNull MavlinkConnectionManager factory,
      @NonNull @NotNull MavlinkDeviceKey key,
      @NonNull @NotNull EndPoint endPoint,
      @NotNull @NonNull ProtocolConfigDTO protocolConfig)
      throws IOException {
    super(endPoint, protocolConfig);
    this.factory = factory;
    this.key = key;
    tracker = new SequenceTracker();
    this.mavlinkConfig = (MavlinkConfigDTO) protocolConfig;
    String dialectName = mavlinkConfig.getDialectName();
    mavlinkEventFactory = MavlinkInterfaceManager.loadDialect(dialectName);

    MavlinkSchemaConfig config = new MavlinkSchemaConfig();
    config.setDialect(dialectName);
    formatter = MessageFormatterFactory.getInstance().getFormatter(config);

    if (mavlinkConfig.getAcceptedSources() != null) {
      acceptedComponents = new LinkedHashMap<>();
      for (MavlinkAcceptedSourceDTO acceptedSourceDTO : mavlinkConfig.getAcceptedSources()) {
        if (acceptedSourceDTO.getSystemId() == key.getSystemId()) {
          acceptedComponents.put(acceptedSourceDTO.getComponentId(), acceptedSourceDTO);
        }
      }
    } else {
      acceptedComponents = null;
    }

    storeOffline = mavlinkConfig.isStoreOffline();
    qos = QualityOfService.getInstance(mavlinkConfig.getQualityOfService());
    gson = GsonFactory.createStrictJsonWithSafeFloats();

    try {
      session = buildSession(key.getRemoteAddress().getHostName() + "_" + key.getRemotePort() + "_" + key.getSystemId(), mavlinkConfig.getMaximumSessionExpiry());
    } catch (ExecutionException | TimeoutException e) {
      throw new IOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while creating MAVLink session", e);
    }

    String outboundTopic = mavlinkConfig.getOutboundTopicName();
    if (outboundTopic != null && !outboundTopic.isEmpty()) {
      outboundTopic = outboundTopic.replace("{interfaceName}", endPoint.getConfig().getName());
      SubscriptionContextBuilder subscriptionContextBuilder = new SubscriptionContextBuilder(outboundTopic, ClientAcknowledgement.AUTO);
      subscriptionContextBuilder.setQos(qos);
      subscriptionContextBuilder.setReceiveMaximum(10);
      subscriptionContextBuilder.setNoLocalMessages(true);
      session.addSubscription(subscriptionContextBuilder.build());
    }
    outboundTopicName = outboundTopic;
    String remoteSocket = endPoint.getRemoteSocketAddress();
    heartbeatEmitter = new MavlinkHeartbeatEmitter(sequenceCounter, endPoint, mavlinkConfig,  parseSocketAddress(remoteSocket));
    startHeartbeatIfConfigured();
  }

  @Override
  public void close() throws IOException {
    if (!session.isClosed()) {
      SessionManager.getInstance().close(session, false);
    }
    endPoint.close();
    if (mbean != null) {
      mbean.close();
    }
    super.close();
    factory.close(key);
  }

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    MavlinkProtocolInformation information = new MavlinkProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    try {
      byte[] correlationData = messageEvent.getMessage().getCorrelationData();
      if (correlationData == null || correlationData.length == 0) {
        logger.log(MAVLINK_OUTBOUND_MESSAGE_IGNORED_NO_CORRELATION, endPoint.getName());
        return;
      }

      String correlationId = new String(correlationData, StandardCharsets.UTF_8);
      if (!correlationId.startsWith("ID#")) {
        logger.log(MAVLINK_OUTBOUND_MESSAGE_IGNORED_INVALID_CORRELATION, endPoint.getName(), correlationId);
        return;
      }

      String[] parts = correlationId.split("#", 3);
      if (parts.length != 3) {
        logger.log(MAVLINK_OUTBOUND_MESSAGE_IGNORED_INVALID_CORRELATION, endPoint.getName(), correlationId);
        return;
      }

      String endpointId = Long.toString(endPoint.getId());
      if (!endpointId.equals(parts[1])) {
        logger.log(MAVLINK_OUTBOUND_MESSAGE_IGNORED_ENDPOINT_MISMATCH, endPoint.getName(), parts[1], endpointId);
        return;
      }

      String json = new String(messageEvent.getMessage().getOpaqueData(), StandardCharsets.UTF_8);
      JsonObject input = JsonParser.parseString(json).getAsJsonObject();
      String socketAddressText = parts[2];
      try {
        overrideSequence(input);
        byte[] frame = formatter.parseFromJson(input);
        Packet packet = new Packet(ByteBuffer.wrap(frame));
        packet.setFromAddress(parseSocketAddress(socketAddressText));
        endPoint.sendPacket(packet);
      } catch (Throwable e) {
        logger.log(MAVLINK_FAILED_SENDING_OUTBOUND_PACKET, endPoint.getName(), socketAddressText, e);
      }

    } finally {
      messageEvent.getCompletionTask().run();
    }
  }

  private void overrideSequence(JsonObject input) {
    JsonObject header = input.getAsJsonObject("header");
    if (header == null) {
      header = new JsonObject();
      input.add("header", header);
    }
    header.addProperty("sequence", nextSequence());

    if (mavlinkConfig.hasLocalMavlinkIdentity()) {
      header.addProperty("systemId", mavlinkConfig.getSystemId());
      header.addProperty("componentId", mavlinkConfig.getComponentId());
    }
  }

  public int nextSequence() {
    return sequenceCounter.getAndUpdate(value -> (value + 1) & 0xff);
  }


  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    return true;
  }

  public void processRawFrame(ProcessedFrame env, byte[] raw, String socketAddress) {
    endPoint.getEndPointStatus().incrementReceivedMessages();
    endPoint.updateReadBytes(raw.length);

    if (mavlinkConfig.getStatusTopicNameTemplate() != null && !mavlinkConfig.getStatusTopicNameTemplate().isEmpty()) {
      SequenceResult results = tracker.accept(env.getFrame().getSequence());
      if (results.isStatusChanged()) {
        String statusTopic = computeTopicName(mavlinkConfig.getStatusTopicNameTemplate(), env.getFrame(), env.getMessageName());
        JsonObject resultsJson = gson.toJsonTree(results).getAsJsonObject();
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setQoS(qos).storeOffline(storeOffline);
        sendMessage(statusTopic, messageBuilder.setContentType("application/json").setOpaqueData(resultsJson.toString().getBytes(StandardCharsets.UTF_8)).build());
      }
    }

    boolean allow = acceptedComponents == null || acceptedComponents.isEmpty() || acceptedComponents.containsKey(env.getFrame().getComponentId());
    if (allow && allowMessageId(env.getFrame().getComponentId(), env.getFrame().getMessageId())) {
      if (mavlinkConfig.isParseToJson()) {
        Map<String, Object> parsed = env.getFields();
        JsonObject complete = MavlinkJsonEnvelopeBuilder.toJson(env.getFrame(), parsed);
        JsonObject envelope = new JsonObject();
        envelope.add("mavlink", complete);
        if (env.getDetections() != null && !env.getDetections().isEmpty()) {
          envelope.add("detections", gson.toJsonTree(env.getDetections()).getAsJsonArray());
        }
        raw = envelope.toString().getBytes();
      }
      processPacket(env.getFrame(), env.getMessageName(), raw, socketAddress);
    } else {
      if (mavlinkConfig.getRejectedFrameNamespace() != null && !mavlinkConfig.getRejectedFrameNamespace().isEmpty()) {
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setQoS(qos).storeOffline(storeOffline);
        if (mavlinkConfig.isIncludeRejectedFrameMetadata()) {
          JsonObject metadata = new JsonObject();
          metadata.addProperty("messageName", env.getMessageName());
          metadata.addProperty("messageId", env.getFrame().getMessageId());
          metadata.addProperty("systemId", env.getFrame().getSystemId());
          metadata.addProperty("componentId", env.getFrame().getComponentId());
          metadata.addProperty("payload", Base64.getEncoder().encodeToString(env.getFrame().getPayload()));
          metadata.addProperty("sequence", env.getFrame().getSequence());
          metadata.addProperty("signed", env.getFrame().isSigned());
          metadata.addProperty("time_ms", System.currentTimeMillis());
          messageBuilder.setOpaqueData(metadata.toString().getBytes(StandardCharsets.UTF_8));
        } else {
          messageBuilder.setOpaqueData(raw);
        }
        String topicName = computeTopicName(mavlinkConfig.getRejectedFrameNamespace(), env.getFrame(), env.getMessageName());
        sendMessage(topicName, messageBuilder.build());
      }
    }
  }

  public boolean processPacket(@NonNull @NotNull Frame envelope, String messageName, byte[] raw, String socketAddress) {
    MessageBuilder messageBuilder = new MessageBuilder();
    Map<String, String> metaData = new HashMap<>();
    metaData.put("protocol", "MavLink");
    metaData.put("version", "" + envelope.getVersion());
    metaData.put("sessionId", session.getName());
    metaData.put("time_ms", "" + System.currentTimeMillis());

    Message message =
        messageBuilder
            .setContentType("mavlink")
            .setOpaqueData(raw)
            .setDataMap(convertToMap(envelope))
            .setQoS(qos)
            .setRetain(false)
            .setResponseTopic(outboundTopicName)
            .setCorrelationData("ID#" + endPoint.getId() + "#" + socketAddress)
            .storeOffline(storeOffline)
            .setMeta(metaData)
            .build();

    String topicName = computeTopicName(mavlinkConfig.getTopicNameTemplate(), envelope, messageName);
    sendMessage(topicName, message);
    return true;
  }

  private void sendMessage(String topicName, Message message) {
    CompletableFuture<Destination> future = session.findDestination(topicName, DestinationType.TOPIC);
    if (future == null) {
      logger.log(MAVLINK_DESTINATION_NOT_AVAILABLE, topicName);
      return;
    }

    future.whenComplete(
        (destination, throwable) -> {
          if (throwable != null) {
            logger.log(MAVLINK_DESTINATION_LOOKUP_FAILED, topicName, throwable);
            return;
          }

          try {
            destination.storeMessage(message);
          } catch (IOException e) {
            logger.log(MAVLINK_FAILED_STORING_PACKET_MESSAGE, topicName, e);
          }
        });
  }

  @Override
  public String getName() {
    return "mavlink";
  }

  @Override
  public String getSessionId() {
    if (session != null) {
      return session.getName();
    }
    return "waiting";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  protected String computeTopicName(String template, Frame envelope, String messageName) {
    template = template.replace("{remoteSocket}", getRemoteSocket());
    template = template.replace("{systemName}", "" + envelope.getSystemId());
    template = template.replace("{systemId}", "" + envelope.getSystemId());
    template = template.replace("{componentId}", "" + envelope.getComponentId());
    template = template.replace("{messageId}", "" + envelope.getMessageId());
    template = template.replace("{messageName}", messageName);
    return template;
  }

  protected String getRemoteSocket() {
    return key.getRemoteAddress().getHostName() + "_" + key.getRemoteAddress().getPort();
  }

  private Map<String, TypedData> convertToMap(Frame envelope) {
    Map<String, TypedData> map = new LinkedHashMap<>();
    map.put("version", new TypedData(envelope.getVersion().toString()));
    map.put("systemId", new TypedData(envelope.getSystemId()));
    map.put("componentId", new TypedData(envelope.getComponentId()));
    map.put("sequence", new TypedData(envelope.getSequence()));
    map.put("payload", new TypedData(envelope.getPayload()));
    map.put("signed", new TypedData(envelope.isSigned()));
    return map;
  }

  public boolean allowMessageId(int componentId, int messageId){
    if(acceptedComponents == null || acceptedComponents.isEmpty()) return true;
    MavlinkAcceptedSourceDTO knownSource = acceptedComponents.get(componentId);
    if(knownSource == null) return false;
    if(knownSource.getAcceptedMessageIds().isEmpty()){
      return mavlinkConfig.getAcceptedMessageIds().isEmpty() || mavlinkConfig.getAcceptedMessageIds().contains(messageId);
    }
    return knownSource.getAcceptedMessageIds().contains(messageId);
  }

  private static InetSocketAddress parseSocketAddress(String socketAddressText) {
    if (socketAddressText == null || socketAddressText.isBlank()) {
      throw new IllegalArgumentException("Socket address must not be null or blank");
    }

    String trimmedSocketAddress = socketAddressText.trim();

    int portSeparatorIndex = trimmedSocketAddress.lastIndexOf(':');
    if (portSeparatorIndex < 0 || portSeparatorIndex == trimmedSocketAddress.length() - 1) {
      throw new IllegalArgumentException("Socket address must include a port: " + socketAddressText);
    }

    String hostPart = trimmedSocketAddress.substring(0, portSeparatorIndex);
    String portPart = trimmedSocketAddress.substring(portSeparatorIndex + 1);

    if (hostPart.startsWith("/")) {
      hostPart = hostPart.substring(1);
    }

    int slashIndex = hostPart.indexOf('/');
    if (slashIndex >= 0) {
      hostPart = hostPart.substring(0, slashIndex);
    }

    if (hostPart.isBlank()) {
      throw new IllegalArgumentException("Socket address must include a host: " + socketAddressText);
    }

    int port = Integer.parseInt(portPart);

    return new InetSocketAddress(hostPart, port);
  }


  private void startHeartbeatIfConfigured() {
    if (!mavlinkConfig.hasLocalMavlinkIdentity()) {
      return;
    }

    long intervalSeconds = Math.max(1, mavlinkConfig.getHeartbeatIntervalSeconds());
    SimpleTaskScheduler.getInstance().scheduleAtFixedRate(heartbeatEmitter, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
  }

  private void stopHeartbeat() {
    if (heartbeatFuture != null) {
      heartbeatFuture.cancel(false);
      heartbeatFuture = null;
    }
  }

}