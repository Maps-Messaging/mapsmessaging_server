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

package io.mapsmessaging.network.protocol.impl.websockets;

import static java.nio.channels.SelectionKey.OP_READ;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.websockets.endpoint.WebSocketEndPoint;
import java.io.IOException;
import javax.security.auth.Subject;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class WebSocketProtocol extends Protocol {

  private final Connecting connectingHandler;
  private final SelectorTask selectorTask;
  private boolean upgraded;
  private boolean waitingForUpgrade;

  public WebSocketProtocol(EndPoint endPoint, Packet packet) throws IOException {
    super(endPoint, new ProtocolConfigDTO());
    connectingHandler = new Connecting();
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());

    boolean complete = processPacket(packet);
    selectorTask.getReadTask().pushOutstandingData(packet);
    if (!complete) {
      waitingForUpgrade = true;
      selectorTask.register(OP_READ);
    }
  }

  @Override
  public void close() throws IOException {
    if (waitingForUpgrade) {
      selectorTask.close();
      waitingForUpgrade = false;
    }
    super.close();
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    // This is an in-line protocol and does not actively receive events to send from the messaging engine
  }

  @Override
  public Subject getSubject() {
    return null;
  }

  @Override
  public void sendKeepAlive() {
    // Keep alives are sent via the embedded protocol
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    return null;
  }

  @Override
  public boolean processPacket(@NotNull Packet packet) throws IOException {
    if (upgraded) {
      return true;
    }

    ServerPacket serverPacket = connectingHandler.handle(packet, endPoint);
    if (serverPacket == null) {
      return false;
    }

    if (waitingForUpgrade) {
      selectorTask.close();
      waitingForUpgrade = false;
    }

    Packet response = new Packet(64 * 1024, false);
    serverPacket.packFrame(response);
    response.flip();
    while (response.hasRemaining()) {
      int written = endPoint.sendPacket(response);
      if (written <= 0) {
        throw new IOException("Unable to complete WebSocket upgrade response");
      }
    }

    upgraded = true;
    WebSocketEndPoint webSocketEndPoint = new WebSocketEndPoint(endPoint, packet);
    endPoint.getServer().handleNewEndPoint(webSocketEndPoint);
    return true;
  }

  @Override
  public String getName() {
    return "WebSockets-SubProtocol";
  }

  @Override
  public String getSessionId() {
    return null;
  }

  @Override
  public String getVersion() {
    return null;
  }
}
