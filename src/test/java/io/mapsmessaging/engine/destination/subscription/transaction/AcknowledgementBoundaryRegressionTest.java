/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.transaction;

import io.mapsmessaging.api.message.Message;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Boundary cases for the distinct cumulative and individual acknowledgement contracts. */
class AcknowledgementBoundaryRegressionTest {

  @Test
  void cumulativeAcknowledgementIgnoresLateIdsAndSettlesOnlyUpToAcknowledgedId() {
    ClientAcknowledgementController controller = new ClientAcknowledgementController(new FixedCreditManager(3));
    controller.sent(message(11L));
    controller.sent(message(12L));
    controller.sent(message(13L));
    assertFalse(controller.canSend());

    controller.ack(10L);
    assertEquals(List.of(11L, 12L, 13L), outstandingIds(controller));
    assertFalse(controller.canSend());

    controller.ack(12L);
    assertEquals(List.of(13L), outstandingIds(controller));
    assertTrue(controller.canSend());

    controller.ack(12L);
    controller.rollback(10L);
    assertEquals(List.of(13L), outstandingIds(controller));

    controller.ack(999L);
    assertEquals(List.of(), outstandingIds(controller));
    assertTrue(controller.canSend());
  }

  @Test
  void individualAcknowledgementDoesNotSettleOtherIdsOrDoubleReleaseCredit() {
    IndividualAcknowledgementController controller = new IndividualAcknowledgementController(new FixedCreditManager(2));
    controller.sent(message(21L));
    controller.sent(message(22L));
    assertFalse(controller.canSend());

    controller.ack(999L);
    assertEquals(List.of(21L, 22L), outstandingIds(controller));
    assertFalse(controller.canSend());

    controller.ack(22L);
    assertEquals(List.of(21L), outstandingIds(controller));
    assertTrue(controller.canSend());

    controller.ack(22L);
    controller.rollback(999L);
    assertEquals(List.of(21L), outstandingIds(controller));

    controller.rollback(21L);
    assertEquals(List.of(), outstandingIds(controller));
    assertTrue(controller.canSend());
  }

  private static List<Long> outstandingIds(ClientAcknowledgementController controller) {
    return controller.getOutstanding().stream().map(details -> details.getId()).toList();
  }

  private static Message message(long id) {
    Message message = mock(Message.class, RETURNS_DEEP_STUBS);
    when(message.getIdentifier()).thenReturn(id);
    when(message.getPriority().getValue()).thenReturn(1);
    return message;
  }
}
