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

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.network.protocol.impl.amqp.proton.SaslManager;
import io.mapsmessaging.network.protocol.impl.amqp.proton.listeners.EventListenerFactory;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.TransportResult;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InputPacketProcessTaskTest {

  @Test
  void call_with_partially_occupied_transport_buffer_uses_remaining_capacity() throws Exception {
    ProtonEngine engine = mock(ProtonEngine.class);
    Transport transport = mock(Transport.class);
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    SaslManager saslManager = mock(SaslManager.class);
    Collector collector = mock(Collector.class);
    TransportResult result = mock(TransportResult.class);
    ByteBuffer first = ByteBuffer.allocate(4);
    first.position(2);
    ByteBuffer second = ByteBuffer.allocate(4);
    Packet incoming = new Packet(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}));

    when(engine.getTransport()).thenReturn(transport);
    when(engine.getProtocol()).thenReturn(protocol);
    when(engine.getCollector()).thenReturn(collector);
    when(engine.getSaslManager()).thenReturn(saslManager);
    when(engine.getConnection()).thenReturn(mock(Connection.class));
    when(engine.getEventListenerFactory()).thenReturn(mock(EventListenerFactory.class));
    when(transport.getInputBuffer()).thenReturn(first, second);
    when(transport.processInput()).thenReturn(result);
    when(result.isOk()).thenReturn(true);
    when(saslManager.isDone()).thenReturn(true);
    when(collector.peek()).thenReturn(null);
    when(transport.pending()).thenReturn(0);

    new InputPacketProcessTask(engine, incoming).call();

    assertEquals(1, first.get(2));
    assertEquals(2, first.get(3));
    assertEquals(3, second.get(0));
    assertEquals(4, second.get(1));
    verify(protocol).registerRead();
  }
}
