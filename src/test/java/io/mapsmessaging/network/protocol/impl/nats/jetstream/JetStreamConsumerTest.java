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

import io.nats.client.*;
import io.nats.client.api.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JetStreamConsumerTest extends JetStreamBaseTest {

  @Test
  void testJetStreamCreateConsumer() throws Exception {
    StreamConfiguration streamConfiguration = StreamConfiguration.builder()
        .name("nats_test")
        .addSubjects("topic1", "topic2", "folder1.topic1")
        .denyDelete(false)
        .denyPurge(false)
        .description("test stream")
        .allowMessageTtl(true)
        .discardPolicy(DiscardPolicy.Old)
        .build();
    jetStreamManagement.addStream(streamConfiguration);
    ConsumerConfiguration.Builder configBuilder = ConsumerConfiguration.builder();
    configBuilder.ackPolicy(AckPolicy.None)
          .deliverSubject("special");

    ConsumerInfo consumerInfo = jetStreamManagement.createConsumer("nats_test", configBuilder.build());
    Assertions.assertNotNull(consumerInfo);
    Assertions.assertEquals("nats_test", consumerInfo.getStreamName());
  }

  @Test
  void testJetStreamCreateDeleteConsumer() throws Exception {
    StreamConfiguration streamConfiguration = StreamConfiguration.builder()
        .name("nats_test")
        .addSubjects("topic1", "topic2", "folder1.topic1")
        .denyDelete(false)
        .denyPurge(false)
        .description("test stream")
        .allowMessageTtl(true)
        .discardPolicy(DiscardPolicy.Old)
        .build();
    jetStreamManagement.addStream(streamConfiguration);

    ConsumerConfiguration.Builder configBuilder = ConsumerConfiguration.builder();
    configBuilder.ackPolicy(AckPolicy.None)
        .deliverSubject("special2");
    ConsumerInfo consumerInfo = jetStreamManagement.createConsumer("nats_test", configBuilder.build());
    Assertions.assertNotNull(consumerInfo);
    Assertions.assertEquals("nats_test", consumerInfo.getStreamName());
    StreamInfo streamInfo = jetStreamManagement.getStreamInfo(consumerInfo.getStreamName());
    Assertions.assertNotNull(streamInfo);

    Assertions.assertTrue(jetStreamManagement.deleteStream(consumerInfo.getStreamName()));
    Assertions.assertThrowsExactly(io.nats.client.JetStreamApiException.class, () -> jetStreamManagement.getStreamInfo(consumerInfo.getStreamName()));
  }


  @Test
  void testJetStreamCreateListDeleteConsumer() throws Exception {
    StreamConfiguration streamConfiguration = StreamConfiguration.builder()
        .name("nats_test")
        .addSubjects("topic1", "topic2", "folder1.topic1")
        .denyDelete(false)
        .denyPurge(false)
        .description("test stream")
        .allowMessageTtl(true)
        .discardPolicy(DiscardPolicy.Old)
        .build();
    StreamInfo  streamInfo = jetStreamManagement.addStream(streamConfiguration);
    Assertions.assertNotNull(streamInfo);
    Assertions.assertEquals(3, streamInfo.getConfig().getSubjects().size());

    ConsumerConfiguration.Builder configBuilder = ConsumerConfiguration.builder();
    configBuilder.ackPolicy(AckPolicy.None)
        .deliverSubject("special2");
    ConsumerInfo consumerInfo = jetStreamManagement.createConsumer("nats_test", configBuilder.build());
    Assertions.assertNotNull(consumerInfo);
    Assertions.assertEquals("nats_test", consumerInfo.getStreamName());
    String consumerName = consumerInfo.getName();

    consumerInfo = jetStreamManagement.getConsumerInfo("nats_test", consumerInfo.getName());
    Assertions.assertNotNull(consumerInfo);
    Assertions.assertEquals("nats_test", consumerInfo.getStreamName());

    List<String> names = jetStreamManagement.getConsumerNames("nats_test");
    Assertions.assertNotNull(names);
    Assertions.assertTrue(names.contains(consumerInfo.getName()));

    Assertions.assertTrue(jetStreamManagement.deleteConsumer(consumerInfo.getStreamName(), consumerInfo.getName()));
    Assertions.assertThrowsExactly(io.nats.client.JetStreamApiException.class, () -> jetStreamManagement.getConsumerInfo("nats_test", consumerName));

    names = jetStreamManagement.getConsumerNames(consumerInfo.getName());
    Assertions.assertTrue(names.isEmpty());

  }


  @Test
  void testMapsStyleEphemeralPullConsumer() throws Exception {
    // Create the stream
    StreamConfiguration streamConfiguration = StreamConfiguration.builder()
        .name("nats_test")
        .addSubjects("topic1", "topic2", "folder1.topic1")
        .description("test stream")
        .discardPolicy(DiscardPolicy.Old)
        .allowMessageTtl(true)
        .build();
    jetStreamManagement.addStream(streamConfiguration);

    // Create ephemeral pull consumer (by subscribing BEFORE publishing)
    PullSubscribeOptions pullOpts = PullSubscribeOptions.builder()
        .stream("nats_test")
        .build();

    JetStreamSubscription sub = jetStream.subscribe(null, pullOpts);

    // Now publish 10 messages — they'll be retained because the consumer exists
    for (int x = 0; x < 10; x++) {
      natsConnection.publish("folder1.topic1", ("payload_" + x).getBytes());
    }

    // Pull and fetch messages
    sub.pull(PullRequestOptions.builder(10).expiresIn(Duration.ofSeconds(2)).build());
    List<Message> msgs = sub.fetch(10, Duration.ofSeconds(2));
    Assertions.assertNotNull(msgs);
    Assertions.assertEquals(10, msgs.size());

    Set<String> payloads = msgs.stream()
        .map(m -> new String(m.getData()))
        .collect(Collectors.toSet());

    for (Message msg : msgs) {
      msg.ack();
    }

    for (int x = 0; x < 10; x++) {
      Assertions.assertTrue(payloads.contains("payload_" + x), "Missing payload_" + x);
    }

    // Cleanup: find and delete ephemeral consumer
    List<String> consumers = jetStreamManagement.getConsumerNames("nats_test");

    for(String consumer : consumers) {
      boolean deleted = jetStreamManagement.deleteConsumer("nats_test", consumer);
      Assertions.assertTrue(deleted);

      Assertions.assertThrowsExactly(JetStreamApiException.class,
          () -> jetStreamManagement.getConsumerInfo("nats_test", consumer));
    }
  }


}