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

package io.mapsmessaging.network.protocol.impl.semtech;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.dto.rest.config.protocol.impl.SemtechConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.SemtechProtocolInformation;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.semtech.handlers.PacketHandler;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutionException;

public class SemTechProtocol extends Protocol {

  @Getter
  private final Logger logger;
  private final SelectorTask selectorTask;
  private final PacketFactory packetFactory;
  private final Session session;
  private final boolean isClient;
  private final byte[] gatewayId;

  @Getter
  private final GatewayManager gatewayManager;

  protected SemTechProtocol(@NonNull @NotNull EndPoint endPoint) throws IOException {
    this(endPoint, null);
  }

  protected SemTechProtocol(@NonNull @NotNull EndPoint endPoint, String sessionId) throws IOException {
    super(endPoint, endPoint.getConfig().getProtocolConfig("semtech"));
    logger = LoggerFactory.getLogger("SemTech Protocol on " + endPoint.getName());
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
    packetFactory = new PacketFactory();
    transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        "semtech",
        "anonymous"
    );

    SemtechConfigDTO semtechConfig = (SemtechConfigDTO) protocolConfig;
    int maxQueued = semtechConfig.getMaxQueued();
    SessionContext sessionContext = new SessionContext("SemTech-Gateway:" + endPoint.getName(), new ProtocolClientConnection(this));
    sessionContext.setPersistentSession(false);
    sessionContext.setResetState(true);
    sessionContext.setReceiveMaximum(maxQueued);
    try {
      session = SessionManager.getInstance().createAsync(sessionContext, this).get();
      String inboundTopicName = semtechConfig.getInboundTopicName();
      String outboundTopicName = semtechConfig.getOutboundTopicName();
      String statusTopicName = semtechConfig.getStatusTopicName();
      gatewayManager = new GatewayManager(session, inboundTopicName, statusTopicName, outboundTopicName, maxQueued);
    } catch (InterruptedException | ExecutionException e) {
      if (Thread.currentThread().isInterrupted()) {
        endPoint.close();
        Thread.currentThread().interrupt();
      }
      throw new IOException(e.getMessage());
    }
    isClient = (sessionId == null);
    if(isClient){
      gatewayId = null;
    }
    else{
      gatewayId = sessionId.getBytes();
    }
  }

  @Override
  public void close() throws IOException {
    logger.log(ServerLogMessages.SEMTECH_CLOSE, endPoint.toString());
    SessionManager.getInstance().close(session, true);
    endPoint.close();
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    SemtechProtocolInformation information = new SemtechProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    logger.log(ServerLogMessages.SEMTECH_QUEUE_MESSAGE, messageEvent.getMessage());
    String alias = messageEvent.getSubscription().getContext().getAlias();
    GatewayInfo info = gatewayManager.getInfo(alias);
    info.getWaitingMessages().offer(messageEvent);
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    SemTechPacket semTechPacket = packetFactory.parse(packet);
    if (semTechPacket != null) {
      logger.log(ServerLogMessages.RECEIVE_PACKET, semTechPacket);
      PacketHandler.getInstance().handle(this, semTechPacket);
    }
    return true;
  }

  public void sendPacket(@NotNull @NonNull SemTechPacket semTechPacket) {
    EndPoint.totalReceived.increment();
    selectorTask.push(semTechPacket);
    logger.log(ServerLogMessages.SEMTECH_SENDING_PACKET, semTechPacket);
    sentMessage();
  }

  @Override
  public String getName() {
    return "semtech";
  }

  @Override
  public String getSessionId() {
    return session.getName();
  }

  @Override
  public String getVersion() {
    return "2";
  }


}
