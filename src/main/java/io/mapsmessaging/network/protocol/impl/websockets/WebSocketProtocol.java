/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.websockets;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.websockets.endpoint.WebSocketEndPoint;
import java.io.IOException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class WebSocketProtocol extends ProtocolImpl {

  private final Connecting connectingHandler;

  public WebSocketProtocol(EndPoint endPoint, Packet packet) throws IOException {
    super(endPoint);
    connectingHandler = new Connecting();
    processPacket(packet);
  }

  @Override
  public void sendMessage(@NonNull @NotNull Destination destination, @NonNull @NotNull String normalisedName, @NonNull @NotNull SubscribedEventManager subscription, @NonNull @NotNull Message message, @NonNull @NotNull Runnable completionTask) {
    // This is an in-line protocol and does not actively receive events to send from the messaging engine
  }

  @Override
  public void sendKeepAlive() {
    // Keep alives are sent via the embedded protocol
  }

  @Override
  public boolean processPacket(@NotNull Packet packet) throws IOException {
    ServerPacket serverPacket = connectingHandler.handle(packet, endPoint);
    if (serverPacket != null) {
      Packet response = new Packet(64 * 1024, false);
      serverPacket.packFrame(response);
      response.flip();
      endPoint.sendPacket(response);
      WebSocketEndPoint webSocketEndPoint = new WebSocketEndPoint(endPoint);
      endPoint.getServer().handleNewEndPoint(webSocketEndPoint);
    }
    return true;
  }

  @Override
  public String getName() {
    return "WebSockets-SubProtocol";
  }

  @Override
  public String getSessionId() {
    // There is no Session ID related to this protocol
    return null;
  }

  @Override
  public String getVersion() {
    // there is no version to return
    return null;
  }
}
