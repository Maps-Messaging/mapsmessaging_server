package io.mapsmessaging.state.adapter.cot;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.state.drone.core.TwinManager;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CotIngestAdapterTest {

  @Test
  void archiveMessageCarriesCotMetadataAndPayload() {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    byte[] xml = "<event uid=\"abc\"/>".getBytes(StandardCharsets.UTF_8);

    Message message = adapter.buildArchiveMessage(xml, "session-1");

    assertArrayEquals(xml, message.getOpaqueData());
    assertEquals("text/xml", message.getContentType());
    assertEquals("CoT", message.getMeta().get("protocol"));
    assertEquals("1.0", message.getMeta().get("version"));
    assertEquals("session-1", message.getMeta().get("sessionId"));
    assertFalse(message.isRetain());
  }

  @Test
  void emptyInboundMessageIsIgnoredAndCompletionStillRuns() {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    MessageEvent event = mock(MessageEvent.class);
    Message message = mock(Message.class);
    Runnable completion = mock(Runnable.class);
    when(event.getMessage()).thenReturn(message);
    when(event.getCompletionTask()).thenReturn(completion);
    when(message.getOpaqueData()).thenReturn(new byte[0]);

    adapter.sendMessage(event);

    assertEquals(0L, adapter.getRoutedCount());
    assertEquals(0L, adapter.getDroppedCount());
    assertEquals(-1L, adapter.getLastMessageAgeMillis());
    verify(completion).run();
  }

  @Test
  void clientConnectionIdentityIsStable() {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));

    assertEquals("cot-ingest", adapter.getName());
    assertEquals("1.0", adapter.getVersion());
    assertEquals("cot-ingest-adapter", adapter.getUniqueName());
    assertEquals("internal", adapter.getProtocolName());
    assertEquals("", adapter.getAuthenticationConfig());
    assertEquals("", adapter.getRemoteIp());
    assertEquals(0L, adapter.getTimeOut());
    assertNull(adapter.getPrincipal());
    assertDoesNotThrow(adapter::sendKeepAlive);
  }
}
