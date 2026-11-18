/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.CreditHandler;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionContextPersistenceContractTest {

  @Test
  void persistentReloadPreservesSharedGroupAndSelector() throws Exception {
    SubscriptionContext original = new SubscriptionContextBuilder(
        "topic/sensors/+", ClientAcknowledgement.INDIVIDUAL)
        .setSharedName("workers-A")
        .setSelector("temperature > 20")
        .setQos(QualityOfService.AT_LEAST_ONCE)
        .setAlias("dashboard")
        .setReceiveMaximum(12)
        .build();

    SubscriptionContext restored = roundTrip(original);
    assertEquals(original.getKey(), restored.getKey());
    assertEquals("workers-A", restored.getSharedName());
    assertEquals("temperature > 20", restored.getSelector());
    assertEquals("dashboard", restored.getAlias());
    assertEquals(QualityOfService.AT_LEAST_ONCE, restored.getQualityOfService());
    assertEquals(ClientAcknowledgement.INDIVIDUAL, restored.getAcknowledgementController());
    assertEquals(12, restored.getReceiveMaximum());
    assertTrue(restored.containsWildcard());
    assertTrue(restored.isSharedSubscription());
  }

  @Test
  void persistentReloadPreservesDeliveryAndRetainOptions() throws Exception {
    SubscriptionContext original = new SubscriptionContextBuilder(
        "topic/events/#", ClientAcknowledgement.BLOCK)
        .setCreditHandler(CreditHandler.CLIENT)
        .setRetainHandler(RetainHandler.DO_NOT_SEND)
        .setReceiveMaximum(4)
        .setNoLocalMessages(true)
        .setAllowOverlap(true)
        .setSync(true)
        .build();
    original.setMaxAtRest(55);

    SubscriptionContext restored = roundTrip(original);
    assertEquals(CreditHandler.CLIENT, restored.getCreditHandler());
    assertEquals(RetainHandler.DO_NOT_SEND, restored.getRetainHandler());
    assertEquals(4, restored.getReceiveMaximum());
    assertEquals(55, restored.getMaxAtRest());
    assertTrue(restored.noLocalMessages());
    assertTrue(restored.allowOverlap());
    assertTrue(restored.isSync());
  }

  @Test
  void distinctSharedGroupNamesRemainDistinctAfterReload() throws Exception {
    SubscriptionContext a = new SubscriptionContextBuilder(
        "topic/orders", ClientAcknowledgement.AUTO).setSharedName("A").build();
    SubscriptionContext b = new SubscriptionContextBuilder(
        "topic/orders", ClientAcknowledgement.AUTO).setSharedName("B").build();

    assertEquals("A", roundTrip(a).getSharedName());
    assertEquals("B", roundTrip(b).getSharedName());
  }

  private static SubscriptionContext roundTrip(SubscriptionContext context) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    context.save(bytes);
    return new SubscriptionContext(new ByteArrayInputStream(bytes.toByteArray()), 7L);
  }
}
