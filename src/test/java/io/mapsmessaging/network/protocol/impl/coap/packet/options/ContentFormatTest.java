package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentFormatTest {

  @Test
  void constructorSetsFormatAndEncodedIdentifier() {
    ContentFormat format = new ContentFormat(Format.JSON);

    assertEquals(Constants.CONTENT_FORMAT, format.getId());
    assertEquals(Format.JSON, format.getFormat());
    assertEquals(50L, format.getValue());
    assertArrayEquals(new byte[]{50}, format.pack());
  }

  @Test
  void updateDecodesKnownFormatIdentifiers() throws Exception {
    ContentFormat format = new ContentFormat();

    format.update(new byte[]{41});

    assertEquals(41L, format.getValue());
    assertEquals(Format.XML, format.getFormat());
  }

  @Test
  void unknownIdentifierFallsBackToOctetStreamWhileRetainingRawValue() throws Exception {
    ContentFormat format = new ContentFormat();

    format.update(new byte[]{99});

    assertEquals(99L, format.getValue());
    assertEquals(Format.OCTET_STREAM, format.getFormat());
  }

  @Test
  void formatLookupDefaultsUnknownNamesAndIdsToOctetStream() {
    assertEquals(Format.TEXT_PLAIN, Format.stringValueOf("text/plain"));
    assertEquals(Format.JSON, Format.valueOf(50));
    assertEquals(Format.OCTET_STREAM, Format.stringValueOf("something/custom"));
    assertEquals(Format.OCTET_STREAM, Format.valueOf(999));
  }
}
