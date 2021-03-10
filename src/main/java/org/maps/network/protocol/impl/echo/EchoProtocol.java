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

package org.maps.network.protocol.impl.echo;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.api.message.Message;
import org.maps.network.io.EndPoint;
import org.maps.network.io.Packet;
import org.maps.network.io.Selectable;
import org.maps.network.io.impl.Selector;

public class EchoProtocol extends org.maps.network.protocol.ProtocolImpl
    implements Selectable {

  private final Executor executor = Executors.newFixedThreadPool(10);
  private final Packet packet;
  private final Logger logger = LoggerFactory.getLogger(EchoProtocol.class);

  public EchoProtocol(EndPoint endPoint, Packet pck) throws IOException {
    super(endPoint);
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
      logger.log(LogMessages.ECHO_EXCEPTION);
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
  public void sendMessage(@NonNull @NotNull Destination destination, @NonNull @NotNull String normalisedName, @NonNull @NotNull SubscribedEventManager subscription, @NonNull @NotNull Message message, @NonNull @NotNull Runnable completionTask) {
    // There is nothing to do
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    return true;
  }

  @Override
  public void sendKeepAlive() {
    // There is no keep alive here
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
            getReceivedMessages().increment();
            packet.flip();
            endPoint.sendPacket(packet);
            getSentMessages().increment();
          } else if (first) {
            endPoint.close();
            return;
          }
          first = false;
        } while (read > 0);
        endPoint.register(SelectionKey.OP_READ, EchoProtocol.this);
      } catch (Exception e) {
        logger.log(LogMessages.ECHO_CLOSED);
        try {
          endPoint.close();
        } catch (IOException ioException) {
          logger.log(LogMessages.END_POINT_CLOSE_EXCEPTION, e);
        }
      }
    }
  }
}
