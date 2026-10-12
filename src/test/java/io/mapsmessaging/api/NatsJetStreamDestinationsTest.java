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

package io.mapsmessaging.api;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.NatsStreamMembership;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NatsJetStreamDestinationsTest {

  @TempDir
  Path directory;

  @Test
  void denied_delete_does_not_change_membership_or_physical_data() throws Exception {
    DestinationImpl resource = resource();
    Destination destination = new Destination(resource, mock(SecurityContext.class));
    AuthManager auth = mock(AuthManager.class);
    try (var authentication = mockStatic(AuthManager.class)) {
      authentication.when(AuthManager::getInstance).thenReturn(auth);
      assertThrows(IOException.class, () -> NatsJetStreamDestinations.disable(destination));
      assertTrue(NatsStreamMembership.isEnabled(resource));
      try (var files = Files.list(directory)) {
        assertEquals(0, files.count());
      }
    }
  }

  @Test
  void authorised_removal_changes_only_nats_membership() throws Exception {
    DestinationImpl resource = resource();
    Destination destination = new Destination(resource, mock(SecurityContext.class));
    Path payload = directory.resolve("messages.bin");
    Files.write(payload, new byte[]{1, 2});
    AuthManager auth = mock(AuthManager.class);
    when(auth.canAccess(any(), any(), any())).thenReturn(true);
    NamespaceManager namespaces = mock(NamespaceManager.class);
    try (var authentication = mockStatic(AuthManager.class);
         var namespaceAccess = mockStatic(NamespaceManager.class)) {
      authentication.when(AuthManager::getInstance).thenReturn(auth);
      namespaceAccess.when(NamespaceManager::getInstance).thenReturn(namespaces);
      NatsJetStreamDestinations.disable(destination);
      assertFalse(NatsStreamMembership.isEnabled(resource));
      assertArrayEquals(new byte[]{1, 2}, Files.readAllBytes(payload));
      verify(namespaces).deleted(resource);
      verify(resource, never()).close();
    }
  }

  private DestinationImpl resource() {
    DestinationImpl resource = mock(DestinationImpl.class);
    when(resource.getResourceType()).thenReturn(DestinationType.TOPIC);
    when(resource.getFullyQualifiedNamespace()).thenReturn("EVENTS/orders/created");
    when(resource.getPhysicalLocation()).thenReturn(directory.toString());
    return resource;
  }
}
