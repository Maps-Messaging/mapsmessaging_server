package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.CotProtocolConfigDTO;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;

import javax.security.auth.Subject;
import java.lang.reflect.Constructor;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CotProtocolTest {

  @Test
  void protocolIdentityInboundContractAndCloseUseInternalSession() throws Exception {
    boolean original = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    SessionManager manager = mock(SessionManager.class);
    Session session = mock(Session.class);
    SecurityContext security = mock(SecurityContext.class);
    Subject subject = new Subject();
    when(session.getSecurityContext()).thenReturn(security);
    when(security.getSubject()).thenReturn(subject);
    when(manager.create(any(), any())).thenReturn(session);

    try (MockedStatic<SessionManager> mocked = mockStatic(SessionManager.class)) {
      mocked.when(SessionManager::getInstance).thenReturn(manager);
      EndPoint endpoint = endpoint();
      CotProtocol protocol =
          new CotProtocol(endpoint, new Packet(0, false), new CotProtocolConfigDTO());

      assertEquals("CoT", protocol.getName());
      assertEquals("CoT 2.0 XML Streaming", protocol.getVersion());
      assertTrue(protocol.getSessionId().startsWith("cot-"));
      assertSame(subject, protocol.getSubject());
      assertFalse(protocol.processPacket(new Packet(0, false)));

      MessageEvent event = mock(MessageEvent.class);
      Runnable completion = mock(Runnable.class);
      when(event.getCompletionTask()).thenReturn(completion);
      protocol.sendMessage(event);
      verify(completion).run();

      MessageEvent eventWithoutCompletion = mock(MessageEvent.class);
      assertDoesNotThrow(() -> protocol.sendMessage(eventWithoutCompletion));

      protocol.close();
      protocol.close();
      verify(manager, times(1)).close(session, false);
    } finally {
      JMXManager.setEnableJMX(original);
    }
  }

  @Test
  @Timeout(10)
  void readTask_whenReadReportsBytesButDecodesNoPayload_parksInsteadOfSpinning() throws Exception {
    boolean original = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    SessionManager manager = mock(SessionManager.class);
    Session session = mock(Session.class);
    SecurityContext security = mock(SecurityContext.class);
    Subject subject = new Subject();
    when(session.getSecurityContext()).thenReturn(security);
    when(security.getSubject()).thenReturn(subject);
    when(manager.create(any(), any())).thenReturn(session);

    try (MockedStatic<SessionManager> mocked = mockStatic(SessionManager.class)) {
      mocked.when(SessionManager::getInstance).thenReturn(manager);
      EndPoint endpoint = endpoint();
      // Simulate the wedged SSL case: readPacket reports a positive (encrypted)
      // count but leaves the application packet empty - no decoded payload. With
      // the old while(read > 0) loop this spun the reader at 100% CPU forever.
      when(endpoint.readPacket(any())).thenReturn(16708);

      CotProtocol protocol =
          new CotProtocol(endpoint, new Packet(0, false), new CotProtocolConfigDTO());
      // The constructor registers OP_READ once; clear that interaction first.
      verify(endpoint).register(eq(SelectionKey.OP_READ), eq(protocol));
      clearInvocations(endpoint);

      protocol.selected(protocol, null, SelectionKey.OP_READ);

      // The reader must stop after a bounded number of reads and re-arm the
      // selector, rather than looping on the empty positive count.
      verify(endpoint, timeout(5000)).register(eq(SelectionKey.OP_READ), eq(protocol));
      verify(endpoint, atMost(5)).readPacket(any());
      verify(endpoint, never()).close();

      protocol.close();
    } finally {
      JMXManager.setEnableJMX(original);
    }
  }

  @Test
  @Timeout(10)
  void readTask_readsIntoBufferThatHoldsATlsRecord_notTheDetectionPacket() throws Exception {
    withSession((endpoint, session) -> {
      List<Packet> readTargets = new CopyOnWriteArrayList<>();
      when(endpoint.readPacket(any())).thenAnswer(invocation -> {
        readTargets.add(invocation.getArgument(0));
        return 16708;
      });
      // Protocol detection hands over its own 1 KB buffer - over ssl:// a TLS record decrypts
      // to up to 16 KB and can never be unwrapped into it.
      Packet detectionPacket = new Packet(1024, true);
      CotProtocol protocol = new CotProtocol(endpoint, detectionPacket, new CotProtocolConfigDTO());
      clearInvocations(endpoint);

      protocol.selected(protocol, null, SelectionKey.OP_READ);
      verify(endpoint, timeout(5000)).register(eq(SelectionKey.OP_READ), eq(protocol));

      assertFalse(readTargets.isEmpty());
      for (Packet target : readTargets) {
        assertNotSame(detectionPacket, target);
        assertTrue(target.getRawBuffer().capacity() >= 16704,
            "read buffer of " + target.getRawBuffer().capacity() + " bytes cannot hold a TLS record");
      }
      protocol.close();
    });
  }

  @Test
  void readBufferSize_usesConfiguredSize_withinTlsRecordFloorAndSafetyCap() {
    assertEquals(100_000, CotProtocol.readBufferSize(endpointWithReadBufferSize(100_000)));
    assertEquals(32 * 1024, CotProtocol.readBufferSize(endpointWithReadBufferSize(10_240)));
    assertEquals(32 * 1024, CotProtocol.readBufferSize(endpointWithReadBufferSize(1024)));
    assertEquals(1_048_576, CotProtocol.readBufferSize(endpointWithReadBufferSize(4_000_000)));
    assertEquals(32 * 1024, CotProtocol.readBufferSize(endpoint()));
  }

  @Test
  @Timeout(10)
  void readTask_doesNotGrowThePooledThreadNameAcrossRuns() throws Exception {
    withSession((endpoint, session) -> {
      List<String> readerNames = new CopyOnWriteArrayList<>();
      when(endpoint.readPacket(any())).thenAnswer(invocation -> {
        readerNames.add(Thread.currentThread().getName());
        return 16708;
      });
      CotProtocol protocol = new CotProtocol(endpoint, new Packet(0, false), new CotProtocolConfigDTO());

      for (int run = 0; run < 3; run++) {
        clearInvocations(endpoint);
        protocol.selected(protocol, null, SelectionKey.OP_READ);
        verify(endpoint, timeout(5000)).register(eq(SelectionKey.OP_READ), eq(protocol));
      }

      assertEquals(3, readerNames.size());
      for (String name : readerNames) {
        assertTrue(name.startsWith("CoT Protocol Reader::"), name);
        assertFalse(name.startsWith("CoT Protocol Reader::CoT Protocol Reader::"), name);
      }
      protocol.close();
    });
  }

  @Test
  @Timeout(10)
  void detection_prefix_and_large_following_read_publish_one_complete_event() throws Exception {
    withSession((endpoint, session) -> {
      Destination destination = mock(Destination.class);
      when(session.findDestination("/tak/cot/inbound", DestinationType.TOPIC))
          .thenReturn(CompletableFuture.completedFuture(destination));
      byte[] event = ("<event uid='handoff' type='a-f-G'><detail><remarks>" + "payload".repeat(800)
          + "</remarks></detail></event>").getBytes(StandardCharsets.UTF_8);
      int split = 128;
      Packet detection = new Packet(1024, false);
      detection.put(Arrays.copyOfRange(event, 0, split));
      detection.flip();
      AtomicBoolean delivered = new AtomicBoolean();
      when(endpoint.readPacket(any())).thenAnswer(invocation -> {
        if (delivered.getAndSet(true)) {
          return 0;
        }
        Packet target = invocation.getArgument(0);
        target.put(Arrays.copyOfRange(event, split, event.length));
        return event.length - split;
      });
      CotProtocol protocol = new CotProtocol(endpoint, detection, new CotProtocolConfigDTO());
      try (MockedStatic<MessageDaemon> daemon = mockStatic(MessageDaemon.class)) {
        daemon.when(MessageDaemon::getInstance).thenReturn(null);
        verify(destination, never()).storeMessage(any());
        clearInvocations(endpoint);
        // Run the actual reader on this thread so the singleton boundary remains isolated,
        // independent of whether another protocol test has started the daemon.
        Class<?> readerType = Class.forName(CotProtocol.class.getName() + "$ReadTask");
        Constructor<?> constructor = readerType.getDeclaredConstructor(CotProtocol.class);
        constructor.setAccessible(true);
        Runnable reader = (Runnable) constructor.newInstance(protocol);
        reader.run();
        verify(endpoint).register(eq(SelectionKey.OP_READ), eq(protocol));
        ArgumentCaptor<Message> stored = ArgumentCaptor.forClass(Message.class);
        verify(destination, times(1)).storeMessage(stored.capture());
        assertArrayEquals(event, stored.getValue().getOpaqueData());
        verify(endpoint, never()).close();
      } finally {
        protocol.close();
      }
    });
  }

  @Test
  void xml_codec_retains_an_incomplete_event_between_reads() throws Exception {
    CotStreamCodec codec = new CotXmlStreamCodec(1_048_576);
    assertTrue(codec.decode("noise<?xml?><event uid='split'>".getBytes(StandardCharsets.UTF_8)).isEmpty());

    List<byte[]> events = codec.decode("<detail>content</detail></event>tail".getBytes(StandardCharsets.UTF_8));

    assertEquals(1, events.size());
    assertArrayEquals("<event uid='split'><detail>content</detail></event>".getBytes(StandardCharsets.UTF_8), events.getFirst());
  }

  @FunctionalInterface
  private interface SessionBody {
    void run(EndPoint endpoint, Session session) throws Exception;
  }

  private static void withSession(SessionBody body) throws Exception {
    boolean original = JMXManager.isEnableJMX();
    JMXManager.setEnableJMX(false);
    SessionManager manager = mock(SessionManager.class);
    Session session = mock(Session.class);
    SecurityContext security = mock(SecurityContext.class);
    when(session.getSecurityContext()).thenReturn(security);
    when(security.getSubject()).thenReturn(new Subject());
    when(manager.create(any(), any())).thenReturn(session);
    try (MockedStatic<SessionManager> mocked = mockStatic(SessionManager.class)) {
      mocked.when(SessionManager::getInstance).thenReturn(manager);
      body.run(endpoint(), session);
    } finally {
      JMXManager.setEnableJMX(original);
    }
  }

  private static EndPoint endpointWithReadBufferSize(long size) {
    EndPoint endpoint = endpoint();
    EndPointConfigDTO endPointConfig = mock(EndPointConfigDTO.class);
    when(endPointConfig.getServerReadBufferSize()).thenReturn(size);
    when(endpoint.getConfig().getEndPointConfig()).thenReturn(endPointConfig);
    return endpoint;
  }

  private static EndPoint endpoint() {
    EndPoint endpoint = mock(EndPoint.class);
    EndPointServerConfigDTO config = mock(EndPointServerConfigDTO.class);
    when(endpoint.getConfig()).thenReturn(config);
    when(config.getUrl()).thenReturn("tcp://localhost:8087");
    when(endpoint.getJMXTypePath()).thenReturn(List.of());
    return endpoint;
  }
}
