/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.engine.session;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.concurrent.FutureTask;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;

public class FakeEndPoint extends EndPoint {

  public FakeEndPoint(long id, EndPointServer server) {
    super(id, server);
    jmxParentPath = new ArrayList<>();
    jmxParentPath.add("endpoint=fakeEndPoint"+id);
  }

  @Override
  public String getProtocol() {
    return "Fake";
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
    return "FakeEndPoint";
  }

  @Override
  protected Logger createLogger() {
    return null;
  }
}
