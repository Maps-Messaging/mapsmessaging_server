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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.qpid.proton.engine.Transport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PacketTaskTest {

  @Test
  void process_output_chunks_data_into_selector_writer_queue() throws Exception {
    ProtonEngine engine = mock(ProtonEngine.class);
    Transport transport = mock(Transport.class);
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    when(engine.getTransport()).thenReturn(transport);
    when(engine.getProtocol()).thenReturn(protocol);
    when(protocol.getOutputChunkSize()).thenReturn(2);
    when(transport.pending()).thenReturn(4, 0);
    when(transport.getOutputBuffer()).thenReturn(ByteBuffer.wrap(new byte[] {1, 2, 3, 4}));
    TestPacketTask task = new TestPacketTask(engine);

    task.processOutput();

    ArgumentCaptor<byte[]> output = ArgumentCaptor.forClass(byte[].class);
    verify(protocol, times(2)).queueOutput(output.capture());
    List<byte[]> chunks = output.getAllValues();
    assertArrayEquals(new byte[] {1, 2}, chunks.get(0));
    assertArrayEquals(new byte[] {3, 4}, chunks.get(1));
    verify(transport).outputConsumed();
  }

  private static final class TestPacketTask extends PacketTask {

    private TestPacketTask(ProtonEngine engine) {
      super(engine);
    }

    @Override
    public Boolean call() {
      return true;
    }
  }
}
