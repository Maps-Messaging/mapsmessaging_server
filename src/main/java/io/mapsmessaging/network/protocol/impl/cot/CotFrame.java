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

package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.Protocol;

import java.net.SocketAddress;

public class CotFrame implements ServerPacket {
  private final byte[] encoded;
  private final Runnable completionTask;
  private boolean completed;
  private final Protocol protocol;

  public CotFrame(byte[] encoded, Runnable completionTask, Protocol protocol) {
    this.encoded = encoded;
    this.completionTask = completionTask;
    this.protocol = protocol;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put(encoded);
    return encoded.length;
  }

  @Override
  public synchronized void complete() {
    if (completed) {
      return;
    }
    completed = true;
    protocol.sentMessage();
    if (completionTask != null) {
      completionTask.run();
    }
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}