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

package io.mapsmessaging.network.protocol.impl.amqp.proton.tasks;


import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.network.protocol.impl.amqp.proton.SaslManager;
import io.mapsmessaging.network.protocol.impl.amqp.proton.listeners.EventListenerFactory;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.TransportResult;

import java.io.IOException;
import java.nio.ByteBuffer;

public class InputPacketProcessTask extends PacketTask {

  private final Collector collector;
  private final EventListenerFactory eventListenerFactory;
  private final Packet incomingPacket;
  private final SaslManager saslManager;
  private final Connection connection;

  public InputPacketProcessTask(ProtonEngine engine, Packet packet) {
    super(engine);
    this.incomingPacket = packet;
    collector = engine.getCollector();
    saslManager = engine.getSaslManager();
    connection = engine.getConnection();
    eventListenerFactory = engine.getEventListenerFactory();
  }

  @Override
  public Boolean call() throws Exception {
    pushDataIntoEngine();
    protocol.registerRead();
    return true;
  }

  private void processBuffers() throws IOException {
    ByteBuffer buffer = transport.getInputBuffer();
    int length = Math.min(buffer.remaining(), incomingPacket.available());
    if (length <= 0) {
      throw new IOException("AMQP transport input buffer has no remaining capacity");
    }
    ByteBuffer source = incomingPacket.getRawBuffer();
    int originalLimit = source.limit();
    source.limit(source.position() + length);
    buffer.put(source);
    source.limit(originalLimit);
  }

  private void handleEvents() {
    for (Event ev = collector.peek(); ev != null; ev = collector.peek()) {
      eventListenerFactory.handleEvent(ev);
      collector.pop();
    }
  }


  private void pushDataIntoEngine() throws IOException {
    while (incomingPacket.hasRemaining()) {
      processBuffers();
      TransportResult result = transport.processInput();
      if (!result.isOk()) {
        if (result.getException() != null) {
          protocol.getLogger().log(ServerLogMessages.AMQP_ENGINE_TRANSPORT_EXCEPTION, result.getErrorDescription(), result.getException());
          throw new IOException(result.getErrorDescription(), result.getException());
        }
        throw new IOException(result.getErrorDescription());
      } else {
        isInSasl();
        handleEvents();
      }
    }
    processOutput();
    if (!incomingPacket.hasRemaining()) {
      incomingPacket.clear();
    }
  }


  private void isInSasl() throws IOException {
    if (!saslManager.isDone()) {
      saslManager.challenge();
      if (saslManager.isDone()) {
        transport.bind(connection);
      }
    }
  }
}
