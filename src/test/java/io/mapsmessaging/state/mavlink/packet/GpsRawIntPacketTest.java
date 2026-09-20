package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GpsRawIntPacketTest {

  @Test
  void mapsFixSatellitesAndDilutionValues() {
    GpsRawIntPacket packet = new GpsRawIntPacket(frame(Map.of(
        "fix_type", 3,
        "satellites_visible", 18,
        "eph", 67,
        "epv", 145
    ), true));

    assertEquals(MavlinkMessageIds.GPS_RAW_INT, packet.getMessageId());
    assertEquals(3, packet.getFixType());
    assertEquals(18, packet.getSatellitesVisible());
    assertEquals(0.67, packet.getHdop(), 0.000001);
    assertEquals(1.45, packet.getVdop(), 0.000001);
    assertTrue(packet.isValid());
    assertTrue(packet.hasValidFix());
    assertEquals("3D", packet.getFixTypeName());
  }

  @Test
  void allDefinedFixTypesHaveStableNames() {
    String[] names = {
        "NO_GPS", "NO_FIX", "2D", "3D", "DGPS",
        "RTK_FLOAT", "RTK_FIXED", "STATIC", "PPP"
    };

    for (int fix = 0; fix < names.length; fix++) {
      GpsRawIntPacket packet =
          new GpsRawIntPacket(frame(Map.of("fix_type", fix), true));
      assertEquals(names[fix], packet.getFixTypeName());
      assertEquals(fix >= 2, packet.hasValidFix());
    }

    GpsRawIntPacket unknown =
        new GpsRawIntPacket(frame(Map.of("fix_type", 99), true));
    assertEquals("UNKNOWN", unknown.getFixTypeName());
  }

  @Test
  void missingAndUnknownDilutionValuesExposeSentinels() {
    Map<String,Object> fields = new HashMap<>();
    fields.put("fix_type", 1);
    fields.put("eph", 65535);

    GpsRawIntPacket packet = new GpsRawIntPacket(frame(fields, false));

    assertTrue(Double.isNaN(packet.getHdop()));
    assertTrue(Double.isNaN(packet.getVdop()));
    assertEquals(-1, packet.getSatellitesVisible());
    assertFalse(packet.isValid());
    assertFalse(packet.hasValidFix());
  }

  private static ProcessedFrame frame(Map<String,Object> fields, boolean valid) {
    return new ProcessedFrame("GPS_RAW_INT", null, fields, valid, List.of(), null);
  }
}
