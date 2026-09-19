package io.mapsmessaging.network.protocol.impl.extension;

import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtensionTest {

  @Test
  void initialiseCanOnlyRunOnce() throws Exception {
    TestExtension extension = new TestExtension();

    extension.initializeExtension();

    assertTrue(extension.isInitialized());
    assertEquals(1, extension.initialiseCalls);
    assertThrows(IllegalStateException.class, extension::initializeExtension);
    assertEquals(1, extension.initialiseCalls);
  }

  @Test
  void protocolCanOnlyBeAttachedOnceAndProvidesSessionId() {
    TestExtension extension = new TestExtension();
    ExtensionProtocol protocol = mock(ExtensionProtocol.class);
    when(protocol.getSessionId()).thenReturn("session-1");

    assertThrows(IllegalStateException.class, extension::getSessionId);

    extension.attach(protocol);
    assertSame(protocol, extension.getExtensionProtocol());
    assertEquals("session-1", extension.getSessionId());

    assertThrows(
        IllegalStateException.class,
        () -> extension.attach(mock(ExtensionProtocol.class))
    );
  }

  @Test
  void closeDelegatesToAttachedProtocolAndIsSafeBeforeAttach() throws Exception {
    TestExtension unattached = new TestExtension();
    assertDoesNotThrow(unattached::close);

    TestExtension attached = new TestExtension();
    ExtensionProtocol protocol = mock(ExtensionProtocol.class);
    attached.attach(protocol);

    attached.close();

    verify(protocol).close();
  }

  @Test
  void inboundDelegatesAndWrapsAsyncFailuresAsIoException() throws Exception {
    TestExtension extension = new TestExtension();
    ExtensionProtocol protocol = mock(ExtensionProtocol.class);
    Message message = mock(Message.class);
    extension.attach(protocol);

    when(protocol.saveMessage("/input", message)).thenReturn(1);
    assertDoesNotThrow(() -> extension.receive("/input", message));
    verify(protocol).saveMessage("/input", message);

    when(protocol.saveMessage("/broken", message))
        .thenThrow(new ExecutionException(new IllegalStateException("failed")));

    IOException failure =
        assertThrows(IOException.class, () -> extension.receive("/broken", message));
    assertInstanceOf(ExecutionException.class, failure.getCause());
  }

  private static final class TestExtension extends Extension {
    int initialiseCalls;

    void attach(ExtensionProtocol protocol) {
      setExtensionProtocol(protocol);
    }

    void receive(String destination, Message message) throws IOException {
      inbound(destination, message);
    }

    @Override
    public void initialise() {
      initialiseCalls++;
    }

    @Override
    public String getName() {
      return "test-extension";
    }

    @Override
    public String getVersion() {
      return "1.0";
    }

    @Override
    public boolean supportsRemoteFiltering() {
      return false;
    }

    @Override
    public void outbound(String destinationName, Message message) {
    }

    @Override
    public void registerRemoteLink(
        String destination, String filter, Map<String, Object> linkProperties) {
    }

    @Override
    public void registerLocalLink(
        String destination, Map<String, Object> linkProperties) {
    }
  }
}
