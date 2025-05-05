package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import io.nats.client.api.DiscardPolicy;
import io.nats.client.api.StreamConfiguration;
import org.junit.jupiter.api.Test;

public class JetStreamPublishTest extends JetStreamBaseTest {

  @Test
  void testJetStreamPublish() throws Exception {
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

    jetStream.publish("topic1", "payload".getBytes());
    jetStream.getStreamContext("test1");
    Thread.sleep(5000);
  }

}