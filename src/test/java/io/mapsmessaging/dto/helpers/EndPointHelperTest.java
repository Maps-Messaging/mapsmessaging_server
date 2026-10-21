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

package io.mapsmessaging.dto.helpers;

import io.mapsmessaging.dto.rest.endpoint.EndPointDetailsDTO;
import io.mapsmessaging.dto.rest.endpoint.EndPointSummaryDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EndPointHelperTest {

  @Test
  void summary_without_bound_protocol_uses_not_detected_values() {
    EndPoint endPoint = endpoint(null);

    EndPointSummaryDTO summary = EndPointHelper.buildSummaryDTO("tcp", endPoint);

    assertEquals(7L, summary.getId());
    assertEquals("tcp", summary.getAdapter());
    assertEquals("endpoint-7", summary.getName());
    assertEquals("anonymous", summary.getUser());
    assertEquals("Not Detected", summary.getProtocolName());
    assertEquals("N/A", summary.getProtocolVersion());
    assertEquals("", summary.getProxyAddress());
    assertEquals(123L, summary.getLastRead());
    assertEquals(456L, summary.getLastWrite());
    assertEquals(1_024L, summary.getTotalBytesRead());
    assertEquals(2_048L, summary.getTotalBytesWritten());
  }

  @Test
  void summary_with_moving_averages_exposes_current_and_total_values() {
    EndPointStatus status = mock(EndPointStatus.class);
    LinkedMovingAverages read = movingAverage(100L, 1_000L);
    LinkedMovingAverages written = movingAverage(200L, 2_000L);
    LinkedMovingAverages overflow = movingAverage(3L, 30L);
    LinkedMovingAverages underflow = movingAverage(4L, 40L);
    when(status.supportsMovingAverages()).thenReturn(true);
    when(status.getReadByteAverages()).thenReturn(read);
    when(status.getWriteByteAverages()).thenReturn(written);
    when(status.getBufferOverFlow()).thenReturn(overflow);
    when(status.getBufferUnderFlow()).thenReturn(underflow);
    EndPoint endPoint = endpoint(null, status);

    EndPointSummaryDTO summary = EndPointHelper.buildSummaryDTO("tcp", endPoint);

    assertEquals(100L, summary.getBytesRead());
    assertEquals(200L, summary.getBytesWritten());
    assertEquals(3L, summary.getOverFlow());
    assertEquals(30L, summary.getTotalOverflow());
    assertEquals(4L, summary.getUnderFlow());
    assertEquals(40L, summary.getTotalUnderflow());
  }

  @Test
  void details_without_bound_protocol_returns_null_protocol_information() {
    EndPoint endPoint = endpoint(null);

    EndPointDetailsDTO details = EndPointHelper.buildDetailsDTO("tcp", endPoint);

    assertEquals("Not Detected", details.getEndPointSummary().getProtocolName());
    assertNull(details.getProtocolInformation());
  }

  @Test
  void details_with_bound_protocol_returns_protocol_information() {
    Protocol protocol = mock(Protocol.class);
    ProtocolInformationDTO information = new ProtocolInformationDTO();
    when(protocol.getName()).thenReturn("mqtt");
    when(protocol.getVersion()).thenReturn("5.0");
    when(protocol.getInformation()).thenReturn(information);
    EndPoint endPoint = endpoint(protocol);

    EndPointDetailsDTO details = EndPointHelper.buildDetailsDTO("tcp", endPoint);

    assertEquals("mqtt", details.getEndPointSummary().getProtocolName());
    assertEquals("5.0", details.getEndPointSummary().getProtocolVersion());
    assertSame(information, details.getProtocolInformation());
  }

  private EndPoint endpoint(Protocol protocol) {
    EndPointStatus status = mock(EndPointStatus.class);
    when(status.getReadBytesTotal()).thenReturn(1_024L);
    when(status.getWriteBytesTotal()).thenReturn(2_048L);
    when(status.supportsMovingAverages()).thenReturn(false);
    return endpoint(protocol, status);
  }

  private EndPoint endpoint(Protocol protocol, EndPointStatus status) {
    EndPoint endPoint = mock(EndPoint.class);
    when(endPoint.getProxyProtocolInfo()).thenReturn(null);
    when(endPoint.getId()).thenReturn(7L);
    when(endPoint.getName()).thenReturn("endpoint-7");
    when(endPoint.getConnected()).thenReturn(System.currentTimeMillis() - 1_000L);
    when(endPoint.getBoundProtocol()).thenReturn(protocol);
    when(endPoint.getEndPointSubject()).thenReturn(null);
    when(endPoint.getLastRead()).thenReturn(123L);
    when(endPoint.getLastWrite()).thenReturn(456L);
    when(endPoint.getEndPointStatus()).thenReturn(status);
    return endPoint;
  }

  private LinkedMovingAverages movingAverage(long current, long total) {
    LinkedMovingAverages movingAverage = mock(LinkedMovingAverages.class);
    when(movingAverage.getCurrent()).thenReturn(current);
    when(movingAverage.getTotal()).thenReturn(total);
    return movingAverage;
  }
}
