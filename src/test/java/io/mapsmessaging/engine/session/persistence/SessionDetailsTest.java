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

package io.mapsmessaging.engine.session.persistence;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.security.access.Identity;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionDetailsTest {

  @Test
  void constructorInitialisesCurrentVersionAndExpiry() {
    long before = System.currentTimeMillis();

    SessionDetails details = new SessionDetails("session", "unique", 42L, 30L);

    long after = System.currentTimeMillis();
    assertEquals(4, details.getVersion());
    assertEquals("session", details.getSessionName());
    assertEquals("unique", details.getUniqueId());
    assertEquals(42L, details.getInternalUnqueId());
    assertTrue(details.getExpiryTime() >= before + TimeUnit.SECONDS.toMillis(30));
    assertTrue(details.getExpiryTime() <= after + TimeUnit.SECONDS.toMillis(30));
  }

  @Test
  void versionFourRoundTripPreservesCoreFieldsWithNoIdentity() throws Exception {
    SessionDetails details = new SessionDetails("session", "unique", 73L, 60L);
    details.setIdentity(null);

    SessionDetails restored = roundTrip(details);

    assertEquals(4, restored.getVersion());
    assertEquals("session", restored.getSessionName());
    assertEquals("unique", restored.getUniqueId());
    assertEquals(73L, restored.getInternalUnqueId());
    assertEquals(details.getExpiryTime(), restored.getExpiryTime());
    assertNull(restored.getIdentity());
    assertTrue(restored.getSubscriptionContextList().isEmpty());
  }

  @Test
  void versionFourRoundTripPreservesIdentityAndAttributes() throws Exception {
    UUID id = UUID.randomUUID();
    Identity identity = mock(Identity.class);
    when(identity.getId()).thenReturn(id);
    when(identity.getUsername()).thenReturn("test-user");
    when(identity.getAttributes()).thenReturn(Map.of("role", "operator"));
    when(identity.getGroupList()).thenReturn(List.of());

    SessionDetails details = new SessionDetails("session", "unique", 91L, 60L);
    details.setIdentity(identity);

    SessionDetails restored = roundTrip(details);

    assertEquals(id, restored.getIdentity().getId());
    assertEquals("test-user", restored.getIdentity().getUsername());
    assertEquals("operator", restored.getIdentity().getAttributes().get("role"));
    assertTrue(restored.getIdentity().getGroupList().isEmpty());
  }

  @Test
  void subscriptionMapUsesAliasesAndCanBeCleared() {
    SubscriptionContext first = mock(SubscriptionContext.class);
    SubscriptionContext second = mock(SubscriptionContext.class);
    when(first.getAlias()).thenReturn("first");
    when(second.getAlias()).thenReturn("second");

    SessionDetails details = new SessionDetails();
    details.getSubscriptionContextList().add(first);
    details.getSubscriptionContextList().add(second);

    Map<String, SubscriptionContext> map = details.getSubscriptionContextMap();

    assertEquals(2, map.size());
    assertSame(first, map.get("first"));
    assertSame(second, map.get("second"));

    details.clearSubscriptions();
    assertTrue(details.getSubscriptionContextList().isEmpty());
  }

  private SessionDetails roundTrip(SessionDetails details) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    details.save(output);
    return new SessionDetails(new ByteArrayInputStream(output.toByteArray()));
  }
}
