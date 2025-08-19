/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.protocol;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.SatelliteConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.SatelliteProtocolInformation;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.MessageData;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.io.SatelliteEndPoint;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.SatelliteMessage;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.SatelliteMessageFactory;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.SatelliteMessageRebuilder;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.mapsmessaging.logging.ServerLogMessages.OGWS_FAILED_TO_SAVE_MESSAGE;


public class SatelliteGatewayProtocol extends Protocol {

  private final Logger logger = LoggerFactory.getLogger(SatelliteGatewayProtocol.class);
  private final SatelliteMessageRebuilder messageRebuilder;
  private final Session session;
  private final String namespacePath;
  private final int maxBufferSize;
  private final int compressionThreshold;

  private boolean closed;

  public SatelliteGatewayProtocol(@NonNull @NotNull EndPoint endPoint, @NotNull @NonNull ProtocolConfigDTO protocolConfig) throws LoginException, IOException {
    super(endPoint, protocolConfig);
    String primeId = ((SatelliteEndPoint) endPoint).getTerminalInfo().getUniqueId();
    SatelliteConfigDTO config = (SatelliteConfigDTO) protocolConfig;

    maxBufferSize = config.getMaxBufferSize();
    compressionThreshold = config.getCompressionCutoffSize();
    closed = false;
    messageRebuilder = new SatelliteMessageRebuilder();
    SessionContextBuilder scb = new SessionContextBuilder(primeId, new ProtocolClientConnection(this));
    scb.setPersistentSession(false)
        .setResetState(true)
        .setSessionExpiry(100)
        .setReceiveMaximum(config.getMaxInflightEventsPerDevice());


    long millis = TimeUnit.SECONDS.toMillis(config.getMaxInflightEventsPerDevice());
    Random random = new Random();
    setKeepAlive(millis + random.nextLong(millis));
    session = SessionManager.getInstance().create(scb.build(), this);
    session.resumeState();
    namespacePath = config.getOutboundNamespaceRoot().trim();
    if(!namespacePath.isEmpty()){
      String path = namespacePath.replace("{deviceId}", primeId);
      SubscriptionContextBuilder subBuilder = new SubscriptionContextBuilder(path, ClientAcknowledgement.AUTO);
      subBuilder.setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(config.getMaxInflightEventsPerDevice())
          .setNoLocalMessages(true);
      session.addSubscription(subBuilder.build());
    }

    String bcast = config.getOutboundBroadcast();
    if(bcast != null && !bcast.isEmpty()){
      SubscriptionContextBuilder subBuilder = new SubscriptionContextBuilder(bcast, ClientAcknowledgement.AUTO);
      subBuilder.setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(config.getMaxInflightEventsPerDevice())
          .setNoLocalMessages(true);
      session.addSubscription(subBuilder.build());
    }
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      SessionManager.getInstance().close(session, false);
      super.close();
    }
  }

  public Subject getSubject() {
    if (session != null) {
      return session.getSecurityContext().getSubject();
    }
    return new Subject();
  }

  public void sendKeepAlive() {
    try {
      ((SatelliteEndPoint) endPoint).updateTerminalInfo();
    } catch (IOException e) {
      // log
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public String getSessionId() {
    if (session == null) {
      return "waiting";
    }
    return session.getName();
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    SatelliteProtocolInformation information = new SatelliteProtocolInformation();
    updateInformation(information);
    information.setRemoteDeviceInfo(((SatelliteEndPoint) endPoint).getTerminalInfo());
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }


  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    // To Do - Transform here
    String destinationName = messageEvent.getDestinationName();
    if(!namespacePath.isEmpty() && !destinationName.startsWith(namespacePath)){
      destinationName = "/inbound";
    }

    List<SatelliteMessage> satelliteMessages = SatelliteMessageFactory.createMessages(destinationName, messageEvent.getMessage().getOpaqueData(), maxBufferSize, compressionThreshold);
    for (SatelliteMessage satelliteMessage : satelliteMessages) {
      byte[] tmp = satelliteMessage.packToSend();
      byte[] payload = new byte[tmp.length + 2];
      payload[0] = (byte) 0x81;
      payload[1] = (byte) 1;
      System.arraycopy(tmp, 0, payload, 2, tmp.length);
      MessageData submitMessage = new MessageData();
      submitMessage.setPayload(payload);
      submitMessage.setCompletionCallback(messageEvent.getCompletionTask());
      ((SatelliteEndPoint) endPoint).sendMessage(submitMessage);
    }
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    return false;
  }

  @Override
  public String getName() {
    return ((SatelliteEndPoint) endPoint).getTerminalInfo().getUniqueId();
  }

  @Override
  public String getVersion() {
    return "0.1-beta";
  }

  public void handleIncomingMessage(MessageData message) throws ExecutionException, InterruptedException {
    byte[] raw = message.getPayload();
    byte sin = raw[0];
    byte min = raw[1];
    byte[] tmp = new byte[raw.length - 2];
    System.arraycopy(raw, 2, tmp, 0, tmp.length);
    SatelliteMessage satelliteMessage = new SatelliteMessage(tmp);
    satelliteMessage = messageRebuilder.rebuild(satelliteMessage);
    if (satelliteMessage != null) {
      byte[] buffer = satelliteMessage.getMessage();
      String namespace = satelliteMessage.getNamespace();
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(buffer);
      Message mapsMessage = messageBuilder.build();
      // Transform

      CompletableFuture<Destination> future = session.findDestination(namespace, DestinationType.TOPIC);
      future.thenApply(destination -> {
        if (destination != null) {
          try {
            destination.storeMessage(mapsMessage);
          } catch (IOException e) {
            logger.log(OGWS_FAILED_TO_SAVE_MESSAGE, e);
            try {
              endPoint.close();
            } catch (IOException ioException) {
              // ignore
            }
            future.completeExceptionally(e);
          }
        }
        return destination;
      });
      future.get();
    }
  }
}
