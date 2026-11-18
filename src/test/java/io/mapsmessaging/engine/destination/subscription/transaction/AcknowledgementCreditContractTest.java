/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.transaction;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcknowledgementCreditContractTest {

  @Test
  void fixedCreditAllowsExactlyTheConfiguredNumberOfOutstandingMessages() {
    ClientAcknowledgementController acknowledgements =
        new ClientAcknowledgementController(new FixedCreditManager(3));

    for (int id = 1; id <= 3; id++) {
      assertTrue(acknowledgements.canSend());
      acknowledgements.sent(message(id));
    }

    assertEquals(3, acknowledgements.size());
    assertFalse(acknowledgements.canSend());
    acknowledgements.ack(1);
    assertTrue(acknowledgements.canSend());
  }

  @Test
  void clientCreditAllowsEveryGrantedSlotBeforeMoreCreditIsRequired() {
    ClientAcknowledgementController acknowledgements =
        new ClientAcknowledgementController(new ClientCreditManager(3));

    for (int id = 1; id <= 3; id++) {
      assertTrue(acknowledgements.canSend(),
          "Granted credit must not be counted twice against outstanding messages");
      acknowledgements.sent(message(id));
    }
    assertFalse(acknowledgements.canSend());
  }

  @Test
  void individualAcknowledgementsReleaseOnlyTheSpecifiedMessage() {
    IndividualAcknowledgementController acknowledgements =
        new IndividualAcknowledgementController(new FixedCreditManager(3));
    acknowledgements.sent(message(10));
    acknowledgements.sent(message(11));
    acknowledgements.sent(message(12));

    acknowledgements.ack(11);
    assertEquals(2, acknowledgements.size());
    assertTrue(acknowledgements.getOutstanding().stream().anyMatch(x -> x.getId() == 10));
    assertFalse(acknowledgements.getOutstanding().stream().anyMatch(x -> x.getId() == 11));
    assertTrue(acknowledgements.getOutstanding().stream().anyMatch(x -> x.getId() == 12));
  }

  @Test
  void blockAcknowledgementReleasesAllMessagesUpToAcknowledgedIdentifier() {
    ClientAcknowledgementController acknowledgements =
        new ClientAcknowledgementController(new FixedCreditManager(3));
    acknowledgements.sent(message(10));
    acknowledgements.sent(message(11));
    acknowledgements.sent(message(12));

    acknowledgements.ack(11);

    assertEquals(1, acknowledgements.size());
    assertEquals(12L, acknowledgements.getOutstanding().get(0).getId());
  }

  @Test
  void rollbackRestoresFixedCreditCapacity() {
    IndividualAcknowledgementController acknowledgements =
        new IndividualAcknowledgementController(new FixedCreditManager(1));
    acknowledgements.sent(message(20));
    assertFalse(acknowledgements.canSend());

    acknowledgements.rollback(20);

    assertEquals(0, acknowledgements.size());
    assertTrue(acknowledgements.canSend());
  }

  private static Message message(long identifier) {
    return new MessageBuilder().setId(identifier).build();
  }
}
