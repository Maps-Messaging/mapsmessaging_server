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

package io.mapsmessaging.network.protocol.impl.nats.state;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocol;
import io.mapsmessaging.network.protocol.impl.nats.frames.HMsgFrame;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NatsHeaderStringContractTest {
  @Test
  void header_values_keep_object_conversion_order_and_crlf() {
    NatsProtocol protocol = mock(NatsProtocol.class);
    when(protocol.getEndPoint()).thenReturn(mock(EndPoint.class));
    when(protocol.getMaxReceiveSize()).thenReturn(4096);
    SessionState state = new SessionState(protocol);
    state.setHeaders(true);
    Message message = mock(Message.class);
    LinkedHashMap<String, TypedData> data = new LinkedHashMap<>();
    data.put("sensor name", new TypedData("µ"));
    data.put("count", new TypedData(7));
    data.put("active", new TypedData(true));
    when(message.getDataMap()).thenReturn(data);
    when(message.getOpaqueData()).thenReturn(new byte[]{1, 2});
    HMsgFrame frame = assertInstanceOf(HMsgFrame.class, state.buildPayloadFrame(message, "sensor/+/#"));
    assertArrayEquals("NATS/1.0\r\nsensor_name: µ\r\ncount: 7\r\nactive: true\r\n\r\n".getBytes(StandardCharsets.UTF_8), frame.getHeaderBytes());
    assertEquals("sensor.*.>", frame.getSubject());
    assertArrayEquals(new byte[]{1, 2}, frame.getPayload());
  }
}
