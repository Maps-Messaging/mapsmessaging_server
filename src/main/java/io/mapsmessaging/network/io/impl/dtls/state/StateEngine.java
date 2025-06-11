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
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.dtls.DTLSSessionManager;
import lombok.Getter;
import lombok.Setter;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class StateEngine {


  private final DTLSSessionManager manager;
  private final Queue<Packet> inboundQueue;

  @Getter
  private final SSLEngine sslEngine;
  @Getter
  private final SocketAddress clientId;
  @Getter
  private long lastAccess;

  @Getter
  @Setter
  private Selectable selectableTask;
  @Getter
  @Setter
  private Selectable writeTask;
  @Getter
  @Setter
  private State currentState;
  @Getter
  @Setter
  private StateChangeListener listener;


  public StateEngine(SocketAddress clientId, SSLEngine engine, DTLSSessionManager manager) {
    this.sslEngine = engine;
    this.manager = manager;
    this.clientId = clientId;
    inboundQueue = new ConcurrentLinkedQueue<>();
    currentState = new HandShakeState(this);
    lastAccess = System.currentTimeMillis();
  }

  public void start() throws SSLException {
    sslEngine.setUseClientMode(false);
    sslEngine.beginHandshake();
  }

  public int toNetwork(Packet packet) throws IOException {
    lastAccess = System.currentTimeMillis();
    return currentState.outbound(packet);
  }

  public int fromNetwork(Packet packet) throws IOException {
    lastAccess = System.currentTimeMillis();
    return currentState.inbound(packet);
  }

  public int send(Packet packet) throws IOException {
    lastAccess = System.currentTimeMillis();
    return manager.sendPacket(packet);
  }

  public int read(Packet packet) {
    Packet in = inboundQueue.poll();
    if (in != null) {
      packet.put(in);
      packet.setFromAddress(in.getFromAddress());
      return in.position();
    }
    return 0;
  }

  void pushToInBoundQueue(Packet packet) {
    if (packet.hasRemaining()) {
      inboundQueue.add(packet);
      if (selectableTask != null) {
        selectableTask.selected(selectableTask, null, SelectionKey.OP_READ);
      }
    }
  }

  void handshakeComplete() {
    if (listener != null) {
      listener.handshakeComplete();
    }
  }
}
