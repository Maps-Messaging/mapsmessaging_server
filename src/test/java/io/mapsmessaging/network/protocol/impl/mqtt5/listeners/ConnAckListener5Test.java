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

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.dto.rest.config.auth.AuthConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.ConnAck5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnAckListener5Test {

  @Test
  void alreadyConnectedSessionIsResumedWithoutRecreatingIt() throws Exception {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    Session session = mock(Session.class);
    EndPoint endPoint = mock(EndPoint.class);
    when(protocol.getSession()).thenReturn(session);

    assertNull(new ConnAckListener5().handlePacket(new ConnAck5(), session, endPoint, protocol));

    verify(session).resumeState();
    verify(protocol).setConnected(true);
    verify(endPoint, never()).close();
  }

  @Test
  void rejectedConnectionClosesEndpoint() throws Exception {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    EndPointConnectionServerConfigDTO config = mock(EndPointConnectionServerConfigDTO.class);
    AuthConfigDTO authConfig = mock(AuthConfigDTO.class);
    when(protocol.getSession()).thenReturn(null);
    when(endPoint.getConfig()).thenReturn(config);
    when(config.getAuthConfig()).thenReturn(authConfig);
    when(authConfig.getSessionId()).thenReturn("session");
    when(authConfig.getUsername()).thenReturn("user");
    when(authConfig.getPassword()).thenReturn("password");

    ConnAck5 connAck = new ConnAck5();
    connAck.setStatusCode(StatusCode.NOT_AUTHORISED);

    assertNull(new ConnAckListener5().handlePacket(connAck, null, endPoint, protocol));

    verify(endPoint).close();
    verify(protocol, never()).setConnected(true);
  }
}
