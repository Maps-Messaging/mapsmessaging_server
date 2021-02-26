/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package org.maps.network.io.impl.noop;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.network.io.EndPoint;
import org.maps.network.io.EndPointServerStatus;
import org.maps.network.io.Packet;
import org.maps.network.io.Selectable;

public class NoOpEndPoint extends EndPoint {

  public NoOpEndPoint(long id, EndPointServerStatus server, List<String> jmxPath) {
    super(id, server);
    jmxParentPath = new ArrayList<>(jmxPath);
    logger.log(LogMessages.NOOP_END_POINT_CREATE, id);
  }

  @Override
  public void close() throws IOException {
    super.close();
    logger.log(LogMessages.NOOP_END_POINT_CLOSE, getId());
  }

  @Override
  public String getProtocol() {
    return "NoOp";
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    return 0;
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    return 0;
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    return null;
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) throws ClosedChannelException {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return null;
  }

  @Override
  public String getName() {
    return "NoOp";
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(NoOpEndPoint.class);
  }
}
