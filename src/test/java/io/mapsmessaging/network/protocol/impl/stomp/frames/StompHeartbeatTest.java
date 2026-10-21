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

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import org.junit.jupiter.api.Test;

class StompHeartbeatTest {

  @Test
  void parsesTwoNonNegativeHeartbeatValues() throws Exception {
    HeartBeat heartBeat = new HeartBeat("10000, 30000");

    assertEquals(10000, heartBeat.getCanSend());
    assertEquals(30000, heartBeat.getWantsReceive());
    assertEquals("10000,30000", heartBeat.toString());
  }

  @Test
  void rejectsMalformedHeartbeatValues() {
    assertThrows(StompProtocolException.class, () -> new HeartBeat("10000"));
    assertThrows(StompProtocolException.class, () -> new HeartBeat("10000,nope"));
    assertThrows(StompProtocolException.class, () -> new HeartBeat("-1,10000"));
  }

  @Test
  void negotiatesEachDirectionIndependently() {
    HeartBeat server = new HeartBeat(10000, 20000);
    HeartBeat client = new HeartBeat(30000, 40000);

    long serverToClient =
        HeartBeat.negotiate(server.getCanSend(), client.getWantsReceive());
    long clientToServer =
        HeartBeat.negotiate(client.getCanSend(), server.getWantsReceive());

    assertEquals(40000, serverToClient);
    assertEquals(30000, clientToServer);
  }

  @Test
  void zeroInEitherSideDisablesThatDirection() {
    assertEquals(0, HeartBeat.negotiate(0, 10000));
    assertEquals(0, HeartBeat.negotiate(10000, 0));
  }
}
