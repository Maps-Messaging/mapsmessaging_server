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
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class MavlinkProtocol extends Protocol {

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

  protected MavlinkProtocol(@NonNull @NotNull MavlinkConnectionManager factory,
                            @NonNull @NotNull MavlinkDeviceKey key,
                            @NonNull @NotNull EndPoint endPoint,
                            @NotNull @NonNull ProtocolConfigDTO protocolConfig) throws IOException {
    super(endPoint, protocolConfig);
    this.factory = factory;
    this.key = key;
    tracker = new SequenceTracker();
    this.mavlinkConfig = (MavlinkConfigDTO)protocolConfig;
    String dialectName = mavlinkConfig.getDialectName();
    mavlinkEventFactory  = MavlinkInterfaceManager.loadDialect(dialectName);
    MavlinkSchemaConfig config = new MavlinkSchemaConfig();
    config.setDialect(dialectName);
    formatter = MessageFormatterFactory.getInstance().getFormatter(config);

    if(mavlinkConfig.getAcceptedSources() != null){
       acceptedComponents = new LinkedHashMap<>();
      for(MavlinkAcceptedSourceDTO acceptedSourceDTO: mavlinkConfig.getAcceptedSources()){
        if(acceptedSourceDTO.getSystemId() == key.getSystemId()) {
          acceptedComponents.put(acceptedSourceDTO.getComponentId(), acceptedSourceDTO);
        }
      }
    }
    else{
      acceptedComponents = null;
    }
    gson = GsonFactory.createStrictJsonWithSafeFloats();
    try {
      session = buildSession(key.getRemoteAddress().getHostName()+"_"+key.getRemotePort()+"_"+key.getSystemId(), mavlinkConfig.getMaximumSessionExpiry());
    } catch (ExecutionException|TimeoutException e) {
      throw new IOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    String outboundTopic = mavlinkConfig.getOutboundTopicName();
    if(outboundTopic != null && !outboundTopic.isEmpty()) {
      outboundTopic = outboundTopic.replace("{interfaceName}", endPoint.getConfig().getName());
      SubscriptionContextBuilder subscriptionContextBuilder = new SubscriptionContextBuilder(outboundTopic, ClientAcknowledgement.AUTO);
      subscriptionContextBuilder.setQos(QualityOfService.AT_MOST_ONCE);
      subscriptionContextBuilder.setReceiveMaximum(10);
      subscriptionContextBuilder.setNoLocalMessages(true);
      session.addSubscription(subscriptionContextBuilder.build());
    }
    outboundTopicName = outboundTopic;
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
        return;
      }

      String correlationId = new String(correlationData, StandardCharsets.UTF_8);
      if (!correlationId.startsWith("ID#")) {
        return;
      }

      String[] parts = correlationId.split("#", 3);
      if (parts.length != 3) {
        return;
      }

      String endpointId = Long.toString(endPoint.getId());
      if (!endpointId.equals(parts[1])) {
        return;
      }

      String json = new String(messageEvent.getMessage().getOpaqueData(), StandardCharsets.UTF_8);
      JsonObject input = JsonParser.parseString(json).getAsJsonObject();
      try {
        byte[] frame = formatter.parseFromJson(input);
        SocketAddress socketAddress = parseSocketAddress(parts[2]);
        Packet packet = new Packet(1024, false);
        packet.setFromAddress(socketAddress);
        packet.put(frame);
        packet.flip();
        endPoint.sendPacket(packet);
      } catch (IOException e) {
        // todo log this
      }

    } finally {
      messageEvent.getCompletionTask().run();
    }
  }
  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    return true;
  }


  public void processRawFrame(ProcessedFrame env, byte[] raw, String socketAddress)  {
    endPoint.getEndPointStatus().incrementReceivedMessages();
    endPoint.updateReadBytes(raw.length);
    if(mavlinkConfig.getStatusTopicNameTemplate() != null && !mavlinkConfig.getStatusTopicNameTemplate().isEmpty() ) {
      SequenceResult results = tracker.accept(env.getFrame().getSequence());
      if (results.isStatusChanged()) {
        String statusTopic = computeTopicName(mavlinkConfig.getStatusTopicNameTemplate(), env.getFrame(), env.getMessageName());
        JsonObject resultsJson = gson.toJsonTree(results).getAsJsonObject();
        sendMessage(statusTopic, new MessageBuilder().setContentType("application/json").setOpaqueData(resultsJson.toString().getBytes(StandardCharsets.UTF_8)).build());
      }
    }

    boolean allow = (acceptedComponents == null || acceptedComponents.isEmpty() || acceptedComponents.containsKey(env.getFrame().getComponentId()) );
    if(allow && allowMessageId(env.getFrame().getComponentId(), env.getFrame().getMessageId())) {
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
    }
    else{
      if(mavlinkConfig.getRejectedFrameNamespace() != null && !mavlinkConfig.getRejectedFrameNamespace().isEmpty()){
        MessageBuilder messageBuilder = new MessageBuilder();
        if(mavlinkConfig.isIncludeRejectedFrameMetadata()){
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
        }
        else{
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
    metaData.put("version", ""+envelope.getVersion());
    metaData.put("sessionId", session.getName());
    metaData.put("time_ms", "" + System.currentTimeMillis());
    Message message = messageBuilder.setContentType("mavlink")
        .setOpaqueData(raw)
        .setDataMap(convertToMap(envelope))
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setRetain(false)
        .storeOffline(false)
        .setResponseTopic(outboundTopicName)
        .setCorrelationData("ID#"+endPoint.getId()+"#"+socketAddress)
        .setMeta(metaData)

        .build();

    String topicName = computeTopicName(mavlinkConfig.getTopicNameTemplate(), envelope, messageName);

    sendMessage(topicName, message);
    return true;
  }

  private void sendMessage(String topicName, Message message) {
    CompletableFuture<Destination> future = session.findDestination(topicName, DestinationType.TOPIC);
    if (future != null) {
      future.thenApply(destination -> {
        try {
          destination.storeMessage(message);
        } catch (IOException e) {
          future.completeExceptionally(e);
        }
        return destination;
      });
    }
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
    template = template.replace("{systemName}", ""+envelope.getSystemId());
    template = template.replace("{systemId}", ""+envelope.getSystemId());
    template = template.replace("{componentId}", ""+envelope.getComponentId());
    template = template.replace("{messageId}", ""+envelope.getMessageId());
    template = template.replace("{messageName}", messageName);
    return template;
  }

  protected String getRemoteSocket(){
    return key.getRemoteAddress().getHostName()+"_"+key.getRemoteAddress().getPort();
  }


  private Map<String, TypedData> convertToMap(Frame envelope) {
    Map<String, TypedData> map = new LinkedHashMap<>();
    map.put("version", new TypedData(envelope.getVersion().toString()));
    map.put("systemId", new TypedData(envelope.getSystemId()));
    map.put("componentId", new TypedData(envelope.getComponentId()));
    map.put("sequence",  new TypedData(envelope.getSequence()));
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
}

