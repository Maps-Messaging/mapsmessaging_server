package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import io.nats.client.api.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

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
    Pattern pattern = Pattern.compile("^_Ephemeral-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    assertTrue(pattern.matcher(consumerInfo.getName()).matches(), "Invalid ephemeral consumer name format");
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
    Pattern pattern = Pattern.compile("^_Ephemeral-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    assertTrue(pattern.matcher(consumerInfo.getName()).matches(), "Invalid ephemeral consumer name format");

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
    jetStreamManagement.addStream(streamConfiguration);

    ConsumerConfiguration.Builder configBuilder = ConsumerConfiguration.builder();
    configBuilder.ackPolicy(AckPolicy.None)
        .deliverSubject("special2");
    ConsumerInfo consumerInfo = jetStreamManagement.createConsumer("nats_test", configBuilder.build());
    Assertions.assertNotNull(consumerInfo);
    Assertions.assertEquals("nats_test", consumerInfo.getStreamName());
    Pattern pattern = Pattern.compile("^_Ephemeral-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    assertTrue(pattern.matcher(consumerInfo.getName()).matches(), "Invalid ephemeral consumer name format");
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

}