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

package io.mapsmessaging.network.protocol.impl.orbcomm.ogws.protocol;

import com.amazonaws.util.Base64;
import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.OrbcommProtocolInformation;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.data.CommonMessage;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.data.ElementType;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.data.Field;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.data.ReturnMessage;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.io.OrbcommOgwsEndPoint;
import io.mapsmessaging.network.protocol.impl.orbcomm.protocol.OrbCommMessage;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.mapsmessaging.logging.ServerLogMessages.OGWS_FAILED_TO_SAVE_MESSAGE;


public class OrbCommOgwsProtocol extends Protocol {

  private final Logger logger = LoggerFactory.getLogger(OrbCommOgwsProtocol.class);
  private final Session session;
  private final String primeId;
  private boolean closed;

  public OrbCommOgwsProtocol(@NonNull @NotNull EndPoint endPoint, @NotNull @NonNull ProtocolConfigDTO protocolConfig) throws LoginException, IOException {
    super(endPoint, protocolConfig);
    primeId = ((OrbcommOgwsEndPoint)endPoint).getTerminalInfo().getPrimeId();
    closed = false;
    SessionContextBuilder scb = new SessionContextBuilder(primeId, new ProtocolClientConnection(this));
    scb.setPersistentSession(false)
        .setResetState(true)
        .setKeepAlive(60000)
        .setSessionExpiry(100)
        .setReceiveMaximum(2);

    session = SessionManager.getInstance().create(scb.build(), this);
    session.resumeState(); // We have established a session to read/write with this prime id
  }

  protected OrbCommOgwsProtocol(@NonNull @NotNull EndPoint endPoint, @NonNull @NotNull SocketAddress socketAddress, @NotNull @NonNull ProtocolConfigDTO protocolConfig) {
    super(endPoint, socketAddress, protocolConfig);
    primeId = ((OrbcommOgwsEndPoint)endPoint).getTerminalInfo().getPrimeId();
    closed = false;
    session = null;
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
    if(session != null) {
      return session.getSecurityContext().getSubject();
    }
    return new Subject();
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
    OrbcommProtocolInformation information = new OrbcommProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }


  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    // To Do - Transform here
    OrbCommMessage orbCommMessage = new OrbCommMessage(messageEvent.getDestinationName(), messageEvent.getMessage().getOpaqueData());
    CommonMessage commonMessage = new CommonMessage();
    commonMessage.setSin(128);
    commonMessage.setMin(1);
    commonMessage.setName("maps_message");
    List<Field> fields = new ArrayList<>();
    Field field = new Field();
    field.setName("data");
    field.setValue(Base64.encodeAsString(orbCommMessage.packToSend()));
    field.setType(ElementType.DATA);
    fields.add(field);
    commonMessage.setFields(fields);
    ((OrbcommOgwsEndPoint)endPoint).sendMessage(commonMessage);
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    return false;
  }

  @Override
  public String getName() {
    return ((OrbcommOgwsEndPoint)endPoint).getTerminalInfo().getPrimeId();
  }

  @Override
  public String getVersion() {
    return "0.1-beta";
  }

  public void handleIncomingMessage(ReturnMessage message) throws ExecutionException, InterruptedException {
    byte[] data  = Base64.decode(message.getRawPayload());

    processMessage(data);
  }

  private void processMessage(byte[] raw) throws ExecutionException, InterruptedException {
    byte sin = raw[0];
    byte min = raw[1];
    byte[] tmp = new byte[raw.length - 1];
    System.arraycopy(raw, 1, tmp, 0, tmp.length);
    OrbCommMessage orbCommMessage = new OrbCommMessage(tmp);
    byte[] buffer = orbCommMessage.getMessage();
    String namespace = orbCommMessage.getNamespace();
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
