package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.drone.drone.DroneTwin;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CotToTwinMapperBranchCoverageTest {

  @Test
  void malformedNonEventAndMissingUidDocumentsAreRejected() {
    CotToTwinMapper mapper = new CotToTwinMapper();

    assertNull(mapper.map("<event".getBytes(StandardCharsets.UTF_8)));
    assertNull(mapper.map("<detail/>".getBytes(StandardCharsets.UTF_8)));
    assertNull(mapper.map("<event uid=\" \"/>".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void validEventMapsOriginalTypePointContactAndTrack() {
    String xml = """
        <event uid="uav-1" type="a-f-A">
          <point lat="38.4" lon="-9.1" hae="25.5"/>
          <detail>
            <contact callsign="UAV-001"/>
            <track speed="4.5" course="123.0"/>
          </detail>
        </event>
        """;

    DroneTwin twin = new CotToTwinMapper().map(xml.getBytes(StandardCharsets.UTF_8));

    assertNotNull(twin);
    assertEquals("uav-1", twin.getTwinId());
    assertEquals("a-f-A", twin.getAttributes().get(CotToTwinMapper.ORIGINAL_COT_TYPE_ATTRIBUTE));
    assertEquals(38.4, twin.getGeoPosition().getLatitude(), 0.0);
    assertEquals(-9.1, twin.getGeoPosition().getLongitude(), 0.0);
    assertEquals(25.5, twin.getGeoPosition().getAltitudeMslMeters(), 0.0);
    assertEquals("UAV-001", twin.getCallSign());
    assertEquals("UAV-001", twin.getDisplayName());
    assertEquals(4.5, twin.getGroundSpeedMetersPerSecond(), 0.0);
    assertEquals(123.0, twin.getCourseOverGroundDegrees(), 0.0);
  }

  @Test
  void invalidPointAndTrackNumbersAreIgnoredWithoutRejectingTwin() {
    String xml = """
        <event uid="uav-2">
          <point lat="north" lon="-9.1" hae="unknown"/>
          <detail><track speed="fast" course="west"/></detail>
        </event>
        """;

    DroneTwin twin = new CotToTwinMapper().map(xml.getBytes(StandardCharsets.UTF_8));

    assertNotNull(twin);
    assertNull(twin.getGeoPosition());
    assertNull(twin.getGroundSpeedMetersPerSecond());
    assertNull(twin.getCourseOverGroundDegrees());
  }
}