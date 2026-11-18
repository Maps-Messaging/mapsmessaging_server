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

  private static SubscriptionContext shared(String name) {
    return new SubscriptionContextBuilder("topic/orders", ClientAcknowledgement.AUTO)
        .setSharedName(name).build();
  }
}
