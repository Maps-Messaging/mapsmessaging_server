/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mapsmessaging.state.drone.tak.model.TakEvent;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class CotXmlParserTest {

  private final CotXmlParser parser = new CotXmlParser();

  @Test
  void parsesSupportedCotFields() throws Exception {
    String xml = """
        <event version="2.0" uid="uav-1" type="a-f-A-M-F-U" how="m-g"
          time="2026-09-08T10:00:00Z" start="2026-09-08T10:00:00Z" stale="2026-09-08T10:01:00Z">
          <point lat="47.1" lon="8.2" hae="500" ce="4" le="6"/>
          <detail>
            <contact callsign="Falcon"/><track speed="12.5" course="91"/>
            <status lifecycle="ACTIVE" reason="updated"/>
            <remarks>ready &amp; tracking</remarks><precisionlocation altsrc="GPS"/>
            <takv device="DRONE" platform="UAV" os="MapsMessaging" version="1.0"/>
            <maps-link state="CONNECTED" connected="true" rssiDbm="-61" snrDb="18.5" latencyMs="42" rxErrorRate="0.01" txErrorRate="0"/>
            <vendor-extension value="preserved"><nested/></vendor-extension>
            <link uid="gcs-1" relation="controlled-by"/><link uid="team-1" relation="member-of"/>
          </detail>
        </event>
        """;
    TakEvent event = parser.parse(xml);
    assertEquals("uav-1", event.getUid());
    assertEquals(47.1, event.getPoint().getLat());
    assertEquals("Falcon", event.getDetail().getContact().getCallsign());
    assertEquals("ready & tracking", event.getDetail().getRemarks());
    assertEquals(-61, event.getDetail().getMapsLink().getRssiDbm());
    assertEquals(2, event.getDetail().getLinks().size());
    assertEquals(1, event.getDetail().getExtensions().size());
    assertTrue(new TakXmlSerialiser().toXml(event).contains(
        "<vendor-extension value=\"preserved\"><nested/></vendor-extension>"));
  }

  @Test
  void rejectsMalformedNumbersAndDoctypes() {
    assertThrows(IOException.class,
        () -> parser.parse("<event><point lat=\"NaN\"/></event>"));
    assertThrows(IOException.class,
        () -> parser.parse("<!DOCTYPE event [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><event><detail><remarks>&xxe;</remarks></detail></event>"));
  }

  @Test
  void rejectsSchemaInvalidEvents() {
    String xml = """
        <event version="2.0" uid="uav-1" type="a-f-A-M-F-U" how="m-g"
          time="2026-09-08T10:00:00Z" start="2026-09-08T10:00:00Z" stale="2026-09-08T10:01:00Z">
          <point lat="91" lon="8.2" hae="500" ce="4" le="6"/>
        </event>
        """;
    assertThrows(IOException.class, () -> parser.parse(xml));
  }
}
