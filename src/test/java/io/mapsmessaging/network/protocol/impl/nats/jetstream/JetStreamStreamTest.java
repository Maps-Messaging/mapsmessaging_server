package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import io.nats.client.StreamContext;
import io.nats.client.api.DiscardPolicy;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class JetStreamStreamTest extends JetStreamBaseTest {

  @Test
  void testJetStreamCreate() throws Exception {
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
    StreamContext context = jetStream.getStreamContext("nats_test");
    Assertions.assertNotNull(context);
    Assertions.assertEquals("nats_test", context.getStreamInfo().getConfig().getName());
  }

  @Test
  void testJetStreamInfo() throws Exception {
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
    StreamInfo streamInfo = jetStreamManagement.getStreamInfo("nats_test");
    Assertions.assertNotNull(streamInfo);
    Assertions.assertEquals("nats_test", streamInfo.getConfig().getName());
    List<StreamInfo> f = jetStreamManagement.getStreams();
    Assertions.assertNotNull(f);
    Assertions.assertFalse(f.isEmpty());

  }

  @Test
  void testJetStreamDelete() throws Exception {
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
    StreamInfo streamInfo = jetStreamManagement.getStreamInfo("nats_test");
    Assertions.assertNotNull(streamInfo);
    Assertions.assertEquals("nats_test", streamInfo.getConfig().getName());
    Assertions.assertTrue(jetStreamManagement.deleteStream("nats_test"));
    List<StreamInfo> f = jetStreamManagement.getStreams();
    Assertions.assertNotNull(f);
    Assertions.assertTrue(f.isEmpty());
  }
}