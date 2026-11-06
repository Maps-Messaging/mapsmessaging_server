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

package io.mapsmessaging.network.protocol.impl.amqp;

import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.TransportResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import static org.apache.qpid.proton.engine.Event.Type.CONNECTION_REMOTE_OPEN;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmqpFragmentationTest {

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 5, 10, 100, 1024})
  void proton_accepts_fragmented_protocol_and_open_frame(int chunkSize) {
    byte[] encoded = createClientOpen();

    Collector collector = Collector.Factory.create();
    Connection connection = Connection.Factory.create();
    connection.collect(collector);
    Transport transport = Transport.Factory.create();
    transport.bind(connection);

    int offset = 0;
    while (offset < encoded.length) {
      ByteBuffer input = transport.getInputBuffer();
      int length = Math.min(Math.min(chunkSize, encoded.length - offset), input.remaining());
      input.put(encoded, offset, length);
      offset += length;

      TransportResult result = transport.processInput();
      assertTrue(result.isOk(), result.getErrorDescription());
    }

    boolean remoteOpen = false;
    for (Event event = collector.peek(); event != null; event = collector.peek()) {
      remoteOpen |= event.getType() == CONNECTION_REMOTE_OPEN;
      collector.pop();
    }
    assertTrue(remoteOpen, "Expected AMQP remote-open after fragmented input");
  }

  private byte[] createClientOpen() {
    Connection connection = Connection.Factory.create();
    connection.setContainer("fragmentation-test-client");
    Transport transport = Transport.Factory.create();
    transport.bind(connection);
    connection.open();

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    transport.process();
    while (transport.pending() > 0) {
      ByteBuffer buffer = transport.getOutputBuffer();
      byte[] data = new byte[buffer.remaining()];
      buffer.get(data);
      output.write(data, 0, data.length);
      transport.outputConsumed();
      transport.process();
    }
    return output.toByteArray();
  }
}
