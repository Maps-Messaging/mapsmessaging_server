/*
 *
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
package io.mapsmessaging.network.protocol.impl.cot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mapsmessaging.dto.rest.config.protocol.impl.CotPresenceConfigDTO;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class CotPresenceBuilderTest {

  @Test
  void builds_tak_presence_with_current_timestamps_and_configured_identity() throws Exception {
    CotPresenceConfigDTO config = new CotPresenceConfigDTO();
    config.setUid("maps-{interfaceName}");
    config.setCallsign("MAPS & {interfaceName}");
    config.setLatitude(38.44);
    config.setLongitude(-9.10);
    config.setHae(12.5);
    config.setSoftwareVersion("4.5.0-SNAPSHOT");
    Instant now = Instant.parse("2026-09-08T15:30:00Z");

    Document document = parse(CotPresenceBuilder.build(config, "remote-tak", now));
    Element event = document.getDocumentElement();
    Element point = (Element) event.getElementsByTagName("point").item(0);
    Element contact = (Element) event.getElementsByTagName("contact").item(0);
    Element group = (Element) event.getElementsByTagName("__group").item(0);
    Element takv = (Element) event.getElementsByTagName("takv").item(0);

    assertEquals("event", event.getTagName());
    assertEquals("maps-remote-tak", event.getAttribute("uid"));
    assertEquals("2026-09-08T15:30:00Z", event.getAttribute("time"));
    assertEquals("2026-09-08T15:32:00Z", event.getAttribute("stale"));
    assertEquals("38.44", point.getAttribute("lat"));
    assertEquals("-9.1", point.getAttribute("lon"));
    assertEquals("12.5", point.getAttribute("hae"));
    assertEquals("MAPS & remote-tak", contact.getAttribute("callsign"));
    assertEquals("Cyan", group.getAttribute("name"));
    assertEquals("Team Member", group.getAttribute("role"));
    assertEquals("MapsMessaging", takv.getAttribute("device"));
    assertEquals("4.5.0-SNAPSHOT", takv.getAttribute("version"));
  }

  @Test
  void omits_empty_optional_group_and_platform_data() throws Exception {
    CotPresenceConfigDTO config = new CotPresenceConfigDTO();
    config.setGroupName(null);
    config.setGroupRole("");
    config.setDevice(null);
    config.setPlatform("");
    config.setOperatingSystem(null);
    config.setSoftwareVersion("");

    Document document = parse(CotPresenceBuilder.build(config, "tak", Instant.EPOCH));
    assertEquals(0, document.getElementsByTagName("__group").getLength());
    assertEquals(0, document.getElementsByTagName("takv").getLength());
  }

  @Test
  void rejects_invalid_presence_configuration() {
    CotPresenceConfigDTO staleTooSoon = new CotPresenceConfigDTO();
    staleTooSoon.setStaleSeconds(staleTooSoon.getIntervalSeconds());
    assertThrows(IllegalArgumentException.class, () -> CotPresenceBuilder.validate(staleTooSoon));

    CotPresenceConfigDTO invalidLatitude = new CotPresenceConfigDTO();
    invalidLatitude.setLatitude(91);
    assertThrows(IllegalArgumentException.class, () -> CotPresenceBuilder.validate(invalidLatitude));

    CotPresenceConfigDTO nonFiniteAccuracy = new CotPresenceConfigDTO();
    nonFiniteAccuracy.setCe(Double.NaN);
    assertThrows(IllegalArgumentException.class, () -> CotPresenceBuilder.validate(nonFiniteAccuracy));

    CotPresenceConfigDTO missingIdentity = new CotPresenceConfigDTO();
    missingIdentity.setUid(" ");
    assertThrows(IllegalArgumentException.class, () -> CotPresenceBuilder.validate(missingIdentity));
  }

  private Document parse(byte[] xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
  }
}
