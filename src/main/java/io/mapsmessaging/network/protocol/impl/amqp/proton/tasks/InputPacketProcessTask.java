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


import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.network.protocol.impl.amqp.proton.listeners.EventListenerFactory;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.TransportResult;

import java.io.IOException;
import java.nio.ByteBuffer;

public class InputPacketProcessTask extends PacketTask {

  private final Collector collector;
  private final EventListenerFactory eventListenerFactory;
  private final Packet incomingPacket;

  public InputPacketProcessTask(ProtonEngine engine, Packet packet) {
    super(engine);
    this.incomingPacket = packet;
    collector = engine.getCollector();
    eventListenerFactory = engine.getEventListenerFactory();
  }

  @Override
  public Boolean call() throws Exception {
    pushDataIntoEngine();
    protocol.registerRead();
    return true;
  }

  private void processBuffers() {
    ByteBuffer buffer = transport.getInputBuffer();
    if (buffer.capacity() < incomingPacket.available()) {
      // Seems the buffer.put(ByteBuffer) will not just take what it can
      byte[] tmp = new byte[buffer.capacity()];
      incomingPacket.get(tmp);
      buffer.put(tmp);
    } else {
      buffer.put(incomingPacket.getRawBuffer());
    }
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
        }
      } else {
        handleEvents();
      }
    }
    processOutput();
    if (!incomingPacket.hasRemaining()) {
      incomingPacket.clear();
    }
  }


  private boolean isInSasl() {
    return false;
  }
/*
  private boolean isInSasl() throws IOException {
    if (saslContext == null || saslContext.complete()) return false;

    if (initialChallenge || (!sasl.getState().equals(Sasl.SaslState.PN_SASL_PASS) && sasl.pending() > 0)) {
      initialChallenge = false;
      int pending = Math.max(0, sasl.pending());
      byte[] challenge;
      if (pending > 0) {
        challenge = new byte[pending];
        sasl.recv(challenge, 0, challenge.length);
      } else {
        challenge = new byte[0];
      }
      byte[] response = saslContext.challenge(challenge);
      if (response != null) {
        sasl.send(response, 0, response.length);
      }
    }

    if (sasl.getState() != Sasl.SaslState.PN_SASL_PASS) {
      return true;
    }

    sasl.done(Sasl.PN_SASL_OK);
    transport.bind(connection);
    return false;
  }
  */
}