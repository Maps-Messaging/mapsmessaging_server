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

package io.mapsmessaging.network.io.impl.dtls.state;

import io.mapsmessaging.network.io.Packet;

import javax.net.ssl.SSLEngineResult;
import java.io.IOException;

public abstract class State {

  protected final StateEngine stateEngine;

  protected State(StateEngine stateEngine) {
    this.stateEngine = stateEngine;
  }

  abstract int outbound(Packet packet) throws IOException;

  abstract int inbound(Packet packet) throws IOException;

  void runDelegatedTasks() throws IOException {
    Runnable runnable;
    while ((runnable = stateEngine.getSslEngine().getDelegatedTask()) != null) {
      runnable.run();
    }
    SSLEngineResult.HandshakeStatus hs = stateEngine.getSslEngine().getHandshakeStatus();
    if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
      throw new IOException("handshake shouldn't need additional tasks");
    }
  }


}
