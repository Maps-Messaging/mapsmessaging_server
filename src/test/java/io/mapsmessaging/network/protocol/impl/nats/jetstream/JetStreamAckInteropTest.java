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

import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.api.StreamConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JetStreamAckInteropTest extends JetStreamBaseTest {

  @Test
  void officialClientReceivesStreamMetadataAndConfirmsAcknowledgement() throws Exception {
    String suffix = UUID.randomUUID().toString().replace("-", "");
    String stream = "ACK_" + suffix;
    String subject = "ack." + suffix;
    jetStreamManagement.addStream(StreamConfiguration.builder().name(stream).addSubjects(subject).build());
    try {
      PullSubscribeOptions options = PullSubscribeOptions.builder().stream(stream).build();
      JetStreamSubscription consumer = jetStream.subscribe(null, options);
      jetStream.publish(subject, "payload".getBytes(StandardCharsets.UTF_8));
      consumer.pull(1);
      List<Message> messages = consumer.fetch(1, Duration.ofSeconds(4));
      assertEquals(1, messages.size());
      Message message = messages.get(0);
      assertEquals("payload", new String(message.getData(), StandardCharsets.UTF_8));
      assertNotNull(message.metaData(), "ACK reply subject must expose valid standard JetStream metadata");
      message.ackSync(Duration.ofSeconds(4));
    } finally {
      jetStreamManagement.deleteStream(stream);
    }
  }
}
