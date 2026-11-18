/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination.subscription.state;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class QueueBrowserStateIsolationTest {

  @Test
  void browserSnapshotsPendingAndInflightWithoutConsumingQueue() throws IOException {
    MessageStateManagerImpl queue = state();
    Message pending = message(101);
    Message inFlight = message(102);
    queue.register(pending);
    queue.register(inFlight);
    queue.allocate(inFlight);

    try (BrowserHandle browser = new BrowserHandle(queue, true)) {
      assertTrue(browser.state.hasMessage(101));
      assertTrue(browser.state.hasMessage(102));
      assertEquals(2, browser.state.pending());
      assertEquals(1, queue.pending());
      assertTrue(queue.hasMessagesInFlight());
    }
  }

  @Test
  void browserSnapshotDoesNotAutomaticallyIncludeLaterArrivals() throws IOException {
    MessageStateManagerImpl queue = state();
    queue.register(message(201));

    try (BrowserHandle browser = new BrowserHandle(queue, true)) {
      queue.register(message(202));
      assertTrue(browser.state.hasMessage(201));
      assertFalse(browser.state.hasMessage(202));
      assertTrue(queue.hasMessage(202));
    }
  }

  @Test
  void browserReflectsQueueAcknowledgementWithoutChangingQueueOwnership() throws IOException {
    MessageStateManagerImpl queue = state();
    Message message = message(301);
    queue.register(message);

    try (BrowserHandle browser = new BrowserHandle(queue, true)) {
      assertTrue(browser.state.hasMessage(301));
      queue.allocate(message);
      queue.commit(301);
      assertFalse(browser.state.hasMessage(301));
      assertFalse(queue.hasMessage(301));
    }
  }

  @Test
  void browserReflectsExpiryOfQueueMessage() throws IOException {
    MessageStateManagerImpl queue = state();
    queue.register(message(401));

    try (BrowserHandle browser = new BrowserHandle(queue, true)) {
      queue.expired(401);
      assertFalse(browser.state.hasMessage(401));
      assertFalse(queue.hasMessage(401));
    }
  }

  @Test
  void nonCopyingBrowserStartsEmptyAndParentRemainsUnchanged() throws IOException {
    MessageStateManagerImpl queue = state();
    queue.register(message(501));

    try (BrowserHandle browser = new BrowserHandle(queue, false)) {
      assertFalse(browser.state.hasMessage(501));
      assertTrue(queue.hasMessage(501));
      assertEquals(1, queue.pending());
    }
  }

  private static MessageStateManagerImpl state() {
    return new MessageStateManagerImpl("queue-browser-parent", 1L,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
  }

  private static Message message(long id) {
    return new MessageBuilder().setId(id).build();
  }

  private static final class BrowserHandle implements AutoCloseable {
    private final IteratorStateManagerImpl state;

    BrowserHandle(MessageStateManagerImpl parent, boolean copy) {
      state = new IteratorStateManagerImpl("queue-browser", 2L, parent, copy);
    }

    @Override
    public void close() throws IOException {
      state.close();
    }
  }
}
