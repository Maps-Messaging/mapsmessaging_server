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

package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.CotProtocolConfigDTO;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.utilities.admin.JMXManager;
import java.io.ByteArrayOutputStream;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CotProtocolOutboundTest {

  @Test
  void completion_waits_for_the_entire_xml_write_through_the_shared_selector() throws Exception {
    try (Fixture fixture = new Fixture()) {
      AtomicInteger completions = new AtomicInteger();
      byte[] xml = event("partial", "café");
      byte[] expected = ("<?xml version='1.0' standalone='yes'?>\n" + new String(xml, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
      ByteArrayOutputStream wire = new ByteArrayOutputStream();
      AtomicInteger allowance = new AtomicInteger(0);
      when(fixture.endpoint.sendPacket(any())).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        int count = Math.min(allowance.get(), packet.available());
        byte[] written = new byte[count];
        packet.get(written);
        wire.writeBytes(written);
        return count;
      });

      fixture.protocol.sendMessage(fixture.delivery(xml, completions::incrementAndGet));
      assertEquals(0, completions.get(), "queueing must not release subscription capacity");
      verify(fixture.endpoint, never()).sendPacket(any());

      fixture.selected(SelectionKey.OP_WRITE);
      assertEquals(0, completions.get(), "a blocked socket must retain subscription capacity");
      allowance.set(7);
      fixture.selected(SelectionKey.OP_WRITE);
      assertEquals(7, wire.size());
      assertEquals(0, completions.get(), "a partial write must retain subscription capacity");
      allowance.set(Integer.MAX_VALUE);
      fixture.selected(SelectionKey.OP_WRITE);

      assertArrayEquals(expected, wire.toByteArray());
      assertEquals(1, completions.get());
      fixture.selected(SelectionKey.OP_WRITE);
      assertEquals(1, completions.get());
    }
  }

  @Test
  void adjacent_large_events_preserve_order_and_complete_once_without_a_callback() throws Exception {
    try (Fixture fixture = new Fixture()) {
      AtomicInteger completions = new AtomicInteger();
      byte[] first = event("first", "payload".repeat(1000));
      byte[] second = event("second", "second");
      ByteArrayOutputStream wire = new ByteArrayOutputStream();
      when(fixture.endpoint.sendPacket(any())).thenAnswer(invocation -> {
        Packet packet = invocation.getArgument(0);
        byte[] written = new byte[packet.available()];
        packet.get(written);
        wire.writeBytes(written);
        return written.length;
      });

      fixture.protocol.sendMessage(fixture.delivery(first, completions::incrementAndGet));
      fixture.protocol.sendMessage(fixture.delivery(second, null));
      assertEquals(0, completions.get());
      fixture.selected(SelectionKey.OP_WRITE);

      ByteArrayOutputStream expected = new ByteArrayOutputStream();
      expected.writeBytes("<?xml version='1.0' standalone='yes'?>\n".getBytes(StandardCharsets.UTF_8));
      expected.writeBytes(first);
      expected.writeBytes("<?xml version='1.0' standalone='yes'?>\n".getBytes(StandardCharsets.UTF_8));
      expected.writeBytes(second);
      assertArrayEquals(expected.toByteArray(), wire.toByteArray());
      assertEquals(1, completions.get());
    }
  }

  @Test
  void filtered_event_completes_once_without_a_wire_write() throws Exception {
    try (Fixture fixture = new Fixture()) {
      AtomicInteger completions = new AtomicInteger();
      SubscribedEventManager subscription = mock(SubscribedEventManager.class);
      MessageEvent delivery = new MessageEvent("/cot/out", subscription, null, completions::incrementAndGet);

      fixture.protocol.sendMessage(delivery);

      assertEquals(1, completions.get());
      verify(fixture.endpoint, never()).sendPacket(any());
    }
  }

  @Test
  void invalid_xml_completes_once_without_a_wire_write() throws Exception {
    try (Fixture fixture = new Fixture()) {
      AtomicInteger completions = new AtomicInteger();

      fixture.protocol.sendMessage(fixture.delivery("<invalid/>".getBytes(StandardCharsets.UTF_8), completions::incrementAndGet));

      assertEquals(1, completions.get());
      fixture.selected(SelectionKey.OP_WRITE);
      verify(fixture.endpoint, never()).sendPacket(any());
    }
  }

  @Test
  void write_failure_closes_the_protocol_without_reporting_successful_delivery() throws Exception {
    try (Fixture fixture = new Fixture()) {
      AtomicInteger completions = new AtomicInteger();
      when(fixture.endpoint.sendPacket(any())).thenThrow(new java.io.IOException("write failed"));
      fixture.protocol.sendMessage(fixture.delivery(event("failed", ""), completions::incrementAndGet));

      fixture.selected(SelectionKey.OP_WRITE);

      verify(fixture.endpoint).close();
      verify(fixture.manager).close(fixture.session, false);
      assertEquals(0, completions.get());
    }
  }

  private static byte[] event(String uid, String text) {
    return ("<event version='2.0' uid='" + uid + "' type='a-f-G-U-C' how='m-g' "
        + "time='2026-10-29T00:00:00Z' start='2026-10-29T00:00:00Z' stale='2026-10-29T00:01:00Z'>"
        + "<point lat='1' lon='2' hae='3' ce='4' le='5'/><detail><remarks>" + text
        + "</remarks></detail></event>").getBytes(StandardCharsets.UTF_8);
  }

  private static final class Fixture implements AutoCloseable {
    private final boolean originalJmx = JMXManager.isEnableJMX();
    private final EndPoint endpoint = mock(EndPoint.class);
    private final SessionManager manager = mock(SessionManager.class);
    private final Session session = mock(Session.class);
    private final MockedStatic<SessionManager> managerStatic;
    private final CotProtocol protocol;
    private final Selectable selector;

    private Fixture() throws Exception {
      JMXManager.setEnableJMX(false);
      EndPointServerConfigDTO config = mock(EndPointServerConfigDTO.class);
      EndPointConfigDTO buffers = new EndPointConfigDTO("tcp");
      buffers.setServerWriteBufferSize(128);
      when(endpoint.getConfig()).thenReturn(config);
      when(config.getEndPointConfig()).thenReturn(buffers);
      when(config.getUrl()).thenReturn("tcp://localhost:8087");
      when(endpoint.getJMXTypePath()).thenReturn(List.of());
      when(endpoint.getEndPointStatus()).thenReturn(mock(EndPointStatus.class));
      when(manager.create(any(), any())).thenReturn(session);
      managerStatic = mockStatic(SessionManager.class);
      managerStatic.when(SessionManager::getInstance).thenReturn(manager);
      protocol = new CotProtocol(endpoint, new Packet(0, false), new CotProtocolConfigDTO());
      ArgumentCaptor<Selectable> callback = ArgumentCaptor.forClass(Selectable.class);
      verify(endpoint).register(eq(SelectionKey.OP_READ), callback.capture());
      selector = callback.getValue();
    }

    private MessageEvent delivery(byte[] xml, Runnable completion) {
      SubscribedEventManager subscription = mock(SubscribedEventManager.class);
      when(subscription.getContext()).thenReturn(new SubscriptionContext("/cot/out"));
      return new MessageEvent("/cot/out", subscription, new MessageBuilder().setOpaqueData(xml).build(), completion);
    }

    private void selected(int selection) {
      selector.selected(selector, null, selection);
    }

    @Override
    public void close() throws Exception {
      try {
        protocol.close();
      } finally {
        managerStatic.close();
        JMXManager.setEnableJMX(originalJmx);
      }
    }
  }
}
