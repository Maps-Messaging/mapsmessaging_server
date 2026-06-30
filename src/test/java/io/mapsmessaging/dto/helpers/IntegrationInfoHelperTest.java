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

package io.mapsmessaging.dto.helpers;

import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.dto.rest.integration.IntegrationInfoDTO;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntegrationInfoHelperTest {

  @Test
  void fromEndPointConnection_mapsStartedStateAndConfig() {
    EndPointConnection connection = mock(EndPointConnection.class);
    EndPointConnectionServerConfigDTO config = new EndPointConnectionServerConfigDTO();
    when(connection.isStarted()).thenReturn(true);
    when(connection.isPaused()).thenReturn(false);
    when(connection.getProperties()).thenReturn(config);

    IntegrationInfoDTO result = IntegrationInfoHelper.fromEndPointConnection(connection);

    assertEquals("Started", result.getState());
    assertSame(config, result.getConfig());
  }

  @Test
  void fromEndPointConnection_mapsPausedState() {
    EndPointConnection connection = mock(EndPointConnection.class);
    when(connection.isStarted()).thenReturn(true);
    when(connection.isPaused()).thenReturn(true);

    assertEquals("Paused", IntegrationInfoHelper.fromEndPointConnection(connection).getState());
  }

  @Test
  void fromEndPointConnection_mapsStoppedStateWithoutCheckingPaused() {
    EndPointConnection connection = mock(EndPointConnection.class);
    when(connection.isStarted()).thenReturn(false);

    assertEquals("Stopped", IntegrationInfoHelper.fromEndPointConnection(connection).getState());
  }
}
