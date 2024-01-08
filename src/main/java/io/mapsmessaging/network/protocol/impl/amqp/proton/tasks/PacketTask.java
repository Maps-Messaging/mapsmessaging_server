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

package io.mapsmessaging.network.protocol.impl.amqp.proton.tasks;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.engine.Transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

public abstract class PacketTask implements Callable<Boolean> {
  protected final AMQPProtocol protocol;
  protected final Transport transport;

  PacketTask(ProtonEngine engine) {
    transport = engine.getTransport();
    protocol = engine.getProtocol();
  }

  protected void processOutput() throws IOException {
    transport.process();
    while (transport.pending() > 0) {
      ByteBuffer buffer = transport.getOutputBuffer();
      Packet packet = new Packet(buffer);
      while (buffer.hasRemaining()) {
        protocol.getEndPoint().sendPacket(packet);
      }
      transport.outputConsumed();
      transport.process();
    }
  }

}
