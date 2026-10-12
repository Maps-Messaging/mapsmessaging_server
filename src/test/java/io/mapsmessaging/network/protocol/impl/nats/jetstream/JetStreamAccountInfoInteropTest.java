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

package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JetStreamAccountInfoInteropTest extends JetStreamBaseTest {

  @Test
  void officialClientReceivesJetStreamAccountUsageAndLimits() throws Exception {
    JsonObject info = requestJetStreamInfo();

    assertNotNull(info);
    assertEquals("io.nats.jetstream.api.v1.account_info_response", info.get("type").getAsString());
    assertTrue(info.has("memory"));
    assertTrue(info.has("storage"));
    assertTrue(info.has("streams"));
    assertTrue(info.has("consumers"));
    assertTrue(info.has("limits"));
    assertTrue(info.getAsJsonObject("limits").has("max_memory"));
    assertTrue(info.getAsJsonObject("limits").has("max_consumers"));
    assertTrue(info.getAsJsonObject("api").has("total"));
    assertFalse(info.has("server_id"), "JetStream account INFO is not the Core NATS server INFO frame");
  }
}
