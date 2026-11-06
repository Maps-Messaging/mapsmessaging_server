package io.mapsmessaging.tools.config.schema;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaGeneratorMainTest {

  @Test
  void mainReportsGeneratedSchemasOrExplicitlyReportsNoManagers() {
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    try {
      System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
      System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));

      assertDoesNotThrow(() -> JsonSchemaGeneratorMain.main(new String[0]));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }

    String output = out.toString(StandardCharsets.UTF_8) + err.toString(StandardCharsets.UTF_8);
    assertTrue(
        output.contains("Config: ") || output.contains("No ConfigManager implementations discovered."),
        output);
  }
}