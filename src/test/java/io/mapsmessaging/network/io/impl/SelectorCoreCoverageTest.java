/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.io.impl;

import io.mapsmessaging.network.io.Selectable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SelectorCoreCoverageTest {

  @Test
  void registerAssociatesInterestAndAttachmentWithNonBlockingChannel() throws Exception {
    Selector selector = new Selector();
    Pipe pipe = Pipe.open();
    pipe.source().configureBlocking(false);
    Object attachment = new Object();

    try {
      SelectionKey key = selector
          .register(pipe.source(), SelectionKey.OP_READ, attachment)
          .get();

      assertSame(attachment, key.attachment());
      assertEquals(SelectionKey.OP_READ, key.interestOps());
      assertSame(selector.channelSelector, key.selector());
    } finally {
      pipe.source().close();
      pipe.sink().close();
      selector.channelSelector.close();
      selector.close();
    }
  }

  @Test
  void selectedKeyDispatchesReadyOperationsAndIsRemoved() throws Exception {
    Selector selector = new Selector();
    SelectionKey key = mock(SelectionKey.class);
    Selectable selectable = mock(Selectable.class);
    when(key.attachment()).thenReturn(selectable);
    when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
    Set<SelectionKey> selected = new LinkedHashSet<>();
    selected.add(key);

    try {
      processSelectionList(selector, selected);

      verify(selectable).selected(selectable, selector, SelectionKey.OP_READ);
      assertTrue(selected.isEmpty());
    } finally {
      selector.channelSelector.close();
      selector.close();
    }
  }

  @Test
  void selectedCallbackFailureDoesNotPreventKeyRemoval() throws Exception {
    Selector selector = new Selector();
    SelectionKey key = mock(SelectionKey.class);
    Selectable selectable = mock(Selectable.class);
    when(key.attachment()).thenReturn(selectable);
    when(key.readyOps()).thenReturn(SelectionKey.OP_WRITE);
    doThrow(new IllegalStateException("boom"))
        .when(selectable)
        .selected(selectable, selector, SelectionKey.OP_WRITE);
    Set<SelectionKey> selected = new LinkedHashSet<>();
    selected.add(key);

    try {
      assertDoesNotThrow(() -> processSelectionList(selector, selected));
      assertTrue(selected.isEmpty());
    } finally {
      selector.channelSelector.close();
      selector.close();
    }
  }

  @Test
  void nonSelectableAttachmentIsIgnoredAndRemoved() throws Exception {
    Selector selector = new Selector();
    SelectionKey key = mock(SelectionKey.class);
    when(key.attachment()).thenReturn("not-selectable");
    Set<SelectionKey> selected = new LinkedHashSet<>();
    selected.add(key);

    try {
      processSelectionList(selector, selected);
      assertTrue(selected.isEmpty());
    } finally {
      selector.channelSelector.close();
      selector.close();
    }
  }

  private static void processSelectionList(
      Selector selector, Set<SelectionKey> selectedKeys) throws Exception {
    Method method = Selector.class.getDeclaredMethod("processSelectionList", Set.class);
    method.setAccessible(true);
    method.invoke(selector, selectedKeys);
  }
}
