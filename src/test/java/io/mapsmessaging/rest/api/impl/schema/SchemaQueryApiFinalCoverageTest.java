package io.mapsmessaging.rest.api.impl.schema;

import io.mapsmessaging.schemas.config.SchemaConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaQueryApiFinalCoverageTest {

  @Test
  void explicitSchemaMimeTypeTakesPrecedenceOverFormat() throws Exception {
    SchemaConfig config = mock(SchemaConfig.class);
    when(config.getMimeType()).thenReturn("application/custom");
    when(config.getFormat()).thenReturn("json");

    assertEquals("application/custom", mime(config));
  }

  @Test
  void knownSchemaFormatsMapToExpectedMimeTypes() throws Exception {
    assertEquals("application/schema+json", mimeForFormat("json"));
    assertEquals("application/x-protobuf", mimeForFormat("protobuf"));
    assertEquals("application/avro+json", mimeForFormat("avro"));
    assertEquals("application/xml", mimeForFormat("xml"));
    assertEquals("application/cddl", mimeForFormat("cbor"));
  }

  @Test
  void rawUnknownAndNullFormatsFallBackToOctetStreamWhileMsgpackUsesJsonSchemaMime() throws Exception {
    assertEquals("application/octet-stream", mimeForFormat("native"));
    assertEquals("application/octet-stream", mimeForFormat("raw"));
    assertEquals("application/octet-stream", mimeForFormat("something-new"));
    assertEquals("application/octet-stream", mimeForFormat(null));
    assertEquals("application/schema+json", mimeForFormat("msgpack"));
  }

  @Test
  void sha256HelperProducesStableLowercaseHexDigest() throws Exception {
    Method method = SchemaQueryApi.class.getDeclaredMethod("sha256Hex", byte[].class);
    method.setAccessible(true);

    assertEquals(
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        method.invoke(null, "abc".getBytes(StandardCharsets.UTF_8)));
  }

  private static String mimeForFormat(String format) throws Exception {
    SchemaConfig config = mock(SchemaConfig.class);
    when(config.getMimeType()).thenReturn(null);
    when(config.getFormat()).thenReturn(format);
    return mime(config);
  }

  private static String mime(SchemaConfig config) throws Exception {
    Method method = SchemaQueryApi.class.getDeclaredMethod(
        "resolveSchemaMime", SchemaConfig.class);
    method.setAccessible(true);
    return (String) method.invoke(null, config);
  }
}