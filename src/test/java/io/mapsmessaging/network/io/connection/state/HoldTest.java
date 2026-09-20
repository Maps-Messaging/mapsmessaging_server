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

package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.route.link.LinkState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class HoldTest {

  @Test
  void pullLinksUnsubscribeRemoteNamespaceAndSchemaThenEnterHolding() {
    EndPointConnection connection = mockConnection();
    Protocol protocol = connection.getProtocol();

    LinkConfigDTO link = link("pull", "/local", "/remote/#", true);
    when(connection.getProperties().getLinkConfigs()).thenReturn(List.of(link));

    Hold hold = new Hold(connection);
    hold.execute();

    verify(protocol).unsubscribeRemote("/remote/#");
    verify(protocol).unsubscribeRemote(DestinationMode.SCHEMA.getNamespace() + "/remote/#");
    verify(protocol, never()).unsubscribeLocal(anyString());
    verify(connection).scheduleState(argThat(state -> state instanceof Holding));
    assertEquals("Hold", hold.getName());
    assertEquals(LinkState.CONNECTED, hold.getLinkState());
  }

  @Test
  void pushLinksUnsubscribeLocalNamespaceAndOptionalSchema() {
    EndPointConnection connection = mockConnection();
    Protocol protocol = connection.getProtocol();

    LinkConfigDTO withSchema = link("push", "local/topic", "/remote/#", true);
    LinkConfigDTO withoutSchema = link("push", "/other", "/elsewhere/#", false);
    when(connection.getProperties().getLinkConfigs()).thenReturn(List.of(withSchema, withoutSchema));

    new Hold(connection).run();

    verify(protocol).unsubscribeLocal("local/topic");
    verify(protocol).unsubscribeLocal(DestinationMode.SCHEMA.getNamespace() + "/local/topic");
    verify(protocol).unsubscribeLocal("/other");
    verify(protocol, never()).unsubscribeRemote(anyString());
    verify(connection).scheduleState(argThat(state -> state instanceof Holding));
  }

  @Test
  void unknownDirectionDoesNotUnsubscribeButStillTransitions() {
    EndPointConnection connection = mockConnection();
    Protocol protocol = connection.getProtocol();

    when(connection.getProperties().getLinkConfigs())
        .thenReturn(List.of(link("sideways", "/local", "/remote", false)));

    new Hold(connection).execute();

    verify(protocol, never()).unsubscribeLocal(anyString());
    verify(protocol, never()).unsubscribeRemote(anyString());
    verify(connection).scheduleState(argThat(state -> state instanceof Holding));
  }

  private static EndPointConnection mockConnection() {
    EndPointConnection connection = mock(EndPointConnection.class);
    EndPointConnectionServerConfigDTO properties = mock(EndPointConnectionServerConfigDTO.class);
    Protocol protocol = mock(Protocol.class);
    Logger logger = mock(Logger.class);

    when(connection.getProperties()).thenReturn(properties);
    when(connection.getProtocol()).thenReturn(protocol);
    when(connection.getLogger()).thenReturn(logger);

    return connection;
  }

  private static LinkConfigDTO link(
      String direction,
      String local,
      String remote,
      boolean includeSchema
  ) {
    LinkConfigDTO link = new LinkConfigDTO();
    link.setDirection(direction);
    link.setLocalNamespace(local);
    link.setRemoteNamespace(remote);
    link.setIncludeSchema(includeSchema);
    return link;
  }
}
