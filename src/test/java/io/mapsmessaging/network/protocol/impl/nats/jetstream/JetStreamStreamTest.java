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
  void testJetStreamUpdate() throws Exception {
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
    StreamConfiguration updateStreamConfiguration = StreamConfiguration.builder()
        .name("nats_test")
        .addSubjects("folder1.topic1", "folder.folder1.folder2.topic1")
        .description("test stream")
        .allowMessageTtl(true)
        .discardPolicy(DiscardPolicy.Old)
        .build();
    jetStreamManagement.updateStream(updateStreamConfiguration);
    List<StreamInfo> f = jetStreamManagement.getStreams();
    Assertions.assertNotNull(f);
    Assertions.assertFalse(f.isEmpty());
    for(StreamInfo si : f) {
      for(String subject: si.getConfig().getSubjects()) {
        Assertions.assertNotEquals("topic1", subject);
        Assertions.assertNotEquals("topic2", subject);
        Assertions.assertTrue(subject.equals("folder1.topic1") || subject.equals("folder.folder1.folder2.topic1"));
      }
    }
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