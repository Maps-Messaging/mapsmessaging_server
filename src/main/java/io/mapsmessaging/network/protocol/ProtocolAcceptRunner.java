/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * This class parses incoming data in a packet and scans all the known protocols looking for a match Once it finds a match it creates the ProtocolImpl to handle the session based
 * on the protocol detected
 */
public class ProtocolAcceptRunner implements Selectable {

  private static final int BUFFER_SIZE = 1024;

  private final Logger logger;
  private final ProtocolFactory protocolFactory;
  private final EndPoint endPoint;
  private final Packet packet;


  /**
   * Created when we accept a new EndPoint but do not know the corresponding protocol being used by the client Will register a read selector on the EndPoint and then attempt to
   * determine the protocol being requested by the remote client
   *
   * @param endPoint the end point that the accept is being bound to
   * @param protocols a list of protocols to accept
   * @throws IOException if unable to register for incoming connections
   */
  public ProtocolAcceptRunner(EndPoint endPoint, String protocols) throws IOException {
    this.endPoint = endPoint;
    logger = LoggerFactory.getLogger(ProtocolAcceptRunner.class.getName());
    protocolFactory = new ProtocolFactory(protocols);
    packet = new Packet(BUFFER_SIZE, true);
    endPoint.register(SelectionKey.OP_READ, this);
    logger.log(ServerLogMessages.PROTOCOL_ACCEPT_REGISTER);
  }

  /**
   * Called by the SelectorImpl thread when it detects there is data to read from the end point
   */
  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    logger.log(ServerLogMessages.PROTOCOL_ACCEPT_SELECTOR_FIRED);
    try {
      logger.log(ServerLogMessages.PROTOCOL_ACCEPT_FIRING, packet.position(), packet.limit());
      int read = endPoint.readPacket(packet);
      int pos = packet.position();
      if (logger.isDebugEnabled()) {
        logger.log(ServerLogMessages.PROTOCOL_ACCEPT_FIRED, read, pos, packet.limit());
      }
      if (read > 0) {
        packet.flip();
        packet.position(0);
        logger.log(ServerLogMessages.PROTOCOL_ACCEPT_SCANNING, packet);
        ProtocolImplFactory protocolImplFactory = protocolFactory.detect(packet);
        if (protocolImplFactory != null) {
          endPoint.deregister(SelectionKey.OP_READ);
          packet.position(pos);
          packet.limit(packet.capacity());
          packet.flip();
          logger.log(ServerLogMessages.PROTOCOL_ACCEPT_CREATED, protocolImplFactory.getName());
          protocolImplFactory.create(endPoint, packet);
        } else {
          packet.position(pos);
          packet.limit(packet.capacity());
        }
      } else if (read < 0) {
        logger.log(ServerLogMessages.PROTOCOL_ACCEPT_CLOSED);
        endPoint.close();
      }
    } catch (IOException e) {
      logger.log(ServerLogMessages.PROTOCOL_ACCEPT_FAILED_DETECT, e, endPoint.toString());
      try {
        endPoint.close();
      } catch (IOException ioException) {
        logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, ioException);
      }
    }
  }
}
