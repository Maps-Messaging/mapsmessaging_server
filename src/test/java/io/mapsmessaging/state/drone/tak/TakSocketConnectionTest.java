package io.mapsmessaging.state.drone.tak;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class TakSocketConnectionTest {

  @Test
  void writeNormalisesUnsupportedDashAndAppendsNewlineWhenConfigured() throws Exception {
    TakSocketConnection connection =
        new TakSocketConnection("unused", 1, 10, 10, true, 4);
    try {
      ByteArrayOutputStream output = installOutput(connection);
      invokeWrite(connection, "<event>—</event>");

      assertEquals("<event>-</event>\n", output.toString());
    } finally {
      connection.close();
    }
  }

  @Test
  void appendNewLineFalseMustNotAppendNewline() throws Exception {
    TakSocketConnection connection =
        new TakSocketConnection("unused", 1, 10, 10, false, 4);
    try {
      ByteArrayOutputStream output = installOutput(connection);
      invokeWrite(connection, "<event/>");

      assertEquals(
          "<event/>",
          output.toString(),
          "appendNewLine=false should suppress the framing newline");
    } finally {
      connection.close();
    }
  }

  @Test
  void blankAndPostCloseMessagesAreIgnored() {
    TakSocketConnection connection =
        new TakSocketConnection("unused", 1, 10, 10, true, 2);

    connection.accept(null);
    connection.accept("   ");
    assertTrue(connection.getQueue().isEmpty());

    connection.close();
    connection.accept("<event/>");

    assertTrue(connection.getQueue().isEmpty());
  }

  @Test
  void writes_areCountedForThisServer() throws Exception {
    TakSocketConnection connection =
        new TakSocketConnection("unused", 1, 10, 10, true, 4);
    try {
      installOutput(connection);
      assertEquals(-1L, connection.getServerStats().getLastWriteAgeMillis());

      invokeWrite(connection, "<event/>");
      invokeWrite(connection, "<event/>");

      assertEquals(2, connection.getServerStats().getWriteCount());
      assertTrue(connection.getServerStats().getLastWriteAgeMillis() >= 0);
    } finally {
      connection.close();
    }
  }

  @Test
  void failed_tls_handshake_closes_the_new_socket() throws Exception {
    SSLSocketFactory factory = mock(SSLSocketFactory.class);
    SSLSocket socket = mock(SSLSocket.class);
    when(factory.createSocket()).thenReturn(socket);
    doThrow(new IOException("handshake failed")).when(socket).startHandshake();
    TakSocketConnection connection = new TakSocketConnection("localhost", 1234, 10, 10, false, 4, factory);
    try {
      Method reconnect = TakSocketConnection.class.getDeclaredMethod("reconnect");
      reconnect.setAccessible(true);
      reconnect.invoke(connection);
      assertFalse(connection.isConnected());
      verify(socket).close();
      assertEquals(1, connection.getServerStats().getConnectFailureCount());
    } finally {
      connection.close();
    }
  }

  @Test
  void failedConnect_isCountedForThisServerOnly() throws Exception {
    int closedPort;
    try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
      closedPort = serverSocket.getLocalPort();
    }
    TakSocketConnection failing = new TakSocketConnection("127.0.0.1", closedPort, 500, 500, true, 4);
    TakSocketConnection other = new TakSocketConnection("unused", 1, 10, 10, true, 4);
    try {
      Method reconnect = TakSocketConnection.class.getDeclaredMethod("reconnect");
      reconnect.setAccessible(true);
      reconnect.invoke(failing);

      assertFalse(failing.isConnected());
      assertEquals(1, failing.getServerStats().getConnectFailureCount());
      assertEquals(0, failing.getServerStats().getConnectCount());
      assertEquals(0, other.getServerStats().getConnectFailureCount());
    } finally {
      failing.close();
      other.close();
    }
  }

  private static ByteArrayOutputStream installOutput(TakSocketConnection connection)
      throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Field field = TakSocketConnection.class.getDeclaredField("socketOutputStream");
    field.setAccessible(true);
    field.set(connection, output);
    return output;
  }

  private static void invokeWrite(TakSocketConnection connection, String xml)
      throws Exception {
    Method method = TakSocketConnection.class.getDeclaredMethod("write", String.class);
    method.setAccessible(true);
    method.invoke(connection, xml);
  }
}
