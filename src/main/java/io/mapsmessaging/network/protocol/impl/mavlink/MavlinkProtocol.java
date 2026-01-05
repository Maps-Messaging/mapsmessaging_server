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

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.mavlink.MavlinkFrameEnvelope;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MavlinkProtocol extends Protocol {
  private final MavlinkInterfaceManager factory;
  private final MavlinkDeviceKey key;
  private final MavlinkConfigDTO mavlinkConfig;
  protected Session session;


  protected MavlinkProtocol(@NonNull @NotNull MavlinkInterfaceManager factory,
                            @NonNull @NotNull MavlinkDeviceKey key,
                            @NonNull @NotNull EndPoint endPoint,
                            @NotNull @NonNull ProtocolConfigDTO protocolConfig) throws IOException {
    super(endPoint, protocolConfig);
    this.factory = factory;
    this.key = key;
    this.mavlinkConfig = (MavlinkConfigDTO)protocolConfig;
    try {
      session = buildSession();
    } catch (ExecutionException|TimeoutException e) {
      throw new IOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
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
    return null;
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {

  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    Packet respond = new Packet(packet.getRawBuffer().duplicate());
    respond.setFromAddress(key.getRemoteAddress());
    endPoint.sendPacket(respond);
    return true;
  }

  public boolean processPacket(@NonNull @NotNull MavlinkFrameEnvelope envelope, String messageName, byte[] raw) throws IOException {
    MessageBuilder messageBuilder = new MessageBuilder();
    Map<String, String> metaData = new HashMap<>();
    metaData.put("protocol", "NATS");
    metaData.put("version", ""+envelope.getVersion());
    metaData.put("sessionId", session.getName());
    metaData.put("time_ms", "" + System.currentTimeMillis());

    Message message = messageBuilder.setContentType("mavlink")
        .setOpaqueData(raw)
        .setDataMap(convertToMap(envelope))
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setRetain(false)
        .setStoreOffline(false)
        .setMeta(metaData)
        .build();

    String topicName = computeTopicName(envelope, messageName);


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
    return true;
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

  private Session buildSession() throws ExecutionException, InterruptedException, TimeoutException {
    String sessionid = key.getRemoteAddress().getHostName()+"_"+key.getRemotePort()+"_"+key.getSystemId();
    SessionContextBuilder scb = new SessionContextBuilder(sessionid, new ProtocolClientConnection(this));
    scb.setResetState(true)
      .setSessionExpiry(mavlinkConfig.getMaximumSessionExpiry())
      .setPersistentSession(false)
      .setReceiveMaximum(10);
    CompletableFuture<Session> sessionFuture = SessionManager.getInstance().createAsync(scb.build(), this);
    return sessionFuture.get(5, TimeUnit.SECONDS);
  }

  private String computeTopicName(MavlinkFrameEnvelope envelope, String messageName) {
    String template = mavlinkConfig.getTopicNameTemplate();
    template = template.replace("{remoteSocket}", key.getRemoteAddress().getHostName()+"_"+key.getRemoteAddress().getPort());
    template = template.replace("{systemId}", ""+envelope.getSystemId());
    template = template.replace("{componentId}", ""+envelope.getComponentId());
    template = template.replace("{messageId}", ""+envelope.getMessageId());
    template = template.replace("{messageName}", messageName);
    return template;
  }

  private Map<String, TypedData> convertToMap(MavlinkFrameEnvelope envelope) {
    Map<String, TypedData> map = new LinkedHashMap<>();
    map.put("version", new TypedData(envelope.getVersion().toString()));
    map.put("systemId", new TypedData(envelope.getSystemId()));
    map.put("componentId", new TypedData(envelope.getComponentId()));
    map.put("sequence",  new TypedData(envelope.getSequence()));
    map.put("payload", new TypedData(envelope.getPayload()));
    map.put("signed", new TypedData(envelope.isSigned()));
    return map;
  }
}

