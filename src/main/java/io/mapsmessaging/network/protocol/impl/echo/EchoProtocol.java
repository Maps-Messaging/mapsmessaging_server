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

package io.mapsmessaging.network.protocol.impl.echo;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.dto.rest.config.protocol.impl.LoRaProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.protocol.Protocol;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EchoProtocol extends Protocol implements Selectable {

  private final Executor executor = Executors.newFixedThreadPool(10);
  private final Packet packet;
  private final Logger logger = LoggerFactory.getLogger(EchoProtocol.class);

  public EchoProtocol(EndPoint endPoint, Packet pck) throws IOException {
    super(endPoint, new LoRaProtocolConfigDTO());
    packet = pck;
    endPoint.sendPacket(pck);
    endPoint.register(SelectionKey.OP_READ, this);
  }

  public String getName() {
    return "ECHO";
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    try {
      endPoint.deregister(SelectionKey.OP_READ);
    } catch (ClosedChannelException e) {
      logger.log(ServerLogMessages.ECHO_EXCEPTION);
    }
    executor.execute(new Task());
  }

  @Override
  public String getSessionId() {
    return toString();
  }

  public String getVersion() {
    return "1.1";
  }

  // Yes this is duplicate, but for the sake of clarity in the classes it can stay
  @java.lang.SuppressWarnings("common-java:DuplicatedBlocks")
  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    // There is nothing to do
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    return true;
  }

  @Override
  public Subject getSubject() {
    return null;
  }

  @Override
  public void sendKeepAlive() {
    // There is no keep alive here
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    return null;
  }

  private class Task implements Runnable {

    @Override
    public void run() {
      Thread.currentThread().setName("Echo Protocol Thread pool::" + Thread.currentThread().getName());
      try {
        int read;
        boolean first = true;
        do {
          packet.clear();
          read = endPoint.readPacket(packet);
          if (read > 0) {
            EndPoint.totalReceived.increment();
            packet.flip();
            endPoint.sendPacket(packet);
            EndPoint.totalSent.increment();
          } else if (first) {
            endPoint.close();
            return;
          }
          first = false;
        } while (read > 0);
        endPoint.register(SelectionKey.OP_READ, EchoProtocol.this);
      } catch (Exception e) {
        logger.log(ServerLogMessages.ECHO_CLOSED);
        try {
          endPoint.close();
        } catch (IOException ioException) {
          logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
        }
      }
    }
  }
}
