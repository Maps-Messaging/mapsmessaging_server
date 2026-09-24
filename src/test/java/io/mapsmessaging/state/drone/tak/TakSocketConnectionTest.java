package io.mapsmessaging.state.drone.tak;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
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
