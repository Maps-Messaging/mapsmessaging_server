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

package io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncomingMessageDetailsTest {

  @Test
  void ogxStatusLineMapsFieldsAndCompletionState() {
    IncomingMessageDetails details =
        new IncomingMessageDetails("1,msg-42,2026-09-19T20:00:00Z,5,1,128", true);

    assertEquals("msg-42", details.getId());
    assertTrue(Float.isNaN(details.getMessageId()));
    assertEquals(-1, details.getSin());
    assertEquals(-1, details.getMin());
    assertEquals("2026-09-19T20:00:00Z", details.getDateTime());
    assertEquals(5, details.getState());
    assertTrue(details.isClosed());
    assertEquals(128, details.getBytes());
    assertEquals(128, details.getBytesReceived());
    assertTrue(details.isCompleted());
  }

  @Test
  void ogxNonTerminalStateIsNotCompleted() {
    IncomingMessageDetails details =
        new IncomingMessageDetails("2,msg-43,2026-09-19T20:00:00Z,2,0,64", true);

    assertFalse(details.isClosed());
    assertFalse(details.isCompleted());
  }

  @Test
  void legacyStatusLineMapsMessageMetadata() {
    IncomingMessageDetails details =
        new IncomingMessageDetails("msg-9,12.5,3,44,3,100,80", false);

    assertEquals("msg-9", details.getId());
    assertEquals(12.5f, details.getMessageId(), 0.0f);
    assertEquals(3, details.getPriority());
    assertEquals(44, details.getSin());
    assertEquals(3, details.getState());
    assertEquals(100, details.getBytes());
    assertEquals(80, details.getBytesReceived());
    assertEquals("", details.getDateTime());
    assertTrue(details.isCompleted());
  }

  @Test
  void legacyNonTerminalStateIsNotCompleted() {
    assertFalse(new IncomingMessageDetails("id,1,0,1,2,5,3", false).isCompleted());
  }
}
