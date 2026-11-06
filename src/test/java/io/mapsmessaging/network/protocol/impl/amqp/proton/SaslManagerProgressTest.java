/*
 *
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

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mapsmessaging.network.protocol.sasl.SaslAuthenticationMechanism;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.TransportResult;
import org.junit.jupiter.api.Test;

class SaslManagerProgressTest {

  @Test
  void unauthenticated_endpoint_accepts_plain_amqp_header() {
    Transport transport = Transport.Factory.create();
    new SaslManager(transport.sasl(), null);
    transport.getInputBuffer().put(new byte[] {'A', 'M', 'Q', 'P', 0, 1, 0, 0});

    TransportResult result = transport.processInput();

    assertTrue(result.isOk(), result.getErrorDescription());
  }

  @Test
  void authenticated_endpoint_rejects_plain_amqp_header() {
    Transport transport = Transport.Factory.create();
    SaslAuthenticationMechanism mechanism = mock(SaslAuthenticationMechanism.class);
    when(mechanism.getMechanism()).thenReturn("SCRAM-SHA-256");
    new SaslManager(transport.sasl(), mechanism);
    transport.getInputBuffer().put(new byte[] {'A', 'M', 'Q', 'P', 0, 1, 0, 0});

    assertFalse(transport.processInput().isOk());
  }

  @Test
  void emptyProtonInputDoesNotAdvanceScram() throws Exception {
    Sasl sasl = mock(Sasl.class);
    SaslAuthenticationMechanism mechanism = mock(SaslAuthenticationMechanism.class);
    when(mechanism.getMechanism()).thenReturn("SCRAM-SHA-256");
    SaslManager manager = new SaslManager(sasl, mechanism);

    when(sasl.pending()).thenReturn(0);
    manager.challenge();
    assertFalse(manager.isDone());

    verify(mechanism, never()).challenge(any());
    verify(sasl, never()).done(Sasl.PN_SASL_OK);
  }

  @Test
  void receivedChallengeProducesResponseAndSingleSuccessfulOutcome() throws Exception {
    Sasl sasl = mock(Sasl.class);
    SaslAuthenticationMechanism mechanism = mock(SaslAuthenticationMechanism.class);
    when(mechanism.getMechanism()).thenReturn("SCRAM-SHA-256");
    SaslManager manager = new SaslManager(sasl, mechanism);
    when(sasl.pending()).thenReturn(3);
    doAnswer(invocation -> {
      byte[] buffer = invocation.getArgument(0);
      System.arraycopy(new byte[] {1, 2, 3}, 0, buffer, 0, 3);
      return 3;
    }).when(sasl).recv(any(byte[].class), anyInt(), anyInt());
    when(mechanism.challenge(new byte[] {1, 2, 3})).thenReturn(new byte[] {4, 5});
    when(mechanism.complete()).thenReturn(true);
    when(mechanism.getUsername()).thenReturn("authenticated");
    when(sasl.getOutcome()).thenReturn(Sasl.SaslOutcome.PN_SASL_OK);

    manager.challenge();
    assertTrue(manager.isDone());
    assertTrue(manager.isDone());

    verify(sasl).send(new byte[] {4, 5}, 0, 2);
    verify(sasl, times(1)).done(Sasl.PN_SASL_OK);
    assertEquals("authenticated", manager.getUsername());
  }

  @Test
  void failedProtonReadDoesNotAdvanceMechanism() throws Exception {
    Sasl sasl = mock(Sasl.class);
    SaslAuthenticationMechanism mechanism = mock(SaslAuthenticationMechanism.class);
    when(mechanism.getMechanism()).thenReturn("SCRAM-SHA-256");
    SaslManager manager = new SaslManager(sasl, mechanism);

    when(sasl.pending()).thenReturn(5);
    when(sasl.recv(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
    manager.challenge();

    verify(mechanism, never()).challenge(any());
  }
}
