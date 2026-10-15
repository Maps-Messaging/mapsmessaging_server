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

import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.SubFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.UnsubFrame;
import io.mapsmessaging.network.protocol.impl.nats.listener.SubListener;
import io.mapsmessaging.network.protocol.impl.nats.listener.UnsubListener;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JetStreamInboxRoutingTest {

  @Test
  void exactInboxSubscriptionDoesNotRequireWildcard() throws Exception {
    JetStreamRequestManager requests = new JetStreamRequestManager();
    SessionState session = mock(SessionState.class);
    when(session.getJetStreamRequestManager()).thenReturn(requests);
    SubFrame subscription = new SubFrame();
    subscription.setSubject("_INBOX.publish");
    subscription.setSubscriptionId("sid1");

    new SubListener().frameEvent(subscription, session, true);

    assertEquals("sid1", requests.getSid("_INBOX.publish"));
    assertEquals("", requests.getSid("_INBOX.publisher"));
  }

  @Test
  void inboxWildcardOnlyMatchesOneTokenAndCorrectPrefix() {
    JetStreamRequestManager requests = new JetStreamRequestManager();
    requests.registerSid("_INBOX.request.*", "sid2");

    assertEquals("sid2", requests.getSid("_INBOX.request.reply"));
    assertEquals("", requests.getSid("_INBOX.request.reply.extra"));
    assertEquals("", requests.getSid("_INBOX.requests.reply"));
    assertEquals("", requests.getSid(null));
  }

  @Test
  void longerMatchingInboxHasPriority() {
    JetStreamRequestManager requests = new JetStreamRequestManager();
    requests.registerSid("_INBOX.>", "catchall");
    requests.registerSid("_INBOX.request.*", "precise");

    assertEquals("precise", requests.getSid("_INBOX.request.reply"));
    assertEquals("catchall", requests.getSid("_INBOX.request.reply.more"));
  }

  @Test
  void unsubscribingRemovesInboxMapping() throws Exception {
    JetStreamRequestManager requests = new JetStreamRequestManager();
    requests.registerSid("_INBOX.request.*", "sid3");
    SessionState session = mock(SessionState.class);
    when(session.getJetStreamRequestManager()).thenReturn(requests);
    UnsubFrame unsubscribe = mock(UnsubFrame.class);
    when(unsubscribe.getSubscriptionId()).thenReturn("sid3");

    new UnsubListener().frameEvent(unsubscribe, session, true);

    assertEquals("", requests.getSid("_INBOX.request.reply"));
    verify(session).removeSubscription("sid3");
  }

  @Test
  void jetStreamClassificationRequiresProtocolTokenBoundary() {
    JetStreamRequestManager requests = new JetStreamRequestManager();
    MsgFrame jetStream = new MsgFrame(128);
    jetStream.setSubject("$JS.API.INFO");
    assertTrue(requests.isJetStreamRequest(jetStream));

    MsgFrame ordinary = new MsgFrame(128);
    ordinary.setSubject("$JSON.events");
    assertFalse(requests.isJetStreamRequest(ordinary));
    ordinary.setSubject("$KVPrefix.topic");
    assertFalse(requests.isJetStreamRequest(ordinary));
    ordinary.setSubject("$Other.events");
    assertFalse(requests.isJetStreamRequest(ordinary));
  }
}
