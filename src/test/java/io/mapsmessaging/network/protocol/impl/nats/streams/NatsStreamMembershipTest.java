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

package io.mapsmessaging.network.protocol.impl.nats.streams;

import io.mapsmessaging.engine.destination.DestinationImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NatsStreamMembershipTest {

  @TempDir
  Path directory;

  @Test
  void logical_removal_survives_restart_and_preserves_maps_data() throws Exception {
    Path payload = directory.resolve("existing-maps-data.bin");
    Files.write(payload, new byte[]{1, 2, 3});
    DestinationImpl destination = destination();
    assertTrue(NatsStreamMembership.isEnabled(destination));

    NatsStreamMembership.setEnabled(destination, false);

    assertFalse(NatsStreamMembership.isEnabled(destination()));
    assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(payload));
    verify(destination, never()).close();
    NatsStreamMembership.setEnabled(destination, true);
    assertTrue(NatsStreamMembership.isEnabled(destination()));
    assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(payload));
  }

  private DestinationImpl destination() {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getPhysicalLocation()).thenReturn(directory.toString());
    return destination;
  }
}
