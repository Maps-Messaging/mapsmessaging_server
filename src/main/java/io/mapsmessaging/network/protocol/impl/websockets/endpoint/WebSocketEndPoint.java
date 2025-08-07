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

package io.mapsmessaging.network.protocol.impl.websockets.endpoint;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.FutureTask;

//ToDo:: Configure packet sizes, break up the packets if its too large, deal with fragmented incoming packets
public class WebSocketEndPoint extends EndPoint {

  private final EndPoint endPoint;
  private final WebSocketPacket wsReadPacket;
  private final WebSocketPacket wsWritePacket;

  public WebSocketEndPoint(EndPoint endPoint) {
    super(endPoint.getId(), endPoint.getServer());
    this.endPoint = endPoint;
    String tmp = endPoint.getName();
    if (tmp.startsWith("tcp")) {
      name = "ws" + tmp.substring(3);
    } else if (tmp.startsWith("ssl")) {
      name = "wss" + tmp.substring(3);
    } else {
      name = tmp;
    }
    wsReadPacket = new WebSocketPacket(1024 * 128);
    wsWritePacket = new WebSocketPacket(1024 * 128);
  }

  @Override
  public void close() throws IOException {
    endPoint.close();
    super.close();
  }

  @Override
  public String getProtocol() {
    if (endPoint.isSSL()) {
      return "wss";
    }
    return "ws";
  }

  @Override
  public boolean isUDP() {
    return false;
  }

  @Override
  public List<String> getJMXTypePath() {
    return endPoint.getJMXTypePath();
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    wsWritePacket.clear();
    wsWritePacket.pack(packet);
    wsWritePacket.flip();
    int sent = wsWritePacket.available();
    endPoint.sendPacket(wsWritePacket);
    endPoint.sendPacket(packet);
    updateWriteBytes(sent);

    return sent;
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    int len = endPoint.readPacket(wsReadPacket);
    while (len > 0) {
      updateReadBytes(len);
      wsReadPacket.flip();
      wsReadPacket.parse();
      WebSocketHeader header = wsReadPacket.getHeader();
      switch (header.getOpCode()) {
        case WebSocketHeader.PING:
        case WebSocketHeader.PONG:
          wsReadPacket.position(0);
          wsWritePacket.clear();
          wsWritePacket.getHeader().setOpCode((byte) WebSocketHeader.PONG);
          wsWritePacket.getHeader().setCompleted(true);
          wsWritePacket.getHeader().setFinish(true);
          wsWritePacket.pack();
          endPoint.sendPacket(wsWritePacket);
          return 0;

        case WebSocketHeader.BINARY:
        case WebSocketHeader.TEXT:
        case WebSocketHeader.CONTINUATION:
          if (header.isMask() &&
              wsReadPacket.available() >= header.getLength()) {
            wsReadPacket.copy(packet);
            wsReadPacket.getHeader().reset();
          }
          break;

        case WebSocketHeader.CLOSE:
        default:
          close();
          return -1;
      }
      if (wsReadPacket.hasRemaining()) {
        len = wsReadPacket.available();
        wsReadPacket.compact();
      } else {
        wsReadPacket.clear();
        len = 0;
      }
    }
    return packet.position();
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    return endPoint.register(selectionKey, runner);
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) throws ClosedChannelException {
    return endPoint.deregister(selectionKey);
  }

  @Override
  public String getAuthenticationConfig() {
    return endPoint.getAuthenticationConfig();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(WebSocketEndPoint.class);
  }
}
