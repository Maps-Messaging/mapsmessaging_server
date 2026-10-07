package io.mapsmessaging.network.protocol.impl.cot;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.cot.CotParser;
import io.mapsmessaging.cot.CotStreamDecoder;
import io.mapsmessaging.cot.CotStreamEncoder;
import io.mapsmessaging.cot.CotValidator;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("conformance")
@Tag("conformance-core")
@Tag("cot")
class CotXmlStreamingConformanceTest {

  private static final String COT_SPEC = "Cursor-on-Target 2.0";
  private static final String TAK_STREAM = "TAK traditional CoT XML stream";

  @Test
  @ProtocolRequirement(
      specification = COT_SPEC,
      value = "Section 2, Table 2-1: event requires version, type, uid, time, start, stale and how; point requires lat, lon, hae, ce and le",
      source = ProtocolRequirement.COT_20_SOURCE)
  void validCot20EventParsesWithRequiredEnvelopeAndPoint() throws Exception {
    byte[] xml = event("alpha", "34.1234", "-117.1234");

    var parsed = new CotParser().parse(xml);

    assertEquals("2.0", parsed.version());
    assertEquals("alpha", parsed.uid());
    assertEquals("a-f-G-U-C", parsed.type());
    assertEquals("m-g", parsed.how());
    assertEquals(0, parsed.point().lat().compareTo(new java.math.BigDecimal("34.1234")));
    assertEquals(0, parsed.point().lon().compareTo(new java.math.BigDecimal("-117.1234")));
  }

  @Test
  @ProtocolRequirement(
      specification = COT_SPEC,
      value = "Section 2, Table 2-1: uid is a required event attribute",
      source = ProtocolRequirement.COT_20_SOURCE)
  void missingUidIsRejectedByCot20Schema() {
    String xml = text(event("alpha", "1", "2")).replace(" uid='alpha'", "");
    assertThrows(IOException.class, () -> new CotValidator().validate(bytes(xml)));
  }

  @Test
  @ProtocolRequirement(
      specification = COT_SPEC,
      value = "Section 2, Table 2-1: latitude is constrained to -90..90 and longitude to -180..180",
      source = ProtocolRequirement.COT_20_SOURCE)
  void outOfRangePositionIsRejected() {
    assertThrows(IOException.class, () -> new CotValidator().validate(event("bad-lat", "91", "0")));
    assertThrows(IOException.class, () -> new CotValidator().validate(event("bad-lon", "0", "181")));
  }

  @Test
  @ProtocolRequirement(
      specification = COT_SPEC,
      value = "Section 2, Table 2-1: detail is optional and may carry CoT sub-schema content",
      source = ProtocolRequirement.COT_20_SOURCE)
  void optionalDetailContentDoesNotInvalidateBaseEvent() throws Exception {
    String xml = text(event("detail", "1", "2")).replace(
        "</event>",
        "<detail><remarks>unit test</remarks><contact callsign='ALPHA'/></detail></event>");
    assertDoesNotThrow(() -> new CotValidator().validate(bytes(xml)));
  }

  @Test
  @ProtocolRequirement(
      specification = TAK_STREAM,
      value = "Section traditional XML CoT streaming: a TCP stream may carry consecutive XML CoT events without TAK Protocol v1 protobuf framing",
      source = ProtocolRequirement.TAK_PROTOCOL_SOURCE)
  void adjacentTraditionalXmlEventsAreDecodedIndependently() throws Exception {
    byte[] first = event("one", "1", "2");
    byte[] second = event("two", "3", "4");
    byte[] stream = concat(first, second);

    List<byte[]> frames = new CotStreamDecoder(16_384).accept(stream);

    assertEquals(2, frames.size());
    assertArrayEquals(first, frames.get(0));
    assertArrayEquals(second, frames.get(1));
  }

  @Test
  @ProtocolRequirement(
      specification = TAK_STREAM,
      value = "Section traditional XML CoT streaming: XML event boundaries must survive arbitrary TCP fragmentation",
      source = ProtocolRequirement.TAK_PROTOCOL_SOURCE)
  void eventSurvivesEveryTwoReadFragmentationBoundary() throws Exception {
    byte[] event = event("split", "1", "2");

    for (int split = 0; split <= event.length; split++) {
      CotStreamDecoder decoder = new CotStreamDecoder(16_384);
      List<byte[]> frames = new ArrayList<>();
      frames.addAll(decoder.accept(java.util.Arrays.copyOfRange(event, 0, split)));
      frames.addAll(decoder.accept(java.util.Arrays.copyOfRange(event, split, event.length)));
      assertEquals(1, frames.size(), "split=" + split);
      assertArrayEquals(event, frames.getFirst(), "split=" + split);
    }
  }

  @Test
  @ProtocolRequirement(
      specification = TAK_STREAM,
      value = "Section traditional XML CoT streaming: framing is XML-aware and must not treat end-tag text inside comments or CDATA as an event boundary",
      source = ProtocolRequirement.TAK_PROTOCOL_SOURCE)
  void sentinelTextInsideCommentAndCdataDoesNotTerminateEvent() throws Exception {
    String xml = text(event("lexical", "1", "2")).replace(
        "</event>",
        "<detail><!-- </event> --><remarks><![CDATA[not really </event>]]></remarks></detail></event>");
    byte[] bytes = bytes(xml);

    List<byte[]> frames = new CotStreamDecoder(16_384).accept(bytes);

    assertEquals(1, frames.size());
    assertArrayEquals(bytes, frames.getFirst());
  }

  @Test
  @ProtocolRequirement(
      specification = TAK_STREAM,
      value = "Section traditional XML CoT streaming: XML representation may be emitted directly without TAK Protocol v1 protobuf framing",
      source = ProtocolRequirement.TAK_PROTOCOL_SOURCE)
  void encoderProducesXmlStreamDocumentRatherThanTakV1BinaryFrame() {
    byte[] event = event("encoded", "1", "2");
    byte[] encoded = new CotStreamEncoder().encode(event);
    String wire = text(encoded);

    assertTrue(wire.startsWith("<?xml"));
    assertTrue(wire.contains("<event"));
    assertFalse((encoded[0] & 0xff) == 0xbf);
  }

  private static byte[] event(String uid, String lat, String lon) {
    return bytes(
        "<event version='2.0' uid='" + uid + "' type='a-f-G-U-C' "
            + "time='2026-10-07T07:00:00Z' start='2026-10-07T07:00:00Z' "
            + "stale='2026-10-07T07:01:00Z' how='m-g'>"
            + "<point lat='" + lat + "' lon='" + lon + "' hae='10' ce='5' le='5'/>"
            + "</event>");
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static String text(byte[] value) {
    return new String(value, StandardCharsets.UTF_8);
  }

  private static byte[] concat(byte[] first, byte[] second) {
    byte[] result = new byte[first.length + second.length];
    System.arraycopy(first, 0, result, 0, first.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }
}
