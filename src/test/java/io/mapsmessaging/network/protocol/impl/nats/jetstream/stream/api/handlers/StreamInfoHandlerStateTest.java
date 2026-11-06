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
import io.mapsmessaging.dto.rest.stats.LinkedMovingAverageRecordDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.resources.ResourceProperties;
import io.mapsmessaging.engine.resources.ResourceStatistics;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StreamInfoHandlerStateTest {

  @Test
  void emptyStreamInfoContainsStandardStateObject() throws IOException {
    StreamInfoList stream = new StreamInfoList("EVENTS");
    StreamInfoResponse response = new StreamInfoResponse();

    new StreamInfoHandler().packResponse(response, stream);

    assertEquals("EVENTS", response.getConfig().getName());
    assertEquals("memory", response.getConfig().getStorage());
    assertNotNull(response.getState());
    assertEquals(0, response.getState().getMessages());
    assertEquals(0, response.getState().getBytes());
    assertEquals(0, response.getState().getFirst_seq());
    assertEquals(0, response.getState().getLast_seq());
    assertEquals(0, response.getState().getConsumer_count());
  }
  @Test
  void nonEmptyStreamInfoUsesCurrentStoredCountAndAvailableStorageSample() throws IOException {
    StreamInfoList stream = new StreamInfoList("ORDERS");
    DestinationImpl destination = mock(DestinationImpl.class);
    ResourceStatistics statistics = mock(ResourceStatistics.class);
    ResourceProperties properties = mock(ResourceProperties.class);
    when(destination.getStoredMessages()).thenReturn(2L);
    when(destination.getSubscriptionStates()).thenReturn(List.of());
    when(destination.getResourceStatistics()).thenReturn(statistics);
    when(destination.getResourceProperties()).thenReturn(properties);
    when(properties.getDate()).thenReturn(new Date(1_000L));
    when(destination.isPersistent()).thenReturn(true);
    when(statistics.getStatistics()).thenReturn(Map.of(
        "store", Map.of("Total Size",
            new LinkedMovingAverageRecordDTO("Total Size", "Bytes", 10_000L, 1024L, Map.of()))));
    stream.addSubject("orders", destination);

    StreamInfoResponse response = new StreamInfoResponse();
    new StreamInfoHandler().packResponse(response, stream);

    assertEquals(2L, response.getState().getMessages(),
        "The stream must report current stored messages, not a cumulative publish counter");
    assertEquals(1024L, response.getState().getBytes(),
        "Existing MAPS resource metrics provide a sampled physical byte count");
    assertEquals("file", response.getConfig().getStorage());
  }

  @Test
  void missingResourceStatisticsAreNotTreatedAsAnEngineApi() throws IOException {
    StreamInfoList stream = new StreamInfoList("ORDERS");
    DestinationImpl destination = mock(DestinationImpl.class);
    ResourceProperties properties = mock(ResourceProperties.class);
    when(destination.getStoredMessages()).thenReturn(1L);
    when(destination.getSubscriptionStates()).thenReturn(List.of());
    when(destination.getResourceProperties()).thenReturn(properties);
    stream.addSubject("orders", destination);

    StreamInfoResponse response = new StreamInfoResponse();
    new StreamInfoHandler().packResponse(response, stream);

    assertEquals(1L, response.getState().getMessages());
    assertEquals(0L, response.getState().getBytes(),
        "No resource sample is exposed; exact JetStream logical byte accounting remains unresolved");
  }

}
