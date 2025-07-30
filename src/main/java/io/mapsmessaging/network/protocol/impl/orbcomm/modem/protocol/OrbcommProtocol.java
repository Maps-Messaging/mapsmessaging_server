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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.protocol;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.OrbcommProtocolInformation;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.orbcomm.protocol.OrbCommMessage;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class OrbcommProtocol extends Protocol implements Consumer<Packet> {

  private final Session session;
  private final SelectorTask selectorTask;
  private final Modem modem;

  public OrbcommProtocol(EndPoint endPoint, Packet packet) throws LoginException, IOException {
    super(endPoint,  endPoint.getConfig().getProtocolConfig("stogi"));

    if (packet != null) {
      packet.clear();
    }
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("stogi" + endPoint.getId(), new ProtocolClientConnection(this));
    sessionContextBuilder.setSessionExpiry(0);
    sessionContextBuilder.setKeepAlive(0);
    sessionContextBuilder.setPersistentSession(false);
    session = SessionManager.getInstance().create(sessionContextBuilder.build(), this);
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    ProtocolMessageTransformation transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        "stogi",
        session.getSecurityContext().getUsername()
    );
    setTransformation(transformation);
    ConfigurationProperties configurationProperties = ConfigurationManager.getInstance().getProperties("stogi");
    boolean setServerLocation = configurationProperties.getBooleanProperty("serverLocation", false);
    modem = new Modem(this);
    try {
      String init = modem.initializeModem().get();
      String query = modem.queryModemInfo().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      // log this...
    }
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException {
    completedConnection();
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @Nullable ParserExecutor executor, @Nullable Transformer transformer) {
    // Will send a subscribe event, once we have one
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    OrbCommMessage message = new OrbCommMessage(messageEvent);
    modem.sendMessage("maps_messaging", 1, 128, 0,  message.packToSend());
  }

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
  }


  @Override
  public void sendKeepAlive() {
    // no op
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    while (packet.hasRemaining()) {
      modem.process(packet);
    }
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    return true;
  }


  @Override
  public String getName() {
    return "stogi";
  }

  @Override
  public String getSessionId() {
    return "stogi" + endPoint.getName();
  }

  @Override
  public String getVersion() {
    return "0.1";
  }

  @Override
  public void accept(Packet packet) {
    try {
      endPoint.sendPacket(packet);
    } catch (IOException e) {
      // log this
    }
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    OrbcommProtocolInformation information = new OrbcommProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

}
