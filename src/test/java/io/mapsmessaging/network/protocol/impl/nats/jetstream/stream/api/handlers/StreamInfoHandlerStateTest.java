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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers;

import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.data.StreamInfoResponse;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StreamInfoHandlerStateTest {

  @Test
  void emptyStreamInfoContainsStandardStateObject() {
    StreamInfoList stream = new StreamInfoList("EVENTS");
    StreamInfoResponse response = new StreamInfoResponse();

    new StreamInfoHandler().packResponse(response, stream);

    assertEquals("EVENTS", response.getConfig().getName());
    assertNotNull(response.getState());
    assertEquals(0, response.getState().getMessages());
    assertEquals(0, response.getState().getBytes());
    assertEquals(0, response.getState().getFirst_seq());
    assertEquals(0, response.getState().getLast_seq());
    assertEquals(0, response.getState().getConsumer_count());
  }
}
