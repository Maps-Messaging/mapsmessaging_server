/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.modes;

import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import io.mapsmessaging.engine.tasks.Response;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers a wildcard subscription registered before the destination exists.
 * Multiple matching contexts must not silently select whichever was iterated first.
 */
class DynamicDestinationSubscriptionSelectionRegressionTest {

  @Test
  void lateDestinationChoosesHighestQosIndependentOfRegistrationOrder() {
    SubscriptionContext low = context(QualityOfService.AT_MOST_ONCE);
    SubscriptionContext high = context(QualityOfService.EXACTLY_ONCE);
    DestinationImpl destination = destination("sensors/temperature");

    RecordingManager first = new RecordingManager();
    first.created(mock(SubscriptionController.class), destination, List.of(
        new DestinationSet(low, Map.of()),
        new DestinationSet(high, Map.of())));
    assertEquals(1, first.scheduled.size());
    assertSame(high, first.scheduled.get(0));

    RecordingManager reverse = new RecordingManager();
    reverse.created(mock(SubscriptionController.class), destination, List.of(
        new DestinationSet(high, Map.of()),
        new DestinationSet(low, Map.of())));
    assertEquals(1, reverse.scheduled.size());
    assertSame(high, reverse.scheduled.get(0));
  }

  @Test
  void lateDestinationDoesNotScheduleUnrelatedFilters() {
    DestinationImpl destination = destination("sensors/temperature");
    SubscriptionContext matching = new SubscriptionContext("sensors/+");
    SubscriptionContext unrelated = new SubscriptionContext("vehicles/#");

    RecordingManager manager = new RecordingManager();
    manager.created(mock(SubscriptionController.class), destination, List.of(
        new DestinationSet(unrelated, Map.of()),
        new DestinationSet(matching, Map.of())));

    assertEquals(List.of(matching), manager.scheduled);
  }

  @Test
  void duplicateCreationNotificationDoesNotRescheduleSameDestinationInstance() {
    SubscriptionContext context = context(QualityOfService.AT_LEAST_ONCE);
    DestinationSet set = new DestinationSet(context, Map.of());
    DestinationImpl destination = destination("sensors/temperature");
    RecordingManager manager = new RecordingManager();
    SubscriptionController controller = mock(SubscriptionController.class);

    manager.created(controller, destination, List.of(set));
    manager.created(controller, destination, List.of(set));

    assertEquals(List.of(context), manager.scheduled);
    assertEquals(1, set.size());
  }

  @Test
  void deletingAndRecreatingDestinationReschedulesMatchingWildcard() {
    SubscriptionContext context = context(QualityOfService.AT_LEAST_ONCE);
    DestinationSet set = new DestinationSet(context, Map.of());
    DestinationImpl first = destination("sensors/temperature");
    DestinationImpl replacement = destination("sensors/temperature");
    RecordingManager manager = new RecordingManager();
    SubscriptionController controller = mock(SubscriptionController.class);

    manager.created(controller, first, List.of(set));
    Subscription existing = mock(Subscription.class);
    manager.add(existing, first);
    manager.deleted(first, new java.util.LinkedHashMap<>(Map.of(context.getKey(), set)));
    manager.created(controller, replacement, List.of(set));

    assertEquals(List.of(context, context), manager.scheduled);
    assertEquals(1, set.size());
    assertSame(replacement, set.iterator().next());
  }

  @Test
  void distinctSharedGroupsBothSubscribeAlongsideHighestQosOrdinarySubscription() {
    SubscriptionContext low = context(QualityOfService.AT_MOST_ONCE);
    SubscriptionContext high = context(QualityOfService.EXACTLY_ONCE);
    SubscriptionContext groupA = new SubscriptionContextBuilder("sensors/+", ClientAcknowledgement.AUTO)
        .setSharedName("group-A").build();
    SubscriptionContext groupB = new SubscriptionContextBuilder("sensors/+", ClientAcknowledgement.AUTO)
        .setSharedName("group-B").build();
    DestinationImpl destination = destination("sensors/temperature");
    RecordingManager manager = new RecordingManager();

    manager.created(mock(SubscriptionController.class), destination, List.of(
        new DestinationSet(low, Map.of()),
        new DestinationSet(groupA, Map.of()),
        new DestinationSet(high, Map.of()),
        new DestinationSet(groupB, Map.of())));

    assertEquals(3, manager.scheduled.size());
    assertSame(high, manager.scheduled.get(0));
    assertSame(groupA, manager.scheduled.get(1));
    assertSame(groupB, manager.scheduled.get(2));
  }

  private static SubscriptionContext context(QualityOfService qos) {
    return new SubscriptionContextBuilder("sensors/+", ClientAcknowledgement.AUTO)
        .setQos(qos).build();
  }

  private static DestinationImpl destination(String name) {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn(name);
    return destination;
  }

  private static final class RecordingManager extends NormalSubscriptionModeManager {
    private final List<SubscriptionContext> scheduled = new ArrayList<>();

    @Override
    protected Future<Response> scheduleSubscription(SubscriptionController controller,
        SubscriptionContext context, DestinationImpl destination, AtomicLong counter) {
      scheduled.add(context);
      return CompletableFuture.completedFuture(null);
    }
  }
}
