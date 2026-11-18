/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.session.persistence;

import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SessionDetailsSharedGroupIdentityTest {

  @Test
  void differentSharedGroupsWithDefaultAliasSurviveSessionSaveAndReload() throws Exception {
    SessionDetails details = new SessionDetails("session", "persistent-id", 51L, 3600L);
    details.getSubscriptionContextList().add(shared("workers-A"));
    details.getSubscriptionContextList().add(shared("workers-B"));

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    details.save(bytes);
    SessionDetails restored = new SessionDetails(new ByteArrayInputStream(bytes.toByteArray()));

    Map<String, SubscriptionContext> contexts = restored.getSubscriptionContextMap();
    assertEquals(2, contexts.size(), "The default alias must not cause distinct shared groups to overwrite one another");
    assertTrue(contexts.containsKey(shared("workers-A").getKey()));
    assertTrue(contexts.containsKey(shared("workers-B").getKey()));
  }

  @Test
  void legacyNonSharedAliasIndexingIsUnchanged() {
    SessionDetails details = new SessionDetails("session", "persistent-id", 52L, 3600L);
    SubscriptionContext context = new SubscriptionContextBuilder("topic/orders", ClientAcknowledgement.AUTO)
        .setAlias("legacy-alias").build();
    details.getSubscriptionContextList().add(context);

    assertSame(context, details.getSubscriptionContextMap().get("legacy-alias"));
  }

  @Test
  void mixedOrdinaryAliasAndSharedGroupsSurviveSamePersistentReload() throws Exception {
    SessionDetails details = new SessionDetails("session", "persistent-id", 53L, 3600L);
    SubscriptionContext ordinary = new SubscriptionContextBuilder("topic/orders", ClientAcknowledgement.AUTO)
        .setAlias("ordinary-alias").build();
    details.getSubscriptionContextList().add(ordinary);
    details.getSubscriptionContextList().add(shared("group-A"));
    details.getSubscriptionContextList().add(shared("group-B"));

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    details.save(bytes);
    SessionDetails restored = new SessionDetails(new ByteArrayInputStream(bytes.toByteArray()));
    Map<String, SubscriptionContext> contexts = restored.getSubscriptionContextMap();

    assertEquals(3, contexts.size());
    assertEquals("topic/orders", contexts.get("ordinary-alias").getFilter());
    assertEquals("group-A", contexts.get(shared("group-A").getKey()).getSharedName());
    assertEquals("group-B", contexts.get(shared("group-B").getKey()).getSharedName());
  }

  @Test
  void sameSharedGroupOnDifferentFiltersRetainsBothSubscriptionsOnReload() throws Exception {
    SessionDetails details = new SessionDetails("session", "persistent-id", 54L, 3600L);
    SubscriptionContext first = new SubscriptionContextBuilder("topic/alpha", ClientAcknowledgement.AUTO)
        .setSharedName("workers").build();
    SubscriptionContext second = new SubscriptionContextBuilder("topic/beta", ClientAcknowledgement.AUTO)
        .setSharedName("workers").build();
    details.getSubscriptionContextList().add(first);
    details.getSubscriptionContextList().add(second);

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    details.save(bytes);
    SessionDetails restored = new SessionDetails(new ByteArrayInputStream(bytes.toByteArray()));
    Map<String, SubscriptionContext> contexts = restored.getSubscriptionContextMap();

    assertEquals(2, contexts.size());
    assertTrue(contexts.containsKey(first.getKey()));
    assertTrue(contexts.containsKey(second.getKey()));
    assertEquals("topic/alpha", contexts.get(first.getKey()).getFilter());
    assertEquals("topic/beta", contexts.get(second.getKey()).getFilter());
  }

  private static SubscriptionContext shared(String name) {
    return new SubscriptionContextBuilder("topic/orders", ClientAcknowledgement.AUTO)
        .setSharedName(name).build();
  }
}
