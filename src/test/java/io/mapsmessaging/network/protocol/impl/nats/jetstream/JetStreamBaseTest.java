/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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
import com.google.gson.JsonParser;
import io.mapsmessaging.test.BaseTestConfig;
import io.nats.client.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JetStreamBaseTest extends BaseTestConfig {

  protected Connection natsConnection;
  protected JetStream jetStream;
  protected JetStreamManagement jetStreamManagement;

  @BeforeEach
  void setupJetStream() throws Exception {
    Options options = new Options.Builder()
        .server("nats://localhost:4222")
        .connectionName("JetStreamTest")
        .requestCleanupInterval(Duration.ofSeconds(10))
        .connectionTimeout(Duration.ofSeconds(5))
        .noNoResponders()
        .build();

    natsConnection = Nats.connect(options);
    jetStream = natsConnection.jetStream();
    jetStreamManagement = natsConnection.jetStreamManagement();

    assertNotNull(natsConnection);
    assertNotNull(jetStream);
    assertNotNull(jetStreamManagement);
    assertTrue(natsConnection.getStatus() == Connection.Status.CONNECTED);
  }

  @AfterEach
  void teardownJetStream() throws Exception {
    if (natsConnection != null && natsConnection.getStatus() != Connection.Status.CLOSED) {
      natsConnection.close();
    }
  }


  protected JsonObject requestJetStreamInfo() throws Exception {
    natsConnection.flush(Duration.ofSeconds(1));
    Message msg = natsConnection.request("$JS.API.INFO", null, Duration.ofSeconds(4));
    if (msg == null || msg.getData() == null) return null;
    return JsonParser.parseString(new String(msg.getData(), StandardCharsets.UTF_8)).getAsJsonObject();
  }

}
