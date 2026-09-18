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
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocol;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PubListenerErrorHandlingTest {

  @Test
  void storeFailure_sendsErrorWithoutPropagatingIOException() throws Exception {
    PubListener listener = new PubListener();
    PayloadFrame frame = mock(PayloadFrame.class);
    SessionState engine = mock(SessionState.class);
    JetStreamRequestManager jetStreamRequestManager = mock(JetStreamRequestManager.class);
    Session session = mock(Session.class);
    Destination destination = mock(Destination.class);
    NatsProtocol protocol = mock(NatsProtocol.class);
    ProtocolConfigDTO protocolConfig = mock(ProtocolConfigDTO.class);

    when(frame.getSubject()).thenReturn("events.test");
    when(frame.getPayload()).thenReturn("payload".getBytes(StandardCharsets.UTF_8));
    when(engine.getJetStreamRequestManager()).thenReturn(jetStreamRequestManager);
    when(jetStreamRequestManager.isJetStreamRequest(frame)).thenReturn(false);
    when(engine.getMapping("events/test")).thenReturn("events/test");
    when(engine.getSession()).thenReturn(session);
    when(session.findDestination("events/test", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));
    when(session.getName()).thenReturn("nats-test-session");
    when(engine.getProtocol()).thenReturn(protocol);
    when(protocol.getVersion()).thenReturn("1.0");
    when(protocol.getProtocolConfig()).thenReturn(protocolConfig);
    when(protocolConfig.getMessageDefaults()).thenReturn(null);

    doThrow(new IOException("store failed")).when(destination).storeMessage(any(Message.class));

    assertDoesNotThrow(() -> listener.frameEvent(frame, engine, true));

    ArgumentCaptor<NatsFrame> response = ArgumentCaptor.forClass(NatsFrame.class);
    verify(engine).send(response.capture());

    ErrFrame error = assertInstanceOf(ErrFrame.class, response.getValue());
    assertEquals("store failed", error.getError());
  }
}
