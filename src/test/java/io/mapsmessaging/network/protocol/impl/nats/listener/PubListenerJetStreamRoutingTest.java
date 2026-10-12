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

package io.mapsmessaging.network.protocol.impl.nats.listener;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.dto.rest.config.protocol.impl.NatsConfigDTO;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocol;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PubFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import org.mockito.ArgumentCaptor;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PubListenerJetStreamRoutingTest {

  @Test
  void corePublishDeliversToCoreAndConfiguredStreamDestinations() throws Exception {
    SessionState state = setup(true);
    Session session = state.getSession();
    Destination core = mock(Destination.class);
    Destination stream = mock(Destination.class);
    when(session.findDestination("ack/order", DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(core));
    when(session.findDestination("ACK/ack/order", DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(stream));

    RecordingListener listener = new RecordingListener(List.of("ACK/ack/order"));
    listener.frameEvent(publish("ack.order"), state, true);

    assertEquals(List.of(core, stream), listener.delivered);
    verify(session).findDestination("ack/order", DestinationType.TOPIC);
    verify(session).findDestination("ACK/ack/order", DestinationType.TOPIC);
  }

  @Test
  void disablingStreamsPreservesCoreOnlyPublishing() throws Exception {
    SessionState state = setup(false);
    Destination core = mock(Destination.class);
    when(state.getSession().findDestination("ack/order", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(core));

    RecordingListener listener = new RecordingListener(List.of("ACK/ack/order"));
    listener.frameEvent(publish("ack.order"), state, true);

    assertEquals(List.of(core), listener.delivered);
    verify(state.getSession(), never()).findDestination("ACK/ack/order", DestinationType.TOPIC);
    assertEquals(0, listener.streamLookups);
  }

  @Test
  void duplicateInternalTargetIsStoredOnlyOnce() throws Exception {
    SessionState state = setup(true);
    Destination core = mock(Destination.class);
    when(state.getSession().findDestination("ack/order", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(core));

    RecordingListener listener = new RecordingListener(List.of("ack/order", "ack/order"));
    listener.frameEvent(publish("ack.order"), state, true);

    assertEquals(List.of(core), listener.delivered);
    verify(state.getSession(), times(1)).findDestination("ack/order", DestinationType.TOPIC);
  }

  @Test
  void ordinaryCoreNatsPublishKeepsExistingStorageContract() throws Exception {
    Destination destination = mock(Destination.class);
    io.mapsmessaging.api.message.Message message = mock(io.mapsmessaging.api.message.Message.class);

    long sequence = new PubListener().storeToMapDestination(destination, message, false);

    assertEquals(0L, sequence);
    verify(destination).storeMessage(message);
  }

  private static SessionState setup(boolean streamsEnabled) {
    SessionState state = mock(SessionState.class);
    NatsProtocol protocol = mock(NatsProtocol.class);
    NatsConfigDTO config = mock(NatsConfigDTO.class);
    Session session = mock(Session.class);
    when(state.getProtocol()).thenReturn(protocol);
    when(protocol.getNatsConfig()).thenReturn(config);
    when(config.isEnableStreams()).thenReturn(streamsEnabled);
    when(state.getSession()).thenReturn(session);
    when(state.getJetStreamRequestManager()).thenReturn(mock(JetStreamRequestManager.class));
    when(state.getMapping("ack/order")).thenReturn("ack/order");
    return state;
  }

  private static PubFrame publish(String subject) {
    PubFrame frame = new PubFrame(1024);
    frame.setSubject(subject);
    frame.setPayload(new byte[]{1});
    return frame;
  }

  private static class RecordingListener extends PubListener {
    private final List<String> streamTargets;
    private final List<Destination> delivered = new ArrayList<>();
    private int streamLookups;

    RecordingListener(List<String> streamTargets) {
      this.streamTargets = streamTargets;
    }

    @Override
    protected boolean supportsDestinationSequence(String namespace) {
      return true;
    }

    @Override
    protected void ensureArchiveForStream(SessionState connection, String stream, String destination) {
      // Live archive wiring is exercised by JetStream's integration tests.
    }

    @Override
    protected List<String> getMatchingStreamDestinations(String subject) {
      streamLookups++;
      return streamTargets;
    }

    @Override
    protected long handleMessageStoreToDestination(Destination destination, SessionState state, PayloadFrame frame, boolean streamTarget) {
      delivered.add(destination);
      return streamTarget ? 1 : 0;
    }
  }

  @Test
  void repliesWithPersistedStreamSequenceAfterDelivery() throws Exception {
    SessionState state = setup(true);
    Destination core = mock(Destination.class);
    Destination stream = mock(Destination.class);
    when(state.getSession().findDestination("ack/order", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(core));
    when(state.getSession().findDestination("ACK/ack/order", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(stream));
    when(state.getJetStreamRequestManager().getSid("_INBOX.publish")).thenReturn("sid-8");

    PubFrame frame = publish("ack.order");
    frame.setReplyTo("_INBOX.publish");
    new RecordingListener(List.of("ACK/ack/order")).frameEvent(frame, state, true);

    ArgumentCaptor<io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame> sent =
        ArgumentCaptor.forClass(io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame.class);
    verify(state).send(sent.capture());
    MsgFrame response = assertInstanceOf(MsgFrame.class, sent.getValue());
    var body = JsonParser.parseString(new String(response.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
    assertEquals("ACK", body.get("stream").getAsString());
    assertEquals(1L, body.get("seq").getAsLong());
    assertFalse(body.get("duplicate").getAsBoolean());
    assertEquals("_INBOX.publish", response.getSubject());
    assertEquals("sid-8", response.getSubscriptionId());
  }

  @Test
  void failedStreamStorageDoesNotPublishSuccessAcknowledgement() throws Exception {
    SessionState state = setup(true);
    Destination core = mock(Destination.class);
    Destination stream = mock(Destination.class);
    when(state.getSession().findDestination("ack/order", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(core));
    when(state.getSession().findDestination("ACK/ack/order", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(stream));

    PubFrame frame = publish("ack.order");
    frame.setReplyTo("_INBOX.publish");
    RecordingListener listener = new RecordingListener(List.of("ACK/ack/order")) {
      @Override
      protected long handleMessageStoreToDestination(Destination destination, SessionState engine,
                                                      PayloadFrame payload, boolean streamTarget) {
        return 0;
      }
    };
    listener.frameEvent(frame, state, true);

    ArgumentCaptor<io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame> sent =
        ArgumentCaptor.forClass(io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame.class);
    verify(state).send(sent.capture());
    MsgFrame response = assertInstanceOf(MsgFrame.class, sent.getValue());
    var body = JsonParser.parseString(new String(response.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
    assertTrue(body.has("error"));
    assertFalse(body.has("seq"));
  }
  @Test
  void unsupported_stream_global_sequences_fail_before_any_message_is_stored() throws Exception {
    SessionState state = setup(true);
    PubFrame frame = publish("ack.order");
    frame.setReplyTo("_INBOX.publish");
    RecordingListener listener = new RecordingListener(List.of("ACK/ack/order")) {
      @Override
      protected boolean supportsDestinationSequence(String namespace) {
        return false;
      }
    };
    listener.frameEvent(frame, state, true);
    verify(state.getSession(), never()).findDestination(anyString(), any());
    ArgumentCaptor<io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame> sent =
        ArgumentCaptor.forClass(io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame.class);
    verify(state).send(sent.capture());
    MsgFrame response = assertInstanceOf(MsgFrame.class, sent.getValue());
    var body = JsonParser.parseString(new String(response.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
    assertEquals(501, body.getAsJsonObject("error").get("code").getAsInt());
    assertFalse(body.has("seq"));
  }

}
